package com.openwispr.app.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WavFileTest {
    @Test
    fun `header describes mono pcm16 audio`() {
        val header = WavFile.header(dataSize = 4_800, sampleRate = 24_000)

        assertEquals("RIFF", header.copyOfRange(0, 4).toString(Charsets.US_ASCII))
        assertEquals("WAVE", header.copyOfRange(8, 12).toString(Charsets.US_ASCII))
        assertEquals("data", header.copyOfRange(36, 40).toString(Charsets.US_ASCII))
        assertArrayEquals(byteArrayOf(0xC0.toByte(), 0x12, 0, 0), header.copyOfRange(40, 44))
    }
}

