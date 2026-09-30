package com.opendictate.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {
    @Test
    fun `existing installations and unknown values follow the system`() {
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStored(null))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStored("unknown"))
    }

    @Test
    fun `stored choices round trip`() {
        AppTheme.entries.forEach { assertEquals(it, AppTheme.fromStored(it.name)) }
    }

    @Test
    fun `system choice follows changes to Android appearance`() {
        assertFalse(AppTheme.SYSTEM.isDark(false))
        assertTrue(AppTheme.SYSTEM.isDark(true))
    }

    @Test
    fun `manual choices override either system appearance`() {
        listOf(false, true).forEach { systemIsDark ->
            assertFalse(AppTheme.LIGHT.isDark(systemIsDark))
            assertTrue(AppTheme.DARK.isDark(systemIsDark))
        }
    }
}
