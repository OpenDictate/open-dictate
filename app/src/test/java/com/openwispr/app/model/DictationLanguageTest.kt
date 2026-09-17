package com.openwispr.app.model

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
}
