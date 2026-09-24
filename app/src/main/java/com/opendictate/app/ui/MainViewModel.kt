package com.opendictate.app.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opendictate.app.OpenDictateApplication
import com.opendictate.app.R
import com.opendictate.app.data.SecureApiKeyStore
import com.opendictate.app.data.SettingsStore
import com.opendictate.app.data.TranscriptHistoryItem
import com.opendictate.app.data.fuzzySearch
import com.opendictate.app.model.DictationLanguage
import com.opendictate.app.model.TextTransformationModel
import com.opendictate.app.model.TranscriptionModel
import com.opendictate.app.model.TranscriptionResponseTimeout
import com.opendictate.app.network.OpenAiTranscriptionClient
import com.opendictate.app.network.TranscriptSearchDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val hasApiKey: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val microphoneGranted: Boolean = false,
    val model: TranscriptionModel = TranscriptionModel.ACCURATE,
    val transformationModel: TextTransformationModel = TextTransformationModel.LUNA,
    val transformationButtonEnabled: Boolean = true,
    val languages: Set<DictationLanguage> = emptySet(),
    val keepTrailingPeriod: Boolean = true,
    val transcriptionResponseTimeoutSeconds: Int? = TranscriptionResponseTimeout.DEFAULT_SECONDS,
)

enum class HistorySearchMode {
    NONE,
    FUZZY,
    AI,
}

