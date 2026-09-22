package com.opendictate.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptFuzzySearchTest {
    @Test
    fun `blank query preserves newest-first history order`() {
        val items = listOf(item(3, "Third"), item(2, "Second"), item(1, "First"))

        assertEquals(items, fuzzySearch(items, "  "))
    }

    @Test
    fun `exact phrase ranks ahead of fuzzy token matches`() {
        val exact = item(1, "Discuss the quarterly roadmap with Alice")
        val fuzzy = item(2, "The quarter road map is ready for Alise")

        assertEquals(
            listOf(exact, fuzzy),
            fuzzySearch(listOf(fuzzy, exact), "quarterly roadmap Alice"),
        )
    }

    @Test
    fun `misspelled words still match`() {
        val expected = item(1, "Позвонить Александру после встречи")
        val unrelated = item(2, "Купить продукты на ужин")

        assertEquals(
            listOf(expected),
            fuzzySearch(listOf(unrelated, expected), "пазванить александру"),
        )
    }

    @Test
    fun `unrelated entries are excluded`() {
        val items = listOf(
            item(1, "Book a table for Friday"),
            item(2, "Prepare the release notes"),
        )

        assertEquals(emptyList<TranscriptHistoryItem>(), fuzzySearch(items, "weather forecast"))
    }

    private fun item(id: Long, text: String) = TranscriptHistoryItem(
        id = id,
        text = text,
        createdAtEpochMillis = id,
    )
}
