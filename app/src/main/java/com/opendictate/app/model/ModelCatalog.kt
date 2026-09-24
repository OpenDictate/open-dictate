package com.opendictate.app.model

import org.json.JSONArray
import org.json.JSONObject

/** The Models API has no endpoint or price metadata; text candidates are limited to general GPT tiers. */
data class ModelCatalog(
    val live: List<String>,
    val accurate: List<String>,
    val text: List<String>,
) {
    fun toCachedJson(): String = JSONObject()
        .put("live", JSONArray(live))
        .put("accurate", JSONArray(accurate))
        .put("text", JSONArray(text))
        .toString()

    companion object {
        private const val MAX_TEXT_MODELS = 5
        val DEFAULT = ModelCatalog(
            live = listOf("gpt-live-transcribe"),
            accurate = listOf("gpt-transcribe"),
            text = listOf("gpt-6-luna", "gpt-6-sol"),
        )

        fun fromModelsResponse(body: String): ModelCatalog {
            val data = JSONObject(body).getJSONArray("data")
            val models = (0 until data.length()).map { data.getJSONObject(it) }
            val ids = models.map { it.getString("id") }.toSet()
            val text = models.asSequence()
                .filter { it.optString("id").matches(TEXT_MODEL) && it.isNull("shutdown_date") }
                .sortedWith(compareByDescending<JSONObject> { it.getLong("created") }
                    .thenByDescending { it.getString("id") })
                .map { it.getString("id") }
                .distinct()
                .take(MAX_TEXT_MODELS)
                .toList()
            return ModelCatalog(
                live = listOfNotNull(DEFAULT.live.first().takeIf(ids::contains)),
                accurate = listOfNotNull(DEFAULT.accurate.first().takeIf(ids::contains)),
                text = text,
            )
        }

        fun fromCachedJson(value: String?): ModelCatalog {
            if (value.isNullOrBlank()) return ModelCatalog(emptyList(), emptyList(), emptyList())
            return runCatching {
                val cached = JSONObject(value)
                val text = cached.getJSONArray("text").strings()
                    .filter { it.matches(TEXT_MODEL) }.distinct().take(MAX_TEXT_MODELS)
                ModelCatalog(
                    live = cached.getJSONArray("live").strings().filter { it == DEFAULT.live.first() },
                    accurate = cached.getJSONArray("accurate").strings().filter { it == DEFAULT.accurate.first() },
                    text = text,
                )
            }.getOrElse { ModelCatalog(emptyList(), emptyList(), emptyList()) }
        }
    }
}

private val TEXT_MODEL = Regex("gpt-(?:[5-9]|[1-9][0-9]+)(?:\\.[0-9]+)?-(?:nano|mini|luna|terra|sol)|gpt-4o-mini")

private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

fun preferredModelId(selected: String, available: List<String>, defaults: List<String>): String {
    val options = available.ifEmpty { defaults }
    if (selected in options) return selected
    val role = selected.substringAfterLast('-', missingDelimiterValue = "")
    return options.firstOrNull { it.endsWith("-$role") } ?: options.first()
}
