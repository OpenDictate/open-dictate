package com.opendictate.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ModelCatalogTest {
    @Test
    fun `classifies only models compatible with the existing request shapes`() {
        val catalog = ModelCatalog.fromIds(listOf(
            "gpt-live-transcribe", "gpt-live-transcribe-2026-09-01",
            "gpt-transcribe", "gpt-transcribe-2026-09-01",
            "gpt-6-luna", "gpt-7-mini", "gpt-7-astra",
            "gpt-realtime-whisper", "gpt-4o-transcribe", "gpt-6-codex", "gpt-7-pro",
            "gpt-image-2", "omni-moderation-latest",
        ))

        assertEquals(listOf("gpt-live-transcribe-2026-09-01", "gpt-live-transcribe"), catalog.live)
        assertEquals(listOf("gpt-transcribe-2026-09-01", "gpt-transcribe"), catalog.accurate)
        assertEquals(listOf("gpt-7-mini", "gpt-7-astra", "gpt-6-luna"), catalog.text)
    }

    @Test
    fun `parses model ids from API response`() {
        val catalog = ModelCatalog.fromModelsResponse(
            """{"object":"list","data":[{"id":"gpt-transcribe"},{"id":"gpt-7-luna"}]}""",
        )
        assertEquals(listOf("gpt-transcribe"), catalog.accurate)
        assertEquals(listOf("gpt-7-luna"), catalog.text)
        assertFalse(catalog.live.isNotEmpty())
    }
}
