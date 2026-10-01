package com.opendictate.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.UUID
import java.util.regex.Pattern

data class WordReplacement(
    val id: String = UUID.randomUUID().toString(),
    val source: String,
    val replacement: String,
    val enabled: Boolean = true,
) {
    val isValid: Boolean get() = runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false) &&
        source.isNotBlank() && source.length <= 256 && replacement.isNotBlank() && replacement.length <= 2048

}

data class ReplacementDocument(val rules: List<WordReplacement> = emptyList(), val schemaVersion: Int = 1) {
    val isValid: Boolean get() = schemaVersion == 1 && rules.size <= 500 && rules.all { it.isValid } && rules.map { it.id }.toSet().size == rules.size
    fun toJson(): String = JSONObject().put("schemaVersion", schemaVersion).put("rules", JSONArray().apply {
        rules.forEach { rule -> put(JSONObject().put("id", rule.id).put("source", rule.source).put("replacement", rule.replacement)
            .put("enabled", rule.enabled)) }
    }).toString()
    companion object {
        fun fromJson(json: String): ReplacementDocument {
            val value = JSONObject(json)
            val version = value.get("schemaVersion")
            require(version is Number && version.toDouble() == 1.0)
            val entries = value.getJSONArray("rules")
            require(entries.length() <= 500)
            return ReplacementDocument((0 until entries.length()).map { index ->
                val rule = entries.getJSONObject(index)
                require(listOf("id", "source", "replacement").all { rule.get(it) is String } && rule.get("enabled") is Boolean)
                WordReplacement(rule.getString("id"), rule.getString("source"), rule.getString("replacement"),
                    rule.getBoolean("enabled"))
            }).also { require(it.isValid) }
        }
    }
}

class WordReplacementEngine(rules: List<WordReplacement>) {
    private val rules = rules.filter { it.isValid && it.enabled }.sortedWith(
        compareByDescending<WordReplacement> { it.source.length }.thenBy { it.id },
    )
    private val pattern = this.rules.takeIf { it.isNotEmpty() }?.joinToString("|") {
        "(${Pattern.quote(Normalizer.normalize(it.source, Normalizer.Form.NFC))})"
    }?.let { Pattern.compile("(?<![\\p{L}\\p{M}\\p{N}_])(?:$it)(?![\\p{L}\\p{M}\\p{N}_])", Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE) }

    fun apply(text: String, final: Boolean = true): String {
        val pattern = pattern ?: return text
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
        val match = pattern.matcher(normalized)
        val result = StringBuilder()
        var cursor = 0
        while (match.find()) {
            if (!final && match.end() == normalized.length) continue
            val index = rules.indices.first { match.group(it + 1) != null }
            result.append(normalized, cursor, match.start()).append(rules[index].replacement)
            cursor = match.end()
        }
        return result.append(normalized, cursor, normalized.length).toString()
    }
}
