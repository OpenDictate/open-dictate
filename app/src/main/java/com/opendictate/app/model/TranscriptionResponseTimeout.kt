package com.opendictate.app.model

object TranscriptionResponseTimeout {
    const val DEFAULT_SECONDS = 30
    const val MIN_SECONDS = 1
    const val MAX_SECONDS = 600

    /** A stored zero disables the overall response limit. Invalid values use the default. */
    fun fromStored(seconds: Int): Int? = when {
        seconds == 0 -> null
        seconds in MIN_SECONDS..MAX_SECONDS -> seconds
        else -> DEFAULT_SECONDS
    }

    fun isValid(seconds: Int): Boolean = seconds in MIN_SECONDS..MAX_SECONDS
}
