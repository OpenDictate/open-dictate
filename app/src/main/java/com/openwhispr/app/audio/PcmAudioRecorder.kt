package com.openwhispr.app.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class PcmAudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val running = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, onChunk: (ByteArray) -> Unit) {
        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        ) { "Microphone permission is not granted" }
        check(running.compareAndSet(false, true)) { "Recorder is already running" }

        val minimum = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minimum * 2, CHUNK_BYTES * 4),
        )
        check(record.state == AudioRecord.STATE_INITIALIZED) { "Microphone could not be initialized" }
        audioRecord = record
        record.startRecording()
        recordingJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(CHUNK_BYTES)
            while (isActive && running.get()) {
                val count = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (count > 0) onChunk(buffer.copyOf(count))
            }
        }
    }

    suspend fun stop() {
        running.set(false)
        runCatching { audioRecord?.stop() }
        recordingJob?.join()
        withContext(Dispatchers.IO) { runCatching { audioRecord?.release() } }
        recordingJob = null
        audioRecord = null
    }

    companion object {
        const val SAMPLE_RATE = 24_000
        private const val CHUNK_MILLIS = 40
        private const val CHUNK_BYTES = SAMPLE_RATE * 2 * CHUNK_MILLIS / 1_000
    }
}

