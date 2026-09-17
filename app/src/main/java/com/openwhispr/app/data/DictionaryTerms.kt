package com.openwhispr.app.data

private val LEGACY_COMMA_SEPARATOR = Regex("[ \\t]*,[ \\t]*")

internal fun normalizeDictionaryTerms(value: String): String =
    value
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace(LEGACY_COMMA_SEPARATOR, "\n")
