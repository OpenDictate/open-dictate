package com.openwispr.app.network

import com.openwispr.app.model.DictationLanguage
import com.openwispr.app.model.TextTransformationModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import org.json.JSONArray
import org.json.JSONObject

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
            prompt = "OpenWispr",
        )
        val session = event.getJSONObject("session")
        val input = session.getJSONObject("audio").getJSONObject("input")
        val transcription = input.getJSONObject("transcription")

        assertEquals("session.update", event.getString("type"))
        assertEquals("transcription", session.getString("type"))
        assertEquals("audio/pcm", input.getJSONObject("format").getString("type"))
        assertEquals(24_000, input.getJSONObject("format").getInt("rate"))
        assertEquals("gpt-live-transcribe", transcription.getString("model"))
        assertEquals("OpenWispr", transcription.getString("prompt"))
        assertEquals(listOf("en", "ru"), transcription.getJSONArray("languages").let { languages ->
            List(languages.length()) { index -> languages.getString(index) }
        })
        assertFalse(transcription.has("delay"))
        assertTrue(input.isNull("turn_detection"))
    }

    @Test
    fun `text transformation request uses selected model without storage`() {
        val request = textTransformationRequest(
            model = TextTransformationModel.LUNA,
            sourceText = "Черновик",
            instruction = "Сделай вежливее",
        )

        assertEquals("gpt-5.6-luna", request.getString("model"))
        assertFalse(request.getBoolean("store"))
        assertEquals("low", request.getJSONObject("reasoning").getString("effort"))
        val input = JSONObject(request.getString("input"))
        assertEquals("Сделай вежливее", input.getString("instruction"))
        assertEquals("Черновик", input.getString("text"))
    }

    @Test
    fun `response text extraction skips reasoning and joins text parts`() {
        val response = JSONObject()
            .put(
                "output",
                JSONArray()
                    .put(JSONObject().put("type", "reasoning"))
                    .put(
                        JSONObject()
                            .put("type", "message")
                            .put(
                                "content",
                                JSONArray()
                                    .put(JSONObject().put("type", "refusal").put("refusal", "no"))
                                    .put(JSONObject().put("type", "output_text").put("text", "Готовый "))
                                    .put(JSONObject().put("type", "output_text").put("text", "текст")),
                            ),
                    ),
            )

        assertEquals("Готовый текст", extractResponseText(response))
    }
}
