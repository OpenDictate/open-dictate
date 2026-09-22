package com.opendictate.app.model

enum class TranscriptionModel(val apiName: String) {
    LIVE("gpt-live-transcribe"),
    ACCURATE("gpt-transcribe");

    companion object {
        fun fromStored(value: String?): TranscriptionModel =
            entries.firstOrNull { it.name == value } ?: ACCURATE
    }
}

enum class DictationLanguage(val code: String) {
    RUSSIAN("ru"),
    ENGLISH("en"),
    UKRAINIAN("uk"),
    GERMAN("de"),
    FRENCH("fr"),
    SPANISH("es"),
    ITALIAN("it"),
    PORTUGUESE("pt"),
    POLISH("pl"),
    TURKISH("tr"),
    CHINESE("zh"),
    JAPANESE("ja"),
    KOREAN("ko"),
    ARABIC("ar"),
    HINDI("hi");

    companion object {
        fun fromStored(values: Set<String>?, legacyValue: String? = null): Set<DictationLanguage> {
            val storedNames = values ?: setOfNotNull(legacyValue).filterNot { it == "AUTO" }.toSet()
            return entries.filterTo(linkedSetOf()) { it.name in storedNames }
        }
    }
}
