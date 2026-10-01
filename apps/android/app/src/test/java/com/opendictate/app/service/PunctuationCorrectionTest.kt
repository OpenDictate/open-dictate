package com.opendictate.app.service

import com.opendictate.app.model.TranscriptionModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class PunctuationCorrectionTest {
    @Test fun `shared validation preserves words numbers addresses and symbols`() {
        val root = generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { java.io.File(it, "shared/punctuation-correction.json").isFile }
        val examples = JSONArray(java.io.File(root, "shared/punctuation-correction.json").readText())
        for (index in 0 until examples.length()) {
            val example = examples.getJSONObject(index)
            assertEquals(example.getString("expected"), PunctuationCorrection.validated(
                example.getString("candidate"), example.getString("source")))
        }
    }

    @Test fun `only enabled Accurate calls the provider`() = runTest {
        var requests = 0
        for ((enabled, model, source) in listOf(Triple(false, TranscriptionModel.ACCURATE, "привет"),
            Triple(true, TranscriptionModel.LIVE, "привет"), Triple(true, TranscriptionModel.ACCURATE, " "))) {
            assertEquals(source, PunctuationCorrection.apply(source, enabled, model) { requests++; "Привет!" })
        }
        assertEquals(0, requests)
        assertEquals("Привет!", PunctuationCorrection.apply("привет", true, TranscriptionModel.ACCURATE) { requests++; "Привет!" })
        assertEquals(1, requests)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun `failures and timeout retain source but cancellation never delivers it`() = runTest {
        assertEquals("привет", PunctuationCorrection.apply("привет", true, TranscriptionModel.ACCURATE) {
            throw java.io.IOException("synthetic failure")
        })
        assertEquals("привет", PunctuationCorrection.apply("привет", true, TranscriptionModel.ACCURATE) { awaitCancellation() })
        assertEquals(10_000, testScheduler.currentTime)
        try {
            PunctuationCorrection.apply("привет", true, TranscriptionModel.ACCURATE) { throw CancellationException() }
            fail("Cancellation must not deliver text")
        } catch (_: CancellationException) { }
    }

    @Test fun `final period preference is applied after correction`() = runTest {
        val corrected = PunctuationCorrection.apply("привет мир", true, TranscriptionModel.ACCURATE) { "Привет, мир." }
        assertEquals("Привет, мир", TranscriptFormatter.formatFinal(corrected, false))
        assertEquals("Привет, мир.", TranscriptFormatter.formatFinal(corrected, true))
    }
}
