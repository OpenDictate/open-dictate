package com.opendictate.app.model

import org.json.JSONObject

/** The Models API has no endpoint, price, or quality metadata. Only expose reviewed model roles. */
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
            val available = ids.toSet()
            val text = available.mapNotNull { id ->
                val match = TEXT_MODEL.matchEntire(id) ?: return@mapNotNull null
                val major = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                if (major < MIN_TEXT_MAJOR) return@mapNotNull null
                val minor = match.groupValues[2].ifEmpty { "0" }.toIntOrNull()
                    ?: return@mapNotNull null
                TextCandidate(id, major, minor, match.groupValues[3])
            }
            val newestMajor = text.maxOfOrNull(TextCandidate::major) ?: MIN_TEXT_MAJOR
            val recentText = text.filter { it.major >= newestMajor - 1 }
            return ModelCatalog(
                live = listOfNotNull(DEFAULT.live.first().takeIf(available::contains)),
                accurate = listOfNotNull(DEFAULT.accurate.first().takeIf(available::contains)),
                text = listOfNotNull(
                    recentText.latest("luna"),
                    recentText.latest("sol"),
                ),
            )
        }
    }
}

private const val MIN_TEXT_MAJOR = 6
private val TEXT_MODEL = Regex("gpt-([0-9]+)(?:\\.([0-9]+))?-(luna|sol)")

private data class TextCandidate(val id: String, val major: Int, val minor: Int, val role: String)

private fun List<TextCandidate>.latest(role: String): String? =
    filter { it.role == role }.maxWithOrNull(compareBy<TextCandidate> { it.major }.thenBy { it.minor })?.id

fun preferredModelId(selected: String, available: List<String>, defaults: List<String>): String {
    val options = available.ifEmpty { defaults }
    if (selected in options) return selected
    val role = selected.substringAfterLast('-', missingDelimiterValue = "")
    return options.firstOrNull { it.endsWith("-$role") } ?: options.first()
}
