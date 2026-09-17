package com.openwispr.app.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictationStateTest {
    @Test
    fun `active session accepts its own updates`() {
        val state = DictationState(sessionId = 42L, phase = DictationPhase.LISTENING)

        assertTrue(state.acceptsActiveUpdate(42L))
    }

    @Test
    fun `cancelled session rejects late partial updates`() {
        val state = DictationState(sessionId = 42L, phase = DictationPhase.IDLE)

        assertFalse(state.acceptsActiveUpdate(42L))
    }

    @Test
    fun `current session rejects updates from an older session`() {
        val state = DictationState(sessionId = 43L, phase = DictationPhase.LISTENING)

        assertFalse(state.acceptsActiveUpdate(42L))
    }

    @Test
    fun `processing dictation and text transformation accept cancellation`() {
        val dictation = DictationState(
            sessionId = 42L,
            phase = DictationPhase.PROCESSING,
            operation = DictationOperation.DICTATION,
        )
        val transformation = DictationState(
            sessionId = 42L,
            phase = DictationPhase.PROCESSING,
            operation = DictationOperation.TRANSFORMATION,
        )

        assertTrue(dictation.acceptsCancellation(42L))
        assertTrue(transformation.acceptsCancellation(42L))
    }

    @Test
    fun `cancellation rejects missing stale and completed sessions`() {
        val active = DictationState(sessionId = 42L, phase = DictationPhase.LISTENING)
        val completed = DictationState(sessionId = 42L, phase = DictationPhase.COMPLETED)

        assertFalse(active.acceptsCancellation(0L))
        assertFalse(active.acceptsCancellation(41L))
        assertFalse(completed.acceptsCancellation(42L))
    }
}
