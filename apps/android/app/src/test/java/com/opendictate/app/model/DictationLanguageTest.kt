package com.opendictate.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DictationLanguageTest {
    @Test
    fun `restores multiple stored languages in display order`() {
        val languages = DictationLanguage.fromStored(setOf("ENGLISH", "RUSSIAN"))

        assertEquals(
            linkedSetOf(DictationLanguage.RUSSIAN, DictationLanguage.ENGLISH),
            languages,
        )
    }

    @Test
    fun `migrates legacy single language`() {
        assertEquals(
            setOf(DictationLanguage.ENGLISH),
            DictationLanguage.fromStored(values = null, legacyValue = "ENGLISH"),
        )
    }

    @Test
    fun `legacy automatic selection stays automatic`() {
        assertEquals(
            emptySet<DictationLanguage>(),
            DictationLanguage.fromStored(values = null, legacyValue = "AUTO"),
        )
    }

    @Test
    fun `ignores unknown stored values`() {
        assertEquals(
            setOf(DictationLanguage.RUSSIAN),
            DictationLanguage.fromStored(setOf("RUSSIAN", "UNKNOWN")),
        )
    }

    @Test
    fun `uses supported device languages when no choice was stored`() {
        val deviceLanguages = DictationLanguage.fromLanguageTags(listOf("ru-RU", "en-US"))

        assertEquals(
            linkedSetOf(DictationLanguage.RUSSIAN, DictationLanguage.ENGLISH),
            DictationLanguage.fromStored(
                values = null,
                defaultValues = deviceLanguages,
            ),
        )
    }

    @Test
    fun `explicit automatic choice stays automatic instead of restoring device languages`() {
        assertEquals(
            emptySet<DictationLanguage>(),
            DictationLanguage.fromStored(
                values = emptySet(),
                defaultValues = setOf(DictationLanguage.RUSSIAN, DictationLanguage.ENGLISH),
            ),
        )
    }

    @Test
    fun `maps regional language tags and ignores unsupported languages`() {
        assertEquals(
            linkedSetOf(
                DictationLanguage.RUSSIAN,
                DictationLanguage.ENGLISH,
                DictationLanguage.UKRAINIAN,
            ),
            DictationLanguage.fromLanguageTags(listOf("en-US", "uk-Latn-UA", "ru-RU", "nl-NL")),
        )
    }
}
