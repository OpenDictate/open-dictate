package com.openwispr.app.service

object TranscriptFormatter {
    fun format(text: String, keepTrailingPeriod: Boolean): String {
        if (keepTrailingPeriod) return text
        val periodIndex = text.indexOfLast { !it.isWhitespace() }
        if (periodIndex < 0 || text[periodIndex] != '.') return text
        if (periodIndex > 0 && text[periodIndex - 1] == '.') return text
        return text.removeRange(periodIndex, periodIndex + 1)
    }

    fun formatFinal(text: String, keepTrailingPeriod: Boolean): String {
        if (!keepTrailingPeriod) return format(text, keepTrailingPeriod = false)
        val lastTextIndex = text.indexOfLast { !it.isWhitespace() }
        if (lastTextIndex < 0 || text[lastTextIndex] in TERMINAL_PUNCTUATION) return text
        return text.substring(0, lastTextIndex + 1) + "." + text.substring(lastTextIndex + 1)
    }

    private val TERMINAL_PUNCTUATION = setOf('.', '?', '!', '…')
}
