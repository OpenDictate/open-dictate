package com.openwhispr.app.data

import android.content.Context
import androidx.core.content.edit
import com.openwhispr.app.model.DictationLanguage
import com.openwhispr.app.model.TranscriptionModel

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var model: TranscriptionModel
        get() = TranscriptionModel.fromStored(prefs.getString(KEY_MODEL, null))
        set(value) = prefs.edit { putString(KEY_MODEL, value.name) }

    var languages: Set<DictationLanguage>
        get() = DictationLanguage.fromStored(
            values = prefs.getStringSet(KEY_LANGUAGES, null),
            legacyValue = prefs.getString(KEY_LANGUAGE, null),
        )
        set(value) = prefs.edit {
            putStringSet(KEY_LANGUAGES, value.mapTo(mutableSetOf()) { it.name })
            remove(KEY_LANGUAGE)
        }

    var prompt: String
        get() = prefs.getString(KEY_PROMPT, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_PROMPT, value.trim()) }

    var keepTrailingPeriod: Boolean
        get() = prefs.getBoolean(KEY_KEEP_TRAILING_PERIOD, true)
        set(value) = prefs.edit { putBoolean(KEY_KEEP_TRAILING_PERIOD, value) }

    companion object {
        private const val FILE_NAME = "openwhispr_settings"
        private const val KEY_MODEL = "model"
        private const val KEY_LANGUAGES = "languages"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_PROMPT = "prompt"
        private const val KEY_KEEP_TRAILING_PERIOD = "keep_trailing_period"
    }
}
