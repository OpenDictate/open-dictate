package com.openwhispr.app.model

enum class TranscriptionModel(val apiName: String) {
    LIVE("gpt-live-transcribe"),
    ACCURATE("gpt-transcribe");

    companion object {
        fun fromStored(value: String?): TranscriptionModel =
            entries.firstOrNull { it.name == value } ?: LIVE
    }
}

enum class LanguageHint(val code: String?) {
    AUTO(null),
    RUSSIAN("ru"),
    ENGLISH("en");

    companion object {
        fun fromStored(value: String?): LanguageHint =
            entries.firstOrNull { it.name == value } ?: AUTO
    }
}

