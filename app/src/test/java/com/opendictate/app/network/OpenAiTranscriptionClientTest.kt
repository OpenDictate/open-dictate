package com.opendictate.app.network

import com.opendictate.app.model.DictationLanguage
import com.opendictate.app.model.TextTransformationModel
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
            prompt = "OpenDictate",
        )
        val session = event.getJSONObject("session")
        val input = session.getJSONObject("audio").getJSONObject("input")
        val transcription = input.getJSONObject("transcription")

        assertEquals("session.update", event.getString("type"))
        assertEquals("transcription", session.getString("type"))
        assertEquals("audio/pcm", input.getJSONObject("format").getString("type"))
        assertEquals(24_000, input.getJSONObject("format").getInt("rate"))
        assertEquals("gpt-live-transcribe", transcription.getString("model"))
        assertEquals(
            "Cyrillic text uses Russian orthography.\nOpenDictate",
            transcription.getString("prompt"),
        )
        assertEquals(listOf("en", "ru"), transcription.getJSONArray("languages").let { languages ->
            List(languages.length()) { index -> languages.getString(index) }
        })
        assertFalse(transcription.has("delay"))
        assertTrue(input.isNull("turn_detection"))
    }

    @Test
    fun `Russian transcription context does not override selected Ukrainian`() {
        val event = realtimeTranscriptionSessionUpdate(
            languages = setOf(DictationLanguage.RUSSIAN, DictationLanguage.UKRAINIAN),
            prompt = "OpenDictate",
        )
        val transcription = event
            .getJSONObject("session")
            .getJSONObject("audio")
            .getJSONObject("input")
            .getJSONObject("transcription")

        assertEquals("OpenDictate", transcription.getString("prompt"))
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
        val format = request.getJSONObject("text").getJSONObject("format")
        assertEquals("json_schema", format.getString("type"))
        assertTrue(format.getBoolean("strict"))
        val schema = format.getJSONObject("schema")
        assertFalse(schema.getBoolean("additionalProperties"))
        assertEquals(
            listOf("transformed_text", "message"),
            schema.getJSONArray("required").let { required ->
                List(required.length()) { index -> required.getString(index) }
            },
        )
        val input = JSONObject(request.getString("input"))
        assertEquals("Сделай вежливее", input.getString("instruction"))
        assertEquals("Черновик", input.getString("source_text"))
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

    @Test
    fun `transformation result normally has no user message`() {
        val response = transformationResponse(
            transformedText = "Готовый текст",
            message = JSONObject.NULL,
        )

        assertEquals(
            TextTransformationResult(text = "Готовый текст", message = null),
            extractTransformationResult(response),
        )
    }

    @Test
    fun `transformation result exposes an attached user message`() {
        val response = transformationResponse(
            transformedText = "Исходный текст",
            message = "Не удалось определить адресата.",
        )

        assertEquals(
            TextTransformationResult(
                text = "Исходный текст",
                message = "Не удалось определить адресата.",
            ),
            extractTransformationResult(response),
        )
    }

    @Test
    fun `history search request uses structured output without storage`() {
        val request = transcriptHistorySearchRequest(
            model = TextTransformationModel.LUNA,
            query = "project planning",
            documents = listOf(
                TranscriptSearchDocument(7, "Discuss the roadmap"),
                TranscriptSearchDocument(9, "Buy coffee"),
            ),
        )

        assertEquals("gpt-5.6-luna", request.getString("model"))
        assertFalse(request.getBoolean("store"))
        val format = request.getJSONObject("text").getJSONObject("format")
        assertEquals("json_schema", format.getString("type"))
        assertTrue(format.getBoolean("strict"))
        val input = JSONObject(request.getString("input"))
        assertEquals("project planning", input.getString("semantic_query"))
        assertEquals(7, input.getJSONArray("documents").getJSONObject(0).getLong("id"))
    }

    @Test
    fun `history search matches are extracted and deduplicated`() {
        val response = JSONObject().put(
            "output",
            JSONArray().put(
                JSONObject()
                    .put("type", "message")
                    .put(
                        "content",
                        JSONArray().put(
                            JSONObject()
                                .put("type", "output_text")
                                .put("text", JSONObject().put("match_ids", JSONArray(listOf(9, 7, 9))).toString()),
                        ),
                    ),
            ),
        )

        assertEquals(listOf(9L, 7L), extractTranscriptHistoryMatches(response))
    }

    @Test
    fun `history search batches bound document count`() {
        val documents = (1L..51L).map { TranscriptSearchDocument(it, "text") }

        assertEquals(listOf(50, 1), historySearchBatches(documents).map(List<*>::size))
    }

    private fun transformationResponse(
        transformedText: String,
        message: Any,
    ): JSONObject = JSONObject().put(
        "output",
        JSONArray().put(
            JSONObject()
                .put("type", "message")
                .put(
                    "content",
                    JSONArray().put(
                        JSONObject()
                            .put("type", "output_text")
                            .put(
                                "text",
                                JSONObject()
                                    .put("transformed_text", transformedText)
                                    .put("message", message)
                                    .toString(),
                            ),
                    ),
                ),
        ),
    )
}
