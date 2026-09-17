package com.openwispr.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `empty locale override follows the system`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags(""))
    }

    @Test
    fun `recognizes supported language and regional variants`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTags("en-US"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromLanguageTags("ru"))
    }

    @Test
    fun `unknown override falls back to the system option`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTags("de"))
    }
}
