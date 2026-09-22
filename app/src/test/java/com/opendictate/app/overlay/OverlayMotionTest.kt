package com.opendictate.app.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayMotionTest {
    @Test
    fun `window height matches visible actions`() {
        val density = 2f

        assertEquals(224, OverlayMotion.heightPx(density, showMenu = true))
        assertEquals(104, OverlayMotion.heightPx(density, showMenu = false))
    }

    @Test
    fun `overlay actions are square icon buttons`() {
        val density = 2f

        assertEquals(OverlayMotion.actionHeightPx(density), OverlayMotion.widthPx(density))
    }

    @Test
    fun `open menu widens the overlay without changing action height`() {
        val density = 2f

        assertEquals(408, OverlayMotion.widthPx(density, showMenu = true))
        assertEquals(224, OverlayMotion.heightPx(density, showMenu = true))
        assertEquals(104, OverlayMotion.actionHeightPx(density))
    }

    @Test
    fun `single action overlay can sit closer to the top edge`() {
        val offset = OverlayMotion.windowOffsetY(
            displayHeightPx = 600,
            keyboardTopPx = 80,
            focusedFieldTopPx = null,
            density = 2f,
            showMenu = false,
        )

        val buttonTop = 600 - offset - OverlayMotion.heightPx(
            density = 2f,
            showMenu = false,
        )
        assertEquals(32f, buttonTop.toFloat(), 0.001f)
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
    fun `swipe to dismiss follows rightward motion and clamps its travel`() {
        val density = 2f

        assertEquals(0f, OverlaySwipeToDismiss.dragOffsetPx(500f, 460f, density), 0.001f)
        assertEquals(80f, OverlaySwipeToDismiss.dragOffsetPx(500f, 580f, density), 0.001f)
        assertEquals(96f, OverlaySwipeToDismiss.dragOffsetPx(500f, 700f, density), 0.001f)
    }

    @Test
    fun `dismiss arms only after a deliberate mostly horizontal pull`() {
        val density = 2f

        assertFalse(OverlaySwipeToDismiss.isArmed(500f, 300f, 563f, 300f, density))
        assertTrue(OverlaySwipeToDismiss.isArmed(500f, 300f, 564f, 330f, density))
        assertFalse(OverlaySwipeToDismiss.isArmed(500f, 300f, 564f, 380f, density))
    }

    @Test
    fun `dismissal lasts only until the current keyboard target disappears`() {
        val session = OverlayVisibilitySession()

        assertTrue(session.shouldShow(hasEligibleTarget = true))
        session.dismiss()
        assertFalse(session.shouldShow(hasEligibleTarget = true))

        assertFalse(session.shouldShow(hasEligibleTarget = false))
        assertTrue(session.shouldShow(hasEligibleTarget = true))
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
