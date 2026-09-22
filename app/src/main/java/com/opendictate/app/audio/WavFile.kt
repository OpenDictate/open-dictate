package com.opendictate.app.audio

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean

class WavFile(
    private val file: File,
    private val sampleRate: Int = 24_000,
) : AutoCloseable {
    private val output = FileOutputStream(file)
    private var dataSize = 0L
    private val closed = AtomicBoolean(false)

    init {
        output.write(ByteArray(HEADER_SIZE))
    }

    @Synchronized
    fun write(bytes: ByteArray) {
        check(!closed.get()) { "WAV file is already closed" }
        output.write(bytes)
        dataSize += bytes.size
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        output.flush()
        output.close()
        RandomAccessFile(file, "rw").use { wav ->
            wav.seek(0)
            wav.write(header(dataSize.toInt(), sampleRate))
        }
    }

    companion object {
        const val HEADER_SIZE = 44

        fun header(dataSize: Int, sampleRate: Int = 24_000): ByteArray {
            val byteRate = sampleRate * 2
            return ByteArray(HEADER_SIZE).also { bytes ->
                fun ascii(offset: Int, value: String) {
                    value.toByteArray(Charsets.US_ASCII).copyInto(bytes, offset)
                }
                fun littleEndian(offset: Int, value: Int, count: Int) {
                    repeat(count) { index ->
                        bytes[offset + index] = (value shr (index * 8)).toByte()
                    }
                }
                ascii(0, "RIFF")
                littleEndian(4, dataSize + 36, 4)
                ascii(8, "WAVE")
                ascii(12, "fmt ")
                littleEndian(16, 16, 4)
                littleEndian(20, 1, 2)
                littleEndian(22, 1, 2)
                littleEndian(24, sampleRate, 4)
                littleEndian(28, byteRate, 4)
                littleEndian(32, 2, 2)
                littleEndian(34, 16, 2)
                ascii(36, "data")
                littleEndian(40, dataSize, 4)
            }
        }
    }
}
