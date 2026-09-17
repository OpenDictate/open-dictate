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
