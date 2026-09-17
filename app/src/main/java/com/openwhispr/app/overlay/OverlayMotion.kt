package com.openwhispr.app.overlay

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt

internal object OverlayMotion {
    private const val HEIGHT_DP = 52f
    private const val FIELD_CLEARANCE_DP = 8f
    private const val KEYBOARD_CLEARANCE_DP = 72f
    private const val TOP_MARGIN_DP = 16f

    fun windowOffsetY(
        displayHeightPx: Int,
        keyboardTopPx: Int,
        focusedFieldTopPx: Int?,
        density: Float,
    ): Int {
        val fieldTop = focusedFieldTopPx?.takeIf { it in 0..keyboardTopPx }
        val anchorTop = fieldTop ?: keyboardTopPx
        val clearance = if (fieldTop != null) FIELD_CLEARANCE_DP else KEYBOARD_CLEARANCE_DP
        val preferredOffset = (displayHeightPx - anchorTop).coerceAtLeast(0) +
            dpToPx(clearance, density)
        val maximumOffset = (
            displayHeightPx - heightPx(density) - dpToPx(TOP_MARGIN_DP, density)
        ).coerceAtLeast(0)
        return min(preferredOffset, maximumOffset)
    }

    fun heightPx(density: Float): Int = dpToPx(HEIGHT_DP, density)

    private fun dpToPx(dp: Float, density: Float): Int = (dp * density).toInt()
}

/**
 * A critically damped position follower whose velocity survives target changes.
 *
 * Accessibility reports several intermediate keyboard bounds while the IME animates. Restarting
 * a conventional easing animation for every bound resets its velocity and makes the overlay look
 * like it is moving in a series of small starts and stops. This motion keeps both its rendered
 * position and velocity when a new bound arrives.
 */
internal class OverlayPositionMotion(initialPositionPx: Int) {
    var positionPx: Float = initialPositionPx.toFloat()
        private set

    var velocityPxPerSecond: Float = 0f
        private set

    var targetPositionPx: Int = initialPositionPx
        private set

    val isAtRest: Boolean
        get() = abs(positionPx - targetPositionPx) < REST_POSITION_EPSILON_PX &&
            abs(velocityPxPerSecond) < REST_VELOCITY_EPSILON_PX_PER_SECOND

    fun retarget(positionPx: Int) {
        targetPositionPx = positionPx
    }

    fun advanceByMillis(elapsedMillis: Long): Int {
        if (isAtRest) {
            positionPx = targetPositionPx.toFloat()
            velocityPxPerSecond = 0f
            return targetPositionPx
        }

        val elapsedSeconds = elapsedMillis
            .coerceIn(1L, MAX_FRAME_MILLIS)
            .toFloat() / 1_000f
        val target = targetPositionPx.toFloat()
        val displacement = positionPx - target
        val velocityTerm = velocityPxPerSecond + ANGULAR_FREQUENCY * displacement
        val decay = exp(-ANGULAR_FREQUENCY * elapsedSeconds)

        positionPx = target + (displacement + velocityTerm * elapsedSeconds) * decay
        velocityPxPerSecond = (
            velocityPxPerSecond - ANGULAR_FREQUENCY * velocityTerm * elapsedSeconds
        ) * decay

        if (isAtRest) {
            positionPx = target
            velocityPxPerSecond = 0f
        }
        return positionPx.roundToInt()
    }

    private companion object {
        const val ANGULAR_FREQUENCY = 30f
        const val MAX_FRAME_MILLIS = 64L
        const val REST_POSITION_EPSILON_PX = 0.5f
        const val REST_VELOCITY_EPSILON_PX_PER_SECOND = 5f
    }
}
