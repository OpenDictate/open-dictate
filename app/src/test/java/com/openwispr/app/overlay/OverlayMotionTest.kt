package com.openwispr.app.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayMotionTest {
    @Test
    fun `window height matches visible actions`() {
        val density = 2f

        assertEquals(224, OverlayMotion.heightPx(density, showTransformation = true))
        assertEquals(104, OverlayMotion.heightPx(density, showTransformation = false))
    }

    @Test
    fun `swipe to cancel follows leftward motion and clamps its travel`() {
        val density = 2f

        assertEquals(0f, OverlaySwipeToCancel.dragOffsetPx(500f, 540f, density), 0.001f)
        assertEquals(80f, OverlaySwipeToCancel.dragOffsetPx(500f, 420f, density), 0.001f)
        assertEquals(208f, OverlaySwipeToCancel.dragOffsetPx(500f, 100f, density), 0.001f)
    }

    @Test
    fun `swipe arms only after a deliberate mostly horizontal pull`() {
        val density = 2f

        assertFalse(OverlaySwipeToCancel.isArmed(500f, 300f, 357f, 300f, density))
        assertTrue(OverlaySwipeToCancel.isArmed(500f, 300f, 356f, 340f, density))
        assertFalse(OverlaySwipeToCancel.isArmed(500f, 300f, 356f, 460f, density))
    }

    @Test
    fun `retarget preserves position and velocity`() {
        val motion = OverlayPositionMotion(initialPositionPx = 800)
        motion.retarget(980)
        motion.advanceByMillis(32)

        val positionBeforeRetarget = motion.positionPx
        val velocityBeforeRetarget = motion.velocityPxPerSecond

        motion.retarget(1_070)

        assertEquals(positionBeforeRetarget, motion.positionPx, 0.001f)
        assertEquals(velocityBeforeRetarget, motion.velocityPxPerSecond, 0.001f)
    }

    @Test
    fun `successive keyboard positions produce continuous monotonic motion`() {
        val motion = OverlayPositionMotion(initialPositionPx = 800)
        val renderedPositions = mutableListOf(motion.positionPx)

        listOf(860, 940, 1_020, 1_070).forEach { keyboardPosition ->
            motion.retarget(keyboardPosition)
            repeat(3) {
                motion.advanceByMillis(16)
                renderedPositions += motion.positionPx
            }
        }
        repeat(120) {
            if (!motion.isAtRest) {
                motion.advanceByMillis(16)
                renderedPositions += motion.positionPx
            }
        }

        renderedPositions.zipWithNext().forEach { (previous, current) ->
            assertTrue("$current moved backwards from $previous", current >= previous)
        }
        assertTrue("motion did not settle", motion.isAtRest)
        assertEquals(1_070f, renderedPositions.last(), 0.001f)
    }

    @Test
    fun `closing keyboard produces continuous downward motion`() {
        val motion = OverlayPositionMotion(initialPositionPx = 1_070)
        val renderedPositions = mutableListOf(motion.positionPx)

        listOf(1_020, 940, 860, 800).forEach { keyboardPosition ->
            motion.retarget(keyboardPosition)
            repeat(3) {
                motion.advanceByMillis(16)
                renderedPositions += motion.positionPx
            }
        }
        repeat(120) {
            if (!motion.isAtRest) {
                motion.advanceByMillis(16)
                renderedPositions += motion.positionPx
            }
        }

        renderedPositions.zipWithNext().forEach { (previous, current) ->
            assertTrue("$current moved upwards from $previous", current <= previous)
        }
        assertTrue("motion did not settle", motion.isAtRest)
        assertEquals(800f, renderedPositions.last(), 0.001f)
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
}
