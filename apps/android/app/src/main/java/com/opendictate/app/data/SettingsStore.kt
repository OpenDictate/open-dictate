package com.opendictate.app.data

import android.content.Context
import android.content.res.Resources
import androidx.core.content.edit
import com.opendictate.app.model.DictationLanguage
import com.opendictate.app.model.ModelCatalog
import com.opendictate.app.model.TextTransformationModel
import com.opendictate.app.model.TranscriptionModel
import com.opendictate.app.model.TranscriptionResponseTimeout
import com.opendictate.app.model.preferredModelId

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
        get() = preferredModelId(
            prefs.getString(KEY_LIVE_MODEL_ID, null).orEmpty(),
            modelCatalog.live,
            ModelCatalog.DEFAULT.live,
        )
        set(value) = prefs.edit { putString(KEY_LIVE_MODEL_ID, value) }

    var accurateModelId: String
        get() = preferredModelId(
            prefs.getString(KEY_ACCURATE_MODEL_ID, null).orEmpty(),
            modelCatalog.accurate,
            ModelCatalog.DEFAULT.accurate,
        )
        set(value) = prefs.edit { putString(KEY_ACCURATE_MODEL_ID, value) }

    var transformationModelId: String
        get() = preferredModelId(
            prefs.getString(KEY_TRANSFORMATION_MODEL_ID, null).orEmpty()
                .ifBlank { transformationModel.apiName },
            modelCatalog.text,
            ModelCatalog.DEFAULT.text,
        )
        set(value) = prefs.edit { putString(KEY_TRANSFORMATION_MODEL_ID, value) }

    var modelCatalog: ModelCatalog
        get() = ModelCatalog.fromCachedJson(prefs.getString(KEY_MODEL_CATALOG_V2, null))
        set(value) = prefs.edit {
            putString(KEY_MODEL_CATALOG_V2, value.toCachedJson())
            remove(KEY_MODEL_CATALOG)
            putLong(KEY_MODEL_CATALOG_UPDATED, System.currentTimeMillis())
        }

    val modelCatalogUpdatedAt: Long
        get() = if (prefs.contains(KEY_MODEL_CATALOG_V2)) {
            prefs.getLong(KEY_MODEL_CATALOG_UPDATED, 0L)
        } else {
            0L
        }

    fun clearModelCatalog() = prefs.edit {
        remove(KEY_MODEL_CATALOG)
        remove(KEY_MODEL_CATALOG_V2)
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

    var excludedPackages: Set<String>
        get() = prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit { putStringSet(KEY_EXCLUDED_PACKAGES, value.toSet()) }

    fun isPackageExcluded(packageName: CharSequence?): Boolean =
        packageName != null && prefs.getStringSet(KEY_EXCLUDED_PACKAGES, null)
            ?.contains(packageName.toString()) == true

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
        private const val KEY_MODEL_CATALOG_V2 = "model_catalog_v2"
        private const val KEY_MODEL_CATALOG_UPDATED = "model_catalog_updated"
        private const val KEY_LANGUAGES = "languages"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_PROMPT = "prompt"
        private const val KEY_KEEP_TRAILING_PERIOD = "keep_trailing_period"
        private const val KEY_TRANSFORMATION_BUTTON_ENABLED = "transformation_button_enabled"
        private const val KEY_EXCLUDED_PACKAGES = "excluded_packages"
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
