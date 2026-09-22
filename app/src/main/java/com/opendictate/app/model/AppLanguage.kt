package com.opendictate.app.model

enum class AppLanguage(val languageTag: String) {
    SYSTEM(""),
    RUSSIAN("ru"),
    ENGLISH("en");

    companion object {
        fun fromLanguageTags(languageTags: String): AppLanguage {
            val primaryTag = languageTags.substringBefore(',').trim()
            return entries.firstOrNull { language ->
                language.languageTag.isNotEmpty() &&
                    (primaryTag == language.languageTag || primaryTag.startsWith("${language.languageTag}-"))
            } ?: SYSTEM
        }
    }
}
