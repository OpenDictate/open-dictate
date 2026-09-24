package com.opendictate.app.data

import android.content.Context
import android.content.res.Resources
import androidx.core.content.edit
import com.opendictate.app.model.DictationLanguage
import com.opendictate.app.model.ModelCatalog
import com.opendictate.app.model.TextTransformationModel
import com.opendictate.app.model.TranscriptionModel
import com.opendictate.app.model.TranscriptionResponseTimeout

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var model: TranscriptionModel
        get() = TranscriptionModel.fromStored(prefs.getString(KEY_MODEL, null))
        set(value) = prefs.edit { putString(KEY_MODEL, value.name) }

    var transformationModel: TextTransformationModel
        get() = TextTransformationModel.fromStored(
            prefs.getString(KEY_TRANSFORMATION_MODEL, null),
        )
        set(value) = prefs.edit { putString(KEY_TRANSFORMATION_MODEL, value.name) }

    var liveModelId: String
        get() = prefs.getString(KEY_LIVE_MODEL_ID, null).orEmpty().ifBlank { ModelCatalog.DEFAULT.live.first() }
        set(value) = prefs.edit { putString(KEY_LIVE_MODEL_ID, value) }

    var accurateModelId: String
        get() = prefs.getString(KEY_ACCURATE_MODEL_ID, null).orEmpty().ifBlank { ModelCatalog.DEFAULT.accurate.first() }
        set(value) = prefs.edit { putString(KEY_ACCURATE_MODEL_ID, value) }

    var transformationModelId: String
        get() = prefs.getString(KEY_TRANSFORMATION_MODEL_ID, null).orEmpty()
            .ifBlank { transformationModel.apiName }
        set(value) = prefs.edit { putString(KEY_TRANSFORMATION_MODEL_ID, value) }

    var modelCatalog: ModelCatalog
        get() = ModelCatalog.fromIds(prefs.getStringSet(KEY_MODEL_CATALOG, emptySet()).orEmpty())
        set(value) = prefs.edit {
            putStringSet(KEY_MODEL_CATALOG, (value.live + value.accurate + value.text).toSet())
            putLong(KEY_MODEL_CATALOG_UPDATED, System.currentTimeMillis())
        }

    val modelCatalogUpdatedAt: Long
        get() = prefs.getLong(KEY_MODEL_CATALOG_UPDATED, 0L)

    fun clearModelCatalog() = prefs.edit {
        remove(KEY_MODEL_CATALOG)
        remove(KEY_MODEL_CATALOG_UPDATED)
    }

    var languages: Set<DictationLanguage>
        get() = DictationLanguage.fromStored(
            values = prefs.getStringSet(KEY_LANGUAGES, null),
            legacyValue = prefs.getString(KEY_LANGUAGE, null),
            defaultValues = systemDictationLanguages(),
        )
        set(value) = prefs.edit {
            putStringSet(KEY_LANGUAGES, value.mapTo(mutableSetOf()) { it.name })
            remove(KEY_LANGUAGE)
        }

    var prompt: String
        get() = normalizeDictionaryTerms(prefs.getString(KEY_PROMPT, DEFAULT_PROMPT).orEmpty())
        set(value) = prefs.edit { putString(KEY_PROMPT, normalizeDictionaryTerms(value).trim()) }

    var keepTrailingPeriod: Boolean
        get() = prefs.getBoolean(KEY_KEEP_TRAILING_PERIOD, true)
        set(value) = prefs.edit { putBoolean(KEY_KEEP_TRAILING_PERIOD, value) }

    var transformationButtonEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRANSFORMATION_BUTTON_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_TRANSFORMATION_BUTTON_ENABLED, value) }

    var transcriptionResponseTimeoutSeconds: Int?
        get() = TranscriptionResponseTimeout.fromStored(
            prefs.getInt(KEY_TRANSCRIPTION_RESPONSE_TIMEOUT, TranscriptionResponseTimeout.DEFAULT_SECONDS),
        )
        set(value) {
            require(value == null || TranscriptionResponseTimeout.isValid(value))
            prefs.edit { putInt(KEY_TRANSCRIPTION_RESPONSE_TIMEOUT, value ?: 0) }
        }

    companion object {
        private const val FILE_NAME = "opendictate_settings"
        private const val KEY_MODEL = "model"
        private const val KEY_TRANSFORMATION_MODEL = "transformation_model"
        private const val KEY_LIVE_MODEL_ID = "live_model_id"
        private const val KEY_ACCURATE_MODEL_ID = "accurate_model_id"
        private const val KEY_TRANSFORMATION_MODEL_ID = "transformation_model_id"
        private const val KEY_MODEL_CATALOG = "model_catalog"
        private const val KEY_MODEL_CATALOG_UPDATED = "model_catalog_updated"
        private const val KEY_LANGUAGES = "languages"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_PROMPT = "prompt"
        private const val KEY_KEEP_TRAILING_PERIOD = "keep_trailing_period"
        private const val KEY_TRANSFORMATION_BUTTON_ENABLED = "transformation_button_enabled"
        private const val KEY_TRANSCRIPTION_RESPONSE_TIMEOUT = "transcription_response_timeout_seconds"
        private const val DEFAULT_PROMPT = "OpenDictate\nDictate"
    }
}

private fun systemDictationLanguages(): Set<DictationLanguage> {
    val locales = Resources.getSystem().configuration.locales
    return DictationLanguage.fromLanguageTags(
        List(locales.size()) { index -> locales[index].toLanguageTag() },
    )
}
