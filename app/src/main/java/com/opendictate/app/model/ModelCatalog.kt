package com.opendictate.app.model

import org.json.JSONObject

/** The Models API exposes IDs, but no endpoint capability metadata. Keep these filters conservative. */
data class ModelCatalog(
    val live: List<String>,
    val accurate: List<String>,
    val text: List<String>,
) {
    companion object {
        val DEFAULT = ModelCatalog(
            live = listOf("gpt-live-transcribe"),
            accurate = listOf("gpt-transcribe"),
            text = listOf("gpt-6-luna", "gpt-6-sol"),
        )

        fun fromModelsResponse(body: String): ModelCatalog {
            val data = JSONObject(body).getJSONArray("data")
            val ids = (0 until data.length()).map { data.getJSONObject(it).getString("id") }
            return fromIds(ids)
        }

        fun fromIds(ids: Collection<String>): ModelCatalog {
            val unique = ids.distinct()
            return ModelCatalog(
                live = unique.filter { it.matches(LIVE_MODEL) }.sortedDescending(),
                accurate = unique.filter { it.matches(FILE_MODEL) }.sortedDescending(),
                text = unique.filter { it.matches(TEXT_MODEL) && TEXT_EXCLUDED.none(it::contains) }
                    .sortedDescending(),
            )
        }
    }
}

private val LIVE_MODEL = Regex("gpt-live-transcribe(?:-[0-9][a-z0-9.-]*)?")
private val FILE_MODEL = Regex("gpt-transcribe(?:-[0-9][a-z0-9.-]*)?")
private val TEXT_MODEL = Regex("gpt-[0-9]+(?:\\.[0-9]+)?(?:-[a-z0-9]+)*(?:-[0-9]{4}-[0-9]{2}-[0-9]{2})?")
private val TEXT_EXCLUDED = listOf(
    "transcribe", "realtime", "audio", "image", "tts", "codex", "chat", "search", "pro",
)
