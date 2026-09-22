package com.openwispr.app.data

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

internal fun fuzzySearch(
    items: List<TranscriptHistoryItem>,
    rawQuery: String,
): List<TranscriptHistoryItem> {
    val query = normalizeForSearch(rawQuery)
    if (query.isBlank()) return items

    return items.mapNotNull { item ->
        fuzzyScore(normalizeForSearch(item.text), query)?.let { score -> item to score }
    }.sortedWith(
        compareByDescending<Pair<TranscriptHistoryItem, Double>> { it.second }
            .thenByDescending { it.first.createdAtEpochMillis }
            .thenByDescending { it.first.id },
    ).map(Pair<TranscriptHistoryItem, Double>::first)
}

private fun fuzzyScore(text: String, query: String): Double? {
    if (text.isBlank()) return null
    val exactIndex = text.indexOf(query)
    if (exactIndex >= 0) return 10.0 - exactIndex.coerceAtMost(1_000) / 10_000.0
    if (query.length < 2) return null

    val queryTokens = query.split(' ').filter(String::isNotBlank)
    val textTokens = text.split(' ').filter(String::isNotBlank)
    if (textTokens.isEmpty()) return null

    val tokenScores = queryTokens.map { queryToken ->
        textTokens.maxOf { textToken -> tokenSimilarity(queryToken, textToken) }
    }
    val average = tokenScores.average()
    val coverage = tokenScores.count { it >= TOKEN_MATCH_THRESHOLD }.toDouble() / queryTokens.size
    if (average < AVERAGE_MATCH_THRESHOLD || coverage < COVERAGE_THRESHOLD) return null
    return average + coverage * 2.0
}

private fun tokenSimilarity(left: String, right: String): Double {
    if (left == right) return 1.0
    if (left in right || right in left) {
        val overlap = left.length.coerceAtMost(right.length).toDouble() /
            left.length.coerceAtLeast(right.length)
        return if (overlap >= 0.65) 0.72 + overlap * 0.28 else overlap
    }
    return 1.0 - levenshteinDistance(left, right).toDouble() / max(left.length, right.length)
}

private fun levenshteinDistance(left: String, right: String): Int {
    var previous = IntArray(right.length + 1) { it }
    for (leftIndex in left.indices) {
        val current = IntArray(right.length + 1)
        current[0] = leftIndex + 1
        for (rightIndex in right.indices) {
            val substitution = previous[rightIndex] + if (left[leftIndex] == right[rightIndex]) 0 else 1
            current[rightIndex + 1] = minOf(
                current[rightIndex] + 1,
                previous[rightIndex + 1] + 1,
                substitution,
            )
        }
        previous = current
    }
    return previous[right.length]
}

private fun normalizeForSearch(value: String): String = Normalizer.normalize(
    value.lowercase(Locale.ROOT),
    Normalizer.Form.NFKD,
).replace(COMBINING_MARKS, "")
    .replace(NON_ALPHANUMERIC, " ")
    .trim()
    .replace(MULTIPLE_SPACES, " ")

private val COMBINING_MARKS = Regex("\\p{M}+")
private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
private val MULTIPLE_SPACES = Regex("\\s+")
private const val TOKEN_MATCH_THRESHOLD = 0.62
private const val AVERAGE_MATCH_THRESHOLD = 0.58
private const val COVERAGE_THRESHOLD = 0.6
