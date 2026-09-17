package com.openwispr.app.service

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptFormatterTest {
    @Test
    fun `removes one trailing period when disabled`() {
        assertEquals(
            "Первое предложение. Второе",
            TranscriptFormatter.format("Первое предложение. Второе.", keepTrailingPeriod = false),
        )
    }

    @Test
    fun `keeps trailing period when enabled`() {
        assertEquals(
            "Предложение.",
            TranscriptFormatter.format("Предложение.", keepTrailingPeriod = true),
        )
    }

    @Test
    fun `adds trailing period to final transcript when enabled`() {
        assertEquals(
            "Предложение.",
            TranscriptFormatter.formatFinal("Предложение", keepTrailingPeriod = true),
        )
    }

    @Test
    fun `adds final period before trailing whitespace`() {
        assertEquals(
            "Предложение.  ",
            TranscriptFormatter.formatFinal("Предложение  ", keepTrailingPeriod = true),
        )
    }

    @Test
    fun `does not add final period after terminal punctuation`() {
        assertEquals(
            "Вопрос?",
            TranscriptFormatter.formatFinal("Вопрос?", keepTrailingPeriod = true),
        )
        assertEquals(
            "Ответ!",
            TranscriptFormatter.formatFinal("Ответ!", keepTrailingPeriod = true),
        )
    }

    @Test
    fun `does not remove question mark or ellipsis`() {
        assertEquals(
            "Вопрос?",
            TranscriptFormatter.format("Вопрос?", keepTrailingPeriod = false),
        )
        assertEquals(
            "Пауза...",
            TranscriptFormatter.format("Пауза...", keepTrailingPeriod = false),
        )
    }

    @Test
    fun `preserves whitespace after removed period`() {
        assertEquals(
            "Фраза  ",
            TranscriptFormatter.format("Фраза.  ", keepTrailingPeriod = false),
        )
    }
}
