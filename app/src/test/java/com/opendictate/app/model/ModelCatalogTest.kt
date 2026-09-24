package com.opendictate.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ModelCatalogTest {
    @Test
    fun `shows only current supported roles and skips old versions and snapshots`() {
        val catalog = ModelCatalog.fromIds(listOf(
            "gpt-live-transcribe", "gpt-live-transcribe-2026-09-01",
            "gpt-transcribe", "gpt-transcribe-2026-09-01",
            "gpt-5.6-luna", "gpt-6-luna", "gpt-7-luna", "gpt-7-luna-2026-09-01",
            "gpt-6-sol", "gpt-7-sol", "gpt-7-mini", "gpt-7-astra",
            "gpt-realtime-whisper", "gpt-4o-transcribe", "gpt-6-codex", "gpt-7-pro",
            "gpt-image-2", "omni-moderation-latest",
        ))

        assertEquals(listOf("gpt-live-transcribe"), catalog.live)
        assertEquals(listOf("gpt-transcribe"), catalog.accurate)
        assertEquals(listOf("gpt-7-luna", "gpt-7-sol"), catalog.text)
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

    @Test
    fun `compares numeric versions rather than sorting model names`() {
        val catalog = ModelCatalog.fromIds(listOf(
            "gpt-6.9-luna", "gpt-6.10-luna", "gpt-7-sol", "gpt-8-sol",
        ))
        assertEquals(listOf("gpt-6.10-luna", "gpt-8-sol"), catalog.text)
    }

    @Test
    fun `replaces an old or unsuitable selection with the current model of the same role`() {
        val available = listOf("gpt-7-luna", "gpt-7-sol")
        assertEquals("gpt-7-sol", preferredModelId("gpt-6-sol", available, ModelCatalog.DEFAULT.text))
        assertEquals("gpt-7-luna", preferredModelId("gpt-7-astra", available, ModelCatalog.DEFAULT.text))
        assertEquals("gpt-transcribe", preferredModelId(
            "gpt-transcribe-2026-09-01", emptyList(), ModelCatalog.DEFAULT.accurate,
        ))
    }
}
