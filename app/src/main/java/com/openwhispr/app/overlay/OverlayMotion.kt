package com.openwhispr.app.overlay

import kotlin.math.min

internal object OverlayMotion {
    private const val HEIGHT_DP = 52f
    private const val KEYBOARD_CLEARANCE_DP = 72f
    private const val TOP_MARGIN_DP = 16f

    fun compensatedTranslationY(
        currentWindowOffsetY: Int,
        targetWindowOffsetY: Int,
        currentTranslationY: Float,
    ): Float = currentTranslationY + targetWindowOffsetY - currentWindowOffsetY

    fun windowOffsetY(
        displayHeightPx: Int,
        keyboardTopPx: Int,
        density: Float,
    ): Int {
        val keyboardHeight = (displayHeightPx - keyboardTopPx).coerceAtLeast(0)
        val preferredOffset = keyboardHeight + dpToPx(KEYBOARD_CLEARANCE_DP, density)
        val maximumOffset = (
            displayHeightPx - heightPx(density) - dpToPx(TOP_MARGIN_DP, density)
        ).coerceAtLeast(0)
        return min(preferredOffset, maximumOffset)
    }

    fun heightPx(density: Float): Int = dpToPx(HEIGHT_DP, density)

    private fun dpToPx(dp: Float, density: Float): Int = (dp * density).toInt()
}
