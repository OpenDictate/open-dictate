package com.opendictate.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelOptionsTest {
    @Test
    fun `uses fetched models without stale defaults`() {
        assertEquals(
            listOf("gpt-7-luna"),
            modelOptions(listOf("gpt-7-luna"), listOf("gpt-6-luna")),
        )
    }

    @Test
    fun `uses bundled defaults when discovery is unavailable`() {
        assertEquals(
            listOf("gpt-transcribe"),
            modelOptions(emptyList(), listOf("gpt-transcribe")),
        )
    }
}
