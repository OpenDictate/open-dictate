package com.openwhispr.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryTermsTest {
    @Test
    fun `keeps one term per line`() {
        assertEquals(
            "Kotlin\nOpenWhispr\nJetpack Compose",
            normalizeDictionaryTerms("Kotlin\nOpenWhispr\nJetpack Compose"),
        )
    }

    @Test
    fun `converts legacy comma-separated terms to lines`() {
        assertEquals(
            "Kotlin\nOpenWhispr\nJetpack Compose",
            normalizeDictionaryTerms("Kotlin, OpenWhispr ,\tJetpack Compose"),
        )
    }

    @Test
    fun `normalizes carriage-return line endings`() {
        assertEquals(
            "Kotlin\nOpenWhispr\nCompose",
            normalizeDictionaryTerms("Kotlin\r\nOpenWhispr\rCompose"),
        )
    }
}
