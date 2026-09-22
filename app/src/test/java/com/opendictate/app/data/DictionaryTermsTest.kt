package com.opendictate.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryTermsTest {
    @Test
    fun `keeps one term per line`() {
        assertEquals(
            "Kotlin\nOpenDictate\nJetpack Compose",
            normalizeDictionaryTerms("Kotlin\nOpenDictate\nJetpack Compose"),
        )
    }

    @Test
    fun `converts legacy comma-separated terms to lines`() {
        assertEquals(
            "Kotlin\nOpenDictate\nJetpack Compose",
            normalizeDictionaryTerms("Kotlin, OpenDictate ,\tJetpack Compose"),
        )
    }

    @Test
    fun `normalizes carriage-return line endings`() {
        assertEquals(
            "Kotlin\nOpenDictate\nCompose",
            normalizeDictionaryTerms("Kotlin\r\nOpenDictate\rCompose"),
        )
    }
}
