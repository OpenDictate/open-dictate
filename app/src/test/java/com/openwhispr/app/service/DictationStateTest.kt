package com.openwhispr.app.service

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
}
