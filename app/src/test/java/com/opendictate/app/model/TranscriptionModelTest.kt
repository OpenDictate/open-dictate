package com.opendictate.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptionModelTest {
    @Test
    fun `defaults to accurate transcription when no model is stored`() {
        assertEquals(TranscriptionModel.ACCURATE, TranscriptionModel.fromStored(null))
    }

    @Test
    fun `defaults to accurate transcription for an unknown stored model`() {
        assertEquals(TranscriptionModel.ACCURATE, TranscriptionModel.fromStored("UNKNOWN"))
    }

    @Test
    fun `restores an explicitly selected model`() {
        assertEquals(TranscriptionModel.LIVE, TranscriptionModel.fromStored("LIVE"))
        assertEquals(TranscriptionModel.ACCURATE, TranscriptionModel.fromStored("ACCURATE"))
    }
}
