package com.opendictate.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionResponseTimeoutTest {
    @Test
    fun `defaults to 30 seconds for missing or invalid preference`() {
        assertEquals(30, TranscriptionResponseTimeout.DEFAULT_SECONDS)
        assertEquals(30, TranscriptionResponseTimeout.fromStored(-1))
        assertEquals(30, TranscriptionResponseTimeout.fromStored(601))
    }

    @Test
    fun `zero disables the limit and valid values are retained`() {
        assertNull(TranscriptionResponseTimeout.fromStored(0))
        assertEquals(1, TranscriptionResponseTimeout.fromStored(1))
        assertEquals(600, TranscriptionResponseTimeout.fromStored(600))
    }

    @Test
    fun `custom limit accepts only 1 to 600 seconds`() {
        assertFalse(TranscriptionResponseTimeout.isValid(0))
        assertTrue(TranscriptionResponseTimeout.isValid(30))
        assertFalse(TranscriptionResponseTimeout.isValid(601))
    }
}
