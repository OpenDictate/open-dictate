package com.openwhispr.app.service

data class EditableTextSnapshot(
    val original: String,
    val selectionStart: Int,
    val selectionEnd: Int,
) {
    private val safeStart = minOf(selectionStart, selectionEnd).coerceIn(0, original.length)
    private val safeEnd = maxOf(selectionStart, selectionEnd).coerceIn(safeStart, original.length)

    fun compose(transcript: String): String =
        original.substring(0, safeStart) + transcript + original.substring(safeEnd)

    fun cursorAfter(transcript: String): Int = safeStart + transcript.length

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
            selectionStart: Int,
            selectionEnd: Int,
            isShowingHintText: Boolean,
        ): EditableTextSnapshot {
            val original = if (isShowingHintText) "" else displayedText?.toString().orEmpty()
            return EditableTextSnapshot(original, selectionStart, selectionEnd)
        }
    }
}

data class TextTransformationTarget(
    val sourceText: String,
    val replacementSnapshot: EditableTextSnapshot,
)
