package com.openwhispr.app.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.openwhispr.app.data.SecureApiKeyStore
import com.openwhispr.app.data.SettingsStore
import com.openwhispr.app.model.DictationLanguage
import com.openwhispr.app.model.TranscriptionModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MainUiState(
    val hasApiKey: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val microphoneGranted: Boolean = false,
    val model: TranscriptionModel = TranscriptionModel.LIVE,
    val languages: Set<DictationLanguage> = emptySet(),
    val prompt: String = "",
    val keepTrailingPeriod: Boolean = true,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsStore(application)
    private val apiKeyStore = SecureApiKeyStore(application)
    private val mutableState = MutableStateFlow(loadState())
    val state = mutableState.asStateFlow()

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

    fun clearApiKey() {
        apiKeyStore.clear()
        refreshPermissions()
    }

    fun selectModel(model: TranscriptionModel) {
        settings.model = model
        mutableState.update { it.copy(model = model) }
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
        mutableState.update { it.copy(prompt = settings.prompt) }
    }

    fun setKeepTrailingPeriod(enabled: Boolean) {
        settings.keepTrailingPeriod = enabled
        mutableState.update { it.copy(keepTrailingPeriod = enabled) }
    }

    private fun loadState() = MainUiState(
        hasApiKey = apiKeyStore.hasKey(),
        accessibilityEnabled = isAccessibilityEnabled(getApplication()),
        microphoneGranted = hasMicrophonePermission(getApplication()),
        model = settings.model,
        languages = settings.languages,
        prompt = settings.prompt,
        keepTrailingPeriod = settings.keepTrailingPeriod,
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
