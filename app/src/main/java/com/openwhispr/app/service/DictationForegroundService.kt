package com.openwhispr.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.openwhispr.app.MainActivity
import com.openwhispr.app.R
import com.openwhispr.app.audio.PcmAudioRecorder
import com.openwhispr.app.audio.WavFile
import com.openwhispr.app.data.SecureApiKeyStore
import com.openwhispr.app.data.SettingsStore
import com.openwhispr.app.model.TranscriptionModel
import com.openwhispr.app.network.OpenAiTranscriptionClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class DictationForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val apiClient = OpenAiTranscriptionClient()
    private var activeJob: Job? = null
    private var stopSignal = CompletableDeferred<Unit>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> requestStop()
            ACTION_START -> if (activeJob?.isActive != true) startDictation()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        requestStop()
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private fun startDictation() {
        val apiKey = SecureApiKeyStore(this).get()
        if (apiKey.isNullOrBlank()) {
            publishError("Добавьте OpenAI API key в приложении")
            stopSelf()
            return
        }
        val settings = SettingsStore(this)
        val sessionId = nextSession.incrementAndGet()
        stopSignal = CompletableDeferred()
        startForeground(NOTIFICATION_ID, notification(false))
        DictationStateBus.set(DictationState(sessionId, DictationPhase.CONNECTING))

        activeJob = scope.launch {
            val recorder = PcmAudioRecorder(this@DictationForegroundService)
            var tempFile: File? = null
            var wavFile: WavFile? = null
            try {
                val transcript = when (settings.model) {
                    TranscriptionModel.LIVE -> apiClient.transcribeLive(
                        apiKey = apiKey,
                        scope = scope,
                        recorder = recorder,
                        language = settings.language,
                        prompt = settings.prompt,
                        waitForStop = { stopSignal.await() },
                        onReady = {
                            DictationStateBus.set(
                                DictationState(sessionId, DictationPhase.LISTENING),
                            )
                        },
                        onPartial = { text ->
                            val phase = if (
                                DictationStateBus.state.value.phase == DictationPhase.PROCESSING
                            ) {
                                DictationPhase.PROCESSING
                            } else {
                                DictationPhase.LISTENING
                            }
                            DictationStateBus.set(
                                DictationState(sessionId, phase, text),
                            )
                        },
                    )
                    TranscriptionModel.ACCURATE -> {
                        tempFile = File.createTempFile("dictation-", ".wav", cacheDir)
                        val wav = WavFile(tempFile!!)
                        wavFile = wav
                        recorder.start(scope, wav::write)
                        DictationStateBus.set(
                            DictationState(sessionId, DictationPhase.LISTENING),
                        )
                        stopSignal.await()
                        recorder.stop()
                        wav.close()
                        DictationStateBus.set(
                            DictationState(sessionId, DictationPhase.PROCESSING),
                        )
                        updateNotification(true)
                        apiClient.transcribeFile(
                            apiKey,
                            tempFile!!,
                            settings.language,
                            settings.prompt,
                        )
                    }
                }
                if (transcript.isBlank()) throw IllegalStateException("Речь не распознана")
                DictationStateBus.set(
                    DictationState(sessionId, DictationPhase.COMPLETED, transcript),
                )
                delay(1_200)
                DictationStateBus.set(DictationState(sessionId, DictationPhase.IDLE))
            } catch (error: Throwable) {
                recorder.stop()
                if (error !is kotlinx.coroutines.CancellationException) {
                    publishError(error.message ?: "Не удалось распознать речь", sessionId)
                    delay(3_000)
                    DictationStateBus.set(DictationState(sessionId, DictationPhase.IDLE))
                }
            } finally {
                runCatching { wavFile?.close() }
                tempFile?.delete()
                ServiceCompat.stopForeground(this@DictationForegroundService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun requestStop() {
        stopSignal.complete(Unit)
        val state = DictationStateBus.state.value
        if (state.isActive) {
            DictationStateBus.set(state.copy(phase = DictationPhase.PROCESSING))
            updateNotification(true)
        }
    }

    private fun publishError(message: String, sessionId: Long = nextSession.get()) {
        DictationStateBus.set(
            DictationState(sessionId, DictationPhase.ERROR, message = message),
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.accessibility_description)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(processing: Boolean): Notification {
        val stopIntent = Intent(this, DictationForegroundService::class.java).setAction(ACTION_STOP)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(if (processing) R.string.notification_processing else R.string.notification_recording),
            )
            .setContentText(if (processing) "Текст скоро появится в поле" else "Нажмите ещё раз, чтобы закончить")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.stop), stopPendingIntent)
            .build()
    }

    private fun updateNotification(processing: Boolean) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            notification(processing),
        )
    }

    companion object {
        const val ACTION_START = "com.openwhispr.app.action.START_DICTATION"
        const val ACTION_STOP = "com.openwhispr.app.action.STOP_DICTATION"
        private const val CHANNEL_ID = "dictation"
        private const val NOTIFICATION_ID = 41
        private val nextSession = AtomicLong(System.currentTimeMillis())
    }
}
