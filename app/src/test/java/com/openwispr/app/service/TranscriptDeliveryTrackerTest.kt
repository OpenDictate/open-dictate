package com.openwispr.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptDeliveryTrackerTest {
    @Test
    fun `failed partial insertion waits for another update`() {
        val tracker = TranscriptDeliveryTracker()

        assertEquals(
            TranscriptDeliveryOutcome.PENDING,
            tracker.record("Partial", DictationPhase.LISTENING, inserted = false),
        )
        assertFalse(tracker.wasDelivered("Partial"))
    }

    @Test
    fun `failed final insertion requests clipboard fallback`() {
        val tracker = TranscriptDeliveryTracker()

        assertEquals(
            TranscriptDeliveryOutcome.COPY_TO_CLIPBOARD,
            tracker.record("Finished text", DictationPhase.COMPLETED, inserted = false),
        )
        assertTrue(tracker.wasDelivered("Finished text"))
    }

    @Test
    fun `successful insertion prevents duplicate delivery`() {
        val tracker = TranscriptDeliveryTracker()

        assertEquals(
            TranscriptDeliveryOutcome.INSERTED,
            tracker.record("Already inserted", DictationPhase.LISTENING, inserted = true),
        )
        assertTrue(tracker.wasDelivered("Already inserted"))

        tracker.reset()
        assertFalse(tracker.wasDelivered("Already inserted"))
    }
}
