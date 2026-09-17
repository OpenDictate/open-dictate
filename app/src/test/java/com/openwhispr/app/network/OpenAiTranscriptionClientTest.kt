package com.openwhispr.app.network

import com.openwhispr.app.model.DictationLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class OpenAiTranscriptionClientTest {
    @Test
    fun `realtime transcription URL selects transcription intent without a model`() {
        val query = URI(realtimeTranscriptionUrl()).rawQuery
            .split('&')
            .associate { part ->
                val (name, value) = part.split('=', limit = 2)
                name to value
            }

        assertEquals("transcription", query["intent"])
        assertFalse(query.containsKey("model"))
    }

    @Test
    fun `session update configures live transcription without unsupported delay`() {
        val event = realtimeTranscriptionSessionUpdate(
            languages = setOf(DictationLanguage.ENGLISH, DictationLanguage.RUSSIAN),
            prompt = "OpenWhispr",
        )
        val session = event.getJSONObject("session")
        val input = session.getJSONObject("audio").getJSONObject("input")
        val transcription = input.getJSONObject("transcription")

        assertEquals("session.update", event.getString("type"))
        assertEquals("transcription", session.getString("type"))
        assertEquals("audio/pcm", input.getJSONObject("format").getString("type"))
        assertEquals(24_000, input.getJSONObject("format").getInt("rate"))
        assertEquals("gpt-live-transcribe", transcription.getString("model"))
        assertEquals("OpenWhispr", transcription.getString("prompt"))
        assertEquals(listOf("en", "ru"), transcription.getJSONArray("languages").let { languages ->
            List(languages.length()) { index -> languages.getString(index) }
        })
        assertFalse(transcription.has("delay"))
        assertTrue(input.isNull("turn_detection"))
    }
}
