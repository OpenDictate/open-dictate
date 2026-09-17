package com.openwhispr.app.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayMotionTest {
    @Test
    fun `retarget keeps the overlay at the same screen position`() {
        val previousScreenTop = screenTop(
            displayHeight = 2_400,
            windowOffsetY = 800,
            overlayHeight = 156,
            translationY = -18f,
        )

        val compensatedTranslation = OverlayMotion.compensatedTranslationY(
            currentWindowOffsetY = 800,
            targetWindowOffsetY = 980,
            currentTranslationY = -18f,
        )

        assertEquals(
            previousScreenTop,
            screenTop(2_400, 980, 156, compensatedTranslation),
            0.001f,
        )
    }

    @Test
    fun `successive retargets remain continuous during an animation`() {
        val screenTopBeforeRetarget = screenTop(
            displayHeight = 2_400,
            windowOffsetY = 980,
            overlayHeight = 156,
            translationY = 72f,
        )

        val compensatedTranslation = OverlayMotion.compensatedTranslationY(
            currentWindowOffsetY = 980,
            targetWindowOffsetY = 1_070,
            currentTranslationY = 72f,
        )

        assertEquals(
            screenTopBeforeRetarget,
            screenTop(2_400, 1_070, 156, compensatedTranslation),
            0.001f,
        )
    }

    @Test
    fun `button sits just above the focused field`() {
        val density = 3f
        val displayHeight = 2_400
        val keyboardTop = 1_600
        val fieldTop = 1_200

        val offset = OverlayMotion.windowOffsetY(
            displayHeightPx = displayHeight,
            keyboardTopPx = keyboardTop,
            focusedFieldTopPx = fieldTop,
            density = density,
        )
        val buttonBottom = displayHeight - offset

        assertEquals(8f * density, (fieldTop - buttonBottom).toFloat(), 0.001f)
    }

    @Test
    fun `button falls back to clearing the keyboard when field bounds are unavailable`() {
        val density = 3f
        val displayHeight = 2_400
        val keyboardTop = 1_600

        val offset = OverlayMotion.windowOffsetY(
            displayHeightPx = displayHeight,
            keyboardTopPx = keyboardTop,
            focusedFieldTopPx = null,
            density = density,
        )
        val buttonBottom = displayHeight - offset

        assertEquals(72f * density, (keyboardTop - buttonBottom).toFloat(), 0.001f)
    }

    @Test
    fun `button ignores field bounds below the keyboard`() {
        val density = 3f
        val displayHeight = 2_400
        val keyboardTop = 1_600

        val offset = OverlayMotion.windowOffsetY(
            displayHeightPx = displayHeight,
            keyboardTopPx = keyboardTop,
            focusedFieldTopPx = 1_700,
            density = density,
        )
        val buttonBottom = displayHeight - offset

        assertEquals(72f * density, (keyboardTop - buttonBottom).toFloat(), 0.001f)
    }

    @Test
    fun `position keeps a top margin when vertical space is tight`() {
        val density = 2f
        val displayHeight = 500
        val keyboardTop = 260

        val offset = OverlayMotion.windowOffsetY(
            displayHeightPx = displayHeight,
            keyboardTopPx = keyboardTop,
            focusedFieldTopPx = 80,
            density = density,
        )
        val buttonTop = displayHeight - offset - OverlayMotion.heightPx(density)

        assertEquals(16f * density, buttonTop.toFloat(), 0.001f)
    }

    private fun screenTop(
        displayHeight: Int,
        windowOffsetY: Int,
        overlayHeight: Int,
        translationY: Float,
    ): Float = displayHeight - windowOffsetY - overlayHeight + translationY
}
