package com.opendictate.app.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opendictate.app.model.TranscriptionModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PunctuationRuntimeTest {
    @Test fun correctionValidatesUnicodeWhitespaceAndNumbersOnAndroid() = runBlocking {
        assertEquals("Привет, мир!", PunctuationCorrection.apply("привет\u00a0мир", true, TranscriptionModel.ACCURATE) {
            "Привет, мир!"
        })
        assertEquals("цена 1,5", PunctuationCorrection.validated("Цена 15.", "цена 1,5"))
        assertEquals("привет", PunctuationCorrection.apply("привет", true, TranscriptionModel.ACCURATE) {
            throw java.io.IOException("Synthetic provider failure")
        })
        assertEquals("привет", PunctuationCorrection.apply("привет", true, TranscriptionModel.LIVE) {
            error("Live must not send a correction request")
        })
    }

    @Test fun disabledCorrectionReturnsTranscriptOnAndroid() = runBlocking {
        assertEquals("привет мир", PunctuationCorrection.apply("привет мир", false, TranscriptionModel.ACCURATE) {
            error("Disabled correction must not send a request")
        })
    }
}
