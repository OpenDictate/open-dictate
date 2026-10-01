package com.opendictate.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReplacementState(
    val document: ReplacementDocument = ReplacementDocument(),
    val enabled: Boolean = true,
    val storageError: Boolean = false,
)

class ReplacementStore(context: Context, preferencesName: String = "word_replacements") {
    private val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(load())
    val state = mutableState.asStateFlow()

    fun engine(): WordReplacementEngine = state.value.let {
        WordReplacementEngine(if (it.enabled) it.document.rules else emptyList())
    }
    fun save(id: String?, source: String, replacement: String): Boolean {
        val source = java.text.Normalizer.normalize(source.trim(), java.text.Normalizer.Form.NFC)
        val current = state.value
        if (current.storageError || current.document.rules.any { it.id != id && it.source.equals(source, ignoreCase = true) }) return false
        val previous = current.document.rules.firstOrNull { it.id == id }
        val rule = WordReplacement(id ?: java.util.UUID.randomUUID().toString(), source, replacement.trim(), previous?.enabled ?: true)
        val next = ReplacementDocument(current.document.rules.filterNot { it.id == rule.id } + rule)
        return next.isValid && persist(next)
    }
    fun setEnabled(enabled: Boolean) {
        if (prefs.edit().putBoolean("enabled", enabled).commit()) mutableState.value = state.value.copy(enabled = enabled)
        else mutableState.value = state.value.copy(storageError = true)
    }
    fun toggle(rule: WordReplacement, enabled: Boolean) {
        if (!state.value.storageError) persist(ReplacementDocument(state.value.document.rules.map { if (it.id == rule.id) it.copy(enabled = enabled) else it }))
    }
    fun delete(rule: WordReplacement) {
        if (!state.value.storageError) persist(ReplacementDocument(state.value.document.rules.filterNot { it.id == rule.id }))
    }
    private fun persist(document: ReplacementDocument): Boolean {
        val saved = prefs.edit().putString("document", document.toJson()).commit()
        mutableState.value = if (saved) state.value.copy(document = document) else state.value.copy(storageError = true)
        return saved
    }
    private fun load(): ReplacementState {
        val enabled = prefs.getBoolean("enabled", true)
        val saved = prefs.getString("document", null) ?: return ReplacementState(enabled = enabled)
        return runCatching { ReplacementState(ReplacementDocument.fromJson(saved), enabled) }
            .getOrElse { ReplacementState(enabled = enabled, storageError = true) }
    }
}
