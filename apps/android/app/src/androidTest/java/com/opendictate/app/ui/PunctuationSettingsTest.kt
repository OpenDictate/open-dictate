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

    @Test fun punctuationDefaultsOffAppearsOnlyInAccurateAndPersistsLocally() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = SettingsStore(context)
        assertFalse(settings.accuratePunctuationEnabled)
        val title = context.getString(R.string.accurate_punctuation_title)
        val live = context.getString(R.string.settings_mode_live)
        val accurate = context.getString(R.string.settings_mode_accurate)
        compose.onNodeWithText(live).performScrollTo().performClick()
        compose.onNodeWithText(title).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.accurate_punctuation_subtitle)).assertDoesNotExist()
        compose.onNodeWithText(accurate).performClick()
        compose.onNodeWithText(title).performScrollTo().assertIsDisplayed().assertIsOff()
        compose.onNodeWithText(context.getString(R.string.accurate_punctuation_subtitle)).assertIsDisplayed()
        compose.onNodeWithText("Final period").assertDoesNotExist()
        compose.onNodeWithText("Точка в конце").assertDoesNotExist()
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "punctuation-phone.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText(title).performClick()
        compose.waitForIdle()
        assertTrue(SettingsStore(context).accuratePunctuationEnabled)
        assertNull(settings.syncDocument().entries["accuratePunctuationEnabled"])
        compose.onNodeWithText(live).performScrollTo().performClick()
        compose.onNodeWithText(title).assertDoesNotExist()
        compose.onNodeWithText(accurate).performClick()
        compose.onNodeWithText(title).performScrollTo().assertIsOn().performClick()
        compose.waitForIdle()
        assertFalse(settings.accuratePunctuationEnabled)
    }
}
