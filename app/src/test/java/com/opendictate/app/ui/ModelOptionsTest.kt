package com.opendictate.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelOptionsTest {
    @Test
    fun `uses fetched models and keeps the user's previous selection`() {
        assertEquals(
            listOf("gpt-7-luna", "gpt-6-luna"),
            modelOptions(listOf("gpt-7-luna"), "gpt-6-luna", listOf("gpt-5-luna")),
        )
    }

    @Test
    fun `uses bundled defaults when discovery is unavailable`() {
        assertEquals(
            listOf("gpt-transcribe"),
            modelOptions(emptyList(), "gpt-transcribe", listOf("gpt-transcribe")),
        )
    }
}
