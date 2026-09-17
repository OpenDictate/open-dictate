package com.openwispr.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryTermsTest {
    @Test
    fun `keeps one term per line`() {
        assertEquals(
            "Kotlin\nOpenWispr\nJetpack Compose",
            normalizeDictionaryTerms("Kotlin\nOpenWispr\nJetpack Compose"),
        )
    }

    @Test
    fun `converts legacy comma-separated terms to lines`() {
        assertEquals(
            "Kotlin\nOpenWispr\nJetpack Compose",
            normalizeDictionaryTerms("Kotlin, OpenWispr ,\tJetpack Compose"),
        )
    }

    @Test
    fun `normalizes carriage-return line endings`() {
        assertEquals(
            "Kotlin\nOpenWispr\nCompose",
            normalizeDictionaryTerms("Kotlin\r\nOpenWispr\rCompose"),
        )
    }
}