data class HistoryUiState(
    val entries: List<TranscriptHistoryItem> = emptyList(),
    val query: String = "",
    val searchMode: HistorySearchMode = HistorySearchMode.NONE,
    val aiMatchIds: List<Long> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val visibleEntries: List<TranscriptHistoryItem>
        get() = when (searchMode) {
            HistorySearchMode.NONE -> entries
            HistorySearchMode.FUZZY -> fuzzySearch(entries, query)
            HistorySearchMode.AI -> {
                val entriesById = entries.associateBy(TranscriptHistoryItem::id)
                aiMatchIds.mapNotNull(entriesById::get)
            }
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsStore(application)
    private val apiKeyStore = SecureApiKeyStore(application)
    private val historyStore = (application as OpenDictateApplication).transcriptHistoryStore
    private val apiClient = OpenAiTranscriptionClient(application.resources)
    private val mutableState = MutableStateFlow(loadState())
    val state = mutableState.asStateFlow()
    private val mutableHistoryState = MutableStateFlow(HistoryUiState())
    val historyState = mutableHistoryState.asStateFlow()
    private var historyLoadJob: Job? = null
    private var historySearchJob: Job? = null
    val prompt: String
        get() = settings.prompt

    fun refreshPermissions() {
        mutableState.update {
            it.copy(
                hasApiKey = apiKeyStore.hasKey(),
                accessibilityEnabled = isAccessibilityEnabled(getApplication()),
                microphoneGranted = hasMicrophonePermission(getApplication()),
            )
        }
    }

    fun saveApiKey(key: String) {
        apiKeyStore.save(key)
        refreshPermissions()
    }

    fun getApiKey(): String = apiKeyStore.get().orEmpty()

    fun clearApiKey() {
        apiKeyStore.clear()
        refreshPermissions()
    }

    fun selectModel(model: TranscriptionModel) {
        settings.model = model
        mutableState.update { it.copy(model = model) }
    }

    fun selectTransformationModel(model: TextTransformationModel) {
        settings.transformationModel = model
        mutableState.update { it.copy(transformationModel = model) }
    }

    fun setTransformationButtonEnabled(enabled: Boolean) {
        settings.transformationButtonEnabled = enabled
        mutableState.update { it.copy(transformationButtonEnabled = enabled) }
    }

    fun toggleLanguage(language: DictationLanguage) {
        val languages = mutableState.value.languages.toMutableSet().apply {
            if (!add(language)) remove(language)
        }
        settings.languages = languages
        mutableState.update { it.copy(languages = languages) }
    }

    fun useAutomaticLanguageDetection() {
        settings.languages = emptySet()
        mutableState.update { it.copy(languages = emptySet()) }
    }

    fun savePrompt(prompt: String) {
        settings.prompt = prompt
    }

    fun setKeepTrailingPeriod(enabled: Boolean) {
        settings.keepTrailingPeriod = enabled
        mutableState.update { it.copy(keepTrailingPeriod = enabled) }
    }

    fun setTranscriptionResponseTimeout(seconds: Int?) {
        settings.transcriptionResponseTimeoutSeconds = seconds
        mutableState.update { it.copy(transcriptionResponseTimeoutSeconds = seconds) }
    }

    fun refreshHistory() {
        historyLoadJob?.cancel()
        historyLoadJob = viewModelScope.launch {
            mutableHistoryState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val entries = withContext(Dispatchers.IO) { historyStore.getAll() }
                mutableHistoryState.update { it.copy(entries = entries, isLoading = false) }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                mutableHistoryState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = getApplication<Application>().getString(
                            R.string.history_load_error,
                        ),
                    )
                }
            }
        }
    }

    fun updateHistoryQuery(query: String) {
        historySearchJob?.cancel()
        mutableHistoryState.update { it.withHistoryQuery(query) }
    }

    fun runAiHistorySearch() {
        val state = mutableHistoryState.value
        val query = state.query.trim()
        if (query.isEmpty() || state.entries.isEmpty()) return
        val apiKey = apiKeyStore.get()
        if (apiKey.isNullOrBlank()) {
            mutableHistoryState.update {
                it.copy(errorMessage = getApplication<Application>().getString(R.string.error_add_api_key))
            }
            return
        }

        historySearchJob?.cancel()
        historySearchJob = viewModelScope.launch {
            mutableHistoryState.update {
                it.copy(
                    searchMode = HistorySearchMode.AI,
                    aiMatchIds = emptyList(),
                    isLoading = true,
                    errorMessage = null,
                )
            }
            try {
                val matches = apiClient.searchTranscriptHistory(
                    apiKey = apiKey,
                    model = settings.transformationModel,
                    query = query,
                    documents = state.entries.map {
                        TranscriptSearchDocument(id = it.id, text = it.text)
                    },
                )
                mutableHistoryState.update { current ->
                    if (current.query.trim() == query) {
                        current.copy(aiMatchIds = matches, isLoading = false)
                    } else {
                        current.copy(isLoading = false)
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                mutableHistoryState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: getApplication<Application>().getString(R.string.history_ai_search_error),
                    )
                }
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        val beforeDelete = mutableHistoryState.value
        mutableHistoryState.update { state ->
            state.copy(
                entries = state.entries.filterNot { it.id == id },
                aiMatchIds = state.aiMatchIds.filterNot { it == id },
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                val deleted = withContext(Dispatchers.IO) { historyStore.delete(id) }
                if (!deleted) throw IllegalStateException("History item was not found")
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                mutableHistoryState.value = beforeDelete.copy(
                    errorMessage = getApplication<Application>().getString(
                        R.string.history_delete_error,
                    ),
                )
            }
        }
    }

    private fun loadState() = MainUiState(
        hasApiKey = apiKeyStore.hasKey(),
        accessibilityEnabled = isAccessibilityEnabled(getApplication()),
        microphoneGranted = hasMicrophonePermission(getApplication()),
        model = settings.model,
        transformationModel = settings.transformationModel,
        transformationButtonEnabled = settings.transformationButtonEnabled,
        languages = settings.languages,
        keepTrailingPeriod = settings.keepTrailingPeriod,
        transcriptionResponseTimeoutSeconds = settings.transcriptionResponseTimeoutSeconds,
    )

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
    }

    private fun hasMicrophonePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

}

private const val MAX_HISTORY_QUERY_CHARS = 300

internal fun HistoryUiState.withHistoryQuery(query: String): HistoryUiState {
    val updatedQuery = query.take(MAX_HISTORY_QUERY_CHARS)
    return copy(
        query = updatedQuery,
        searchMode = if (updatedQuery.isBlank()) {
            HistorySearchMode.NONE
        } else {
            HistorySearchMode.FUZZY
        },
        aiMatchIds = emptyList(),
        isLoading = false,
        errorMessage = null,
    )
}
