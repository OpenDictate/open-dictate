package com.opendictate.app.service

data class EditableTextSnapshot(
    val original: String,
    val selectionStart: Int,
    val selectionEnd: Int,
) {
    private val safeStart = minOf(selectionStart, selectionEnd).coerceIn(0, original.length)
    private val safeEnd = maxOf(selectionStart, selectionEnd).coerceIn(safeStart, original.length)

    val hasText: Boolean
        get() = original.isNotBlank()

    fun compose(transcript: String): String =
        original.substring(0, safeStart) + transcript + original.substring(safeEnd)

    fun cursorAfter(transcript: String): Int = safeStart + transcript.length

    fun restoration(): EditableTextUpdate = EditableTextUpdate(
        text = original,
        selectionStart = safeStart,
        selectionEnd = safeEnd,
    )

    fun transformationTarget(): TextTransformationTarget? {
        val targetStart = if (safeStart == safeEnd) 0 else safeStart
        val targetEnd = if (safeStart == safeEnd) original.length else safeEnd
        val sourceText = original.substring(targetStart, targetEnd)
        if (sourceText.isBlank()) return null
        return TextTransformationTarget(
            sourceText = sourceText,
            replacementSnapshot = EditableTextSnapshot(original, targetStart, targetEnd),
        )
    }

    companion object {
        fun capture(
            displayedText: CharSequence?,
            hintText: CharSequence?,
            selectionStart: Int,
            selectionEnd: Int,
            isShowingHintText: Boolean,
        ): EditableTextSnapshot {
            val displayed = displayedText?.toString().orEmpty()
            val hint = hintText?.toString()
            val displaysHint = isShowingHintText || hint != null && displayed == hint
            val original = if (displaysHint) "" else displayed
            return EditableTextSnapshot(original, selectionStart, selectionEnd)
        }
    }
}

data class EditableTextUpdate(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
)

internal class PendingEditableTextRestoration(
    private val maxAttempts: Int = 5,
) {
    private var snapshot: EditableTextSnapshot? = null
    private var attemptsRemaining = 0

    init {
        require(maxAttempts > 0)
    }

    val isPending: Boolean
        get() = snapshot != null

    fun begin(snapshot: EditableTextSnapshot) {
        this.snapshot = snapshot
        attemptsRemaining = maxAttempts
    }

    fun nextAttempt(): EditableTextUpdate? {
        val current = snapshot ?: return null
        if (attemptsRemaining <= 0) {
            clear()
            return null
        }
        attemptsRemaining -= 1
        return current.restoration()
    }

    fun confirmApplied(
        displayedText: CharSequence?,
        selectionStart: Int,
        selectionEnd: Int,
    ): Boolean {
        val expected = snapshot?.restoration() ?: return false
        val actualStart = minOf(selectionStart, selectionEnd)
        val actualEnd = maxOf(selectionStart, selectionEnd)
        val matches = displayedText?.toString().orEmpty() == expected.text &&
            actualStart == expected.selectionStart &&
            actualEnd == expected.selectionEnd
        if (matches) clear()
        return matches
    }

    fun clear() {
        snapshot = null
        attemptsRemaining = 0
    }
}

data class TextTransformationTarget(
    val sourceText: String,
    val replacementSnapshot: EditableTextSnapshot,
)
