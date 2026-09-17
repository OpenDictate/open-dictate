package com.openwhispr.app.service

import org.junit.Assert.assertEquals
import org.junit.Test

class EditableTextSnapshotTest {
    @Test
    fun `inserts transcript at cursor`() {
        val snapshot = EditableTextSnapshot("Hello world", 6, 6)
        assertEquals("Hello brave world", snapshot.compose("brave "))
        assertEquals(12, snapshot.cursorAfter("brave "))
    }

    @Test
    fun `replaces selected text`() {
        val snapshot = EditableTextSnapshot("wrong answer", 0, 5)
        assertEquals("right answer", snapshot.compose("right"))
    }

    @Test
    fun `normalizes reversed selection`() {
        val snapshot = EditableTextSnapshot("one two", 7, 4)
        assertEquals("one three", snapshot.compose("three"))
    }
}

