package com.openwispr.app.service

internal enum class TranscriptDeliveryOutcome {
    PENDING,
    INSERTED,
    COPY_TO_CLIPBOARD,
}

internal class TranscriptDeliveryTracker {
    private var lastDeliveredTranscript = ""

    fun wasDelivered(transcript: String): Boolean = transcript == lastDeliveredTranscript

    fun record(
        transcript: String,
        phase: DictationPhase,
        inserted: Boolean,
    ): TranscriptDeliveryOutcome {
        if (inserted) {
            lastDeliveredTranscript = transcript
            return TranscriptDeliveryOutcome.INSERTED
        }
        if (phase == DictationPhase.COMPLETED) {
            lastDeliveredTranscript = transcript
            return TranscriptDeliveryOutcome.COPY_TO_CLIPBOARD
        }
        return TranscriptDeliveryOutcome.PENDING
    }

    fun reset() {
        lastDeliveredTranscript = ""
    }
}
