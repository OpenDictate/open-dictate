package com.openwispr.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditableTextSnapshotTest {
    @Test
    fun `does not capture a field hint as user text`() {
        val snapshot = EditableTextSnapshot.capture(
            displayedText = "Ask anything",
            selectionStart = 0,
            selectionEnd = 0,
            isShowingHintText = true,
        )

        assertEquals("hello", snapshot.compose("hello"))
    }

    @Test
    fun `preserves actual user text when hint is not showing`() {
        val snapshot = EditableTextSnapshot.capture(
            displayedText = "Draft",
            selectionStart = 5,
            selectionEnd = 5,
            isShowingHintText = false,
        )

        assertEquals("Draft text", snapshot.compose(" text"))
    }

    @Test
    fun `reports whether field contains user text`() {
        assertTrue(EditableTextSnapshot("Draft", 0, 0).hasText)
        assertFalse(EditableTextSnapshot("   ", 0, 0).hasText)
        assertFalse(
            EditableTextSnapshot.capture(
                displayedText = "Ask anything",
                selectionStart = 0,
                selectionEnd = 0,
                isShowingHintText = true,
            ).hasText,
        )
    }

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
    fun `restoration returns the original text and selection`() {
        val snapshot = EditableTextSnapshot("wrong answer", 5, 0)

        assertEquals(
            EditableTextUpdate("wrong answer", 0, 5),
            snapshot.restoration(),
        )
    }

    @Test
    fun `normalizes reversed selection`() {
        val snapshot = EditableTextSnapshot("one two", 7, 4)
        assertEquals("one three", snapshot.compose("three"))
    }

    @Test
    fun `transformation targets selected text`() {
        val target = EditableTextSnapshot("Make this clearer", 5, 9)
            .transformationTarget()!!

        assertEquals("this", target.sourceText)
        assertEquals(
            "Make everything clearer",
            target.replacementSnapshot.compose("everything"),
        )
    }

    @Test
    fun `transformation targets whole field when selection is collapsed`() {
        val target = EditableTextSnapshot("Make this clearer", 9, 9)
            .transformationTarget()!!

        assertEquals("Make this clearer", target.sourceText)
        assertEquals("Clearer", target.replacementSnapshot.compose("Clearer"))
    }

    @Test
    fun `blank field has no transformation target`() {
        assertNull(EditableTextSnapshot("   ", 0, 0).transformationTarget())
    }

    @Test
    fun `failed restoration can be retried until it succeeds`() {
        val snapshot = EditableTextSnapshot("Draft", 5, 5)
        val restoration = PendingEditableTextRestoration(maxAttempts = 3)

        restoration.begin(snapshot)
        assertEquals(snapshot.restoration(), restoration.nextAttempt())
        assertEquals(snapshot.restoration(), restoration.nextAttempt())

        restoration.complete()

        assertNull(restoration.nextAttempt())
        assertFalse(restoration.isPending)
    }

    @Test
    fun `restoration retries are bounded`() {
        val snapshot = EditableTextSnapshot("Draft", 5, 5)
        val restoration = PendingEditableTextRestoration(maxAttempts = 2)

        restoration.begin(snapshot)
        assertEquals(snapshot.restoration(), restoration.nextAttempt())
        assertEquals(snapshot.restoration(), restoration.nextAttempt())
        assertNull(restoration.nextAttempt())
        assertFalse(restoration.isPending)
    }
}
