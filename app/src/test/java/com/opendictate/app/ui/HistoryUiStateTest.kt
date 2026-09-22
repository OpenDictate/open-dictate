package com.opendictate.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryUiStateTest {
    @Test
    fun `typing a query immediately enables fuzzy search`() {
        val state = HistoryUiState().withHistoryQuery("roadmp")

        assertEquals("roadmp", state.query)
        assertEquals(HistorySearchMode.FUZZY, state.searchMode)
    }

    @Test
    fun `editing an AI query returns to fuzzy results`() {
        val state = HistoryUiState(
            query = "roadmap",
            searchMode = HistorySearchMode.AI,
            aiMatchIds = listOf(42),
            isLoading = true,
            errorMessage = "Old error",
        ).withHistoryQuery("roadmaps")

        assertEquals(HistorySearchMode.FUZZY, state.searchMode)
        assertEquals(emptyList<Long>(), state.aiMatchIds)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `clearing the query restores unfiltered history`() {
        val state = HistoryUiState(
            query = "roadmap",
            searchMode = HistorySearchMode.AI,
            aiMatchIds = listOf(42),
        ).withHistoryQuery("   ")

        assertEquals(HistorySearchMode.NONE, state.searchMode)
        assertEquals(emptyList<Long>(), state.aiMatchIds)
    }
}
