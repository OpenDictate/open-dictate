package com.opendictate.app.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.RandomAccessFile

/** Keeps only the latest dictation in app-private storage, including failed transcriptions. */
class LastDictationAudioStore(private val directory: File) {
    private val file: File get() = File(directory, "last-dictation.wav")

    fun begin(scope: CoroutineScope): Recording {
        directory.mkdirs()
        val wav = WavFile(file)
        return Recording(file, wav, scope)
    }

    fun latestFile(): File? {
        val saved = file.takeIf { it.isFile && it.length() > WavFile.HEADER_SIZE } ?: return null
        // A process killed while recording leaves PCM data behind with an unfinished WAV header.
        val dataSize = saved.length() - WavFile.HEADER_SIZE
        if (dataSize > Int.MAX_VALUE - 36L) return null
        RandomAccessFile(saved, "rw").use { wav ->
            wav.seek(0)
            wav.write(WavFile.header(dataSize.toInt(), PcmAudioRecorder.SAMPLE_RATE))
        }
        return saved
    }

    class Recording internal constructor(val file: File, wav: WavFile, scope: CoroutineScope) {
        private val chunks = Channel<ByteArray>(256)
        private val writer = scope.async(Dispatchers.IO) {
            try {
                for (chunk in chunks) wav.write(chunk)
            } finally {
                chunks.close()
                wav.close()
            }
        }

        fun append(chunk: ByteArray) {
            if (chunks.trySend(chunk).isFailure) {
                // Backpressure is rare, but dropping a chunk would make recovery incomplete.
                runBlocking { chunks.send(chunk) }
            }
        }

        suspend fun finish() {
            chunks.close()
            writer.await()
        }
    }
}
