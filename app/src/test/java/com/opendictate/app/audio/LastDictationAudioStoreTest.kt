package com.opendictate.app.audio

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File
import java.nio.file.Files

class LastDictationAudioStoreTest {
    @Test
    fun `failed transcription can retry the complete latest recording`() = runBlocking {
        val directory = Files.createTempDirectory("last-dictation-test").toFile()
        try {
            val store = LastDictationAudioStore(directory)
            val first = store.begin(this)
            first.append(byteArrayOf(1, 2, 3, 4))
            first.finish()
            val firstSaved = store.latestFile()
            assertNotNull(firstSaved)
            assertEquals(4, firstSaved!!.length() - WavFile.HEADER_SIZE)

            val second = store.begin(this)
            second.append(byteArrayOf(5, 6))
            second.finish()
            val saved = store.latestFile()
            assertNotNull(saved)
            assertArrayEquals(byteArrayOf(5, 6), saved!!.readBytes().copyOfRange(44, 46))
            assertEquals(1, directory.listFiles()?.size)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `recording interrupted by process death is repaired on next access`() {
        val directory = Files.createTempDirectory("last-dictation-test").toFile()
        try {
            val file = File(directory, "last-dictation.wav")
            file.writeBytes(ByteArray(WavFile.HEADER_SIZE) + byteArrayOf(1, 2, 3, 4))
            val saved = LastDictationAudioStore(directory).latestFile()
            assertNotNull(saved)
            assertArrayEquals(WavFile.header(4), saved!!.readBytes().copyOfRange(0, 44))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `empty recording is unavailable`() {
        val directory = Files.createTempDirectory("last-dictation-test").toFile()
        try {
            File(directory, "last-dictation.wav").writeBytes(ByteArray(WavFile.HEADER_SIZE))
            assertFalse(LastDictationAudioStore(directory).latestFile() != null)
        } finally {
            directory.deleteRecursively()
        }
    }
}
