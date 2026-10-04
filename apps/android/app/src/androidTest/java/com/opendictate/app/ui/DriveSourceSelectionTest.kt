package com.opendictate.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.opendictate.app.R
import com.opendictate.app.data.SettingsSyncDocument
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class DriveSourceSelectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cloudIsDefaultAndLocalChoiceAndCancelHaveExplicitActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var choice: SettingsSyncDocument.ConnectionSource? = null
        var cancelled = false
        compose.setContent { OpenDictateTheme { DriveSourceSelectionDialog({ choice = it }, { cancelled = true }) } }
        compose.onNodeWithText(context.getString(R.string.drive_sync_source_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.drive_sync_source_cloud)).assertIsSelected()
        compose.onNodeWithText(context.getString(R.string.drive_sync_use_cloud)).assertIsDisplayed()
        compose.waitForIdle()
        // Native dialog-window transitions continue outside Compose's test clock.
        android.os.SystemClock.sleep(500)
        val capture = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "drive-source.png").outputStream().use {
            capture.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText(context.getString(R.string.drive_sync_use_cloud)).performClick()
        assertEquals(SettingsSyncDocument.ConnectionSource.CLOUD, choice)
        compose.onNodeWithText(context.getString(R.string.drive_sync_source_local)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.drive_sync_use_local)).performClick()
        assertEquals(SettingsSyncDocument.ConnectionSource.LOCAL, choice)
        compose.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
        assertTrue(cancelled)
    }
}
