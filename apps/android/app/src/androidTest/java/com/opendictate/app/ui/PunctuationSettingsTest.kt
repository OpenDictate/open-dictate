package com.opendictate.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.opendictate.app.MainActivity
import com.opendictate.app.R
import com.opendictate.app.data.SettingsStore
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PunctuationSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun optionalAccuratePunctuationTogglePersistsAndIsVisible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = SettingsStore(context)
        assertFalse(settings.accuratePunctuationEnabled)
        val title = context.getString(R.string.accurate_punctuation_title)
        compose.onNodeWithText(title).performScrollTo().assertIsDisplayed().performClick()
        compose.waitForIdle()
        assertTrue(settings.accuratePunctuationEnabled)
        assertEquals("true", settings.syncDocument().entries["accuratePunctuationEnabled"]?.value)
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "punctuation-phone.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText(title).performClick()
        compose.waitForIdle()
        assertFalse(settings.accuratePunctuationEnabled)
    }
}
