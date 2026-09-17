package com.openwhispr.app.service

object TranscriptFormatter {
    fun format(text: String, keepTrailingPeriod: Boolean): String {
        if (keepTrailingPeriod) return text
        val periodIndex = text.indexOfLast { !it.isWhitespace() }
        if (periodIndex < 0 || text[periodIndex] != '.') return text
        if (periodIndex > 0 && text[periodIndex - 1] == '.') return text
        return text.removeRange(periodIndex, periodIndex + 1)
    }
}
