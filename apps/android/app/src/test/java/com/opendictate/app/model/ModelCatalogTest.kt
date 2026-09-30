package com.opendictate.app.model

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {
    @Test
    fun `offers five newest general text models but no Astra or specialized models`() {
        val catalog = ModelCatalog.fromModelsResponse(apiResponse(
            "gpt-5.6-luna" to 10,
            "gpt-5.6-terra" to 20,
            "gpt-5.6-sol" to 30,
            "gpt-6-luna" to 40,
            "gpt-6-sol" to 50,
            "gpt-7-mini" to 60,
            "gpt-7-astra" to 100,
            "gpt-7-mini-2026-09-24" to 101,
            "gpt-7-codex" to 102,
            "gpt-7-pro" to 103,
            "gpt-live-transcribe" to 1,
            "gpt-transcribe" to 1,
        ))

        assertEquals(listOf("gpt-live-transcribe"), catalog.live)
        assertEquals(listOf("gpt-transcribe"), catalog.accurate)
        assertEquals(listOf(
            "gpt-7-mini", "gpt-6-sol", "gpt-6-luna", "gpt-5.6-sol", "gpt-5.6-terra",
        ), catalog.text)
    }

    @Test
    fun `created timestamp determines recency rather than the version number`() {
        val catalog = ModelCatalog.fromModelsResponse(apiResponse(
            "gpt-7-luna" to 10,
            "gpt-6-luna" to 20,
        ))
        assertEquals(listOf("gpt-6-luna", "gpt-7-luna"), catalog.text)
    }

    @Test
    fun `models with announced shutdown are omitted`() {
        val body = JSONObject().put("data", JSONArray().put(
            JSONObject().put("id", "gpt-7-sol").put("created", 20)
                .put("shutdown_date", "2026-10-23"),
        )).toString()
        assertTrue(ModelCatalog.fromModelsResponse(body).text.isEmpty())
    }

    @Test
    fun `cache preserves ranking and rejects unsuitable ids`() {
        val original = ModelCatalog(
            live = listOf("gpt-live-transcribe"),
            accurate = listOf("gpt-transcribe"),
            text = listOf("gpt-7-mini", "gpt-6-luna", "gpt-5.6-terra"),
        )
        assertEquals(original, ModelCatalog.fromCachedJson(original.toCachedJson()))
        val polluted = ModelCatalog(
            live = emptyList(), accurate = emptyList(),
            text = listOf("gpt-7-astra", "gpt-7-mini", "gpt-7-mini"),
        )
        assertEquals(listOf("gpt-7-mini"), ModelCatalog.fromCachedJson(polluted.toCachedJson()).text)
    }

    @Test
    fun `replaces an unsuitable selection with an available model`() {
        val available = listOf("gpt-7-mini", "gpt-6-luna", "gpt-6-sol")
        assertEquals("gpt-6-sol", preferredModelId("gpt-5.6-sol", available, ModelCatalog.DEFAULT.text))
        assertEquals("gpt-7-mini", preferredModelId("gpt-7-astra", available, ModelCatalog.DEFAULT.text))
    }
}

private fun apiResponse(vararg entries: Pair<String, Int>): String {
    val models = JSONArray()
    entries.forEach { (id, created) ->
        models.put(JSONObject().put("id", id).put("created", created))
    }
    return JSONObject().put("data", models).toString()
}
