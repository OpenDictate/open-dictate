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

    @Test
    fun `adds trimmed selected phrase after existing terms`() {
        assertEquals(
            DictionaryAddition.Added("OpenDictate\nJetpack Compose"),
            addDictionaryTerm("OpenDictate", "  Jetpack\n Compose  "),
        )
    }

    @Test
    fun `does not add a case-insensitive duplicate`() {
        assertEquals(
            DictionaryAddition.Duplicate,
            addDictionaryTerm("OpenDictate\nJetpack Compose", " jetpack compose "),
        )
    }

    @Test
    fun `ignores empty selection`() {
        assertEquals(DictionaryAddition.Empty, addDictionaryTerm("OpenDictate", " \n "))
    }

    @Test
    fun `rejects additions that exceed the editor limit`() {
        assertEquals(
            DictionaryAddition.Full,
            addDictionaryTerm("a".repeat(MAX_DICTIONARY_LENGTH - 1), "word"),
        )
    }

    @Test
    fun `accepts a term that exactly fills the editor limit`() {
        assertEquals(
            DictionaryAddition.Added("a".repeat(MAX_DICTIONARY_LENGTH)),
            addDictionaryTerm("", "a".repeat(MAX_DICTIONARY_LENGTH)),
        )
    }
}
