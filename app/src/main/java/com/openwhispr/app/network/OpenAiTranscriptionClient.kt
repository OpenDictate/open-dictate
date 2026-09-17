package com.openwhispr.app.network

import android.util.Base64
import com.openwhispr.app.audio.PcmAudioRecorder
import com.openwhispr.app.model.LanguageHint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class OpenAiTranscriptionClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun transcribeFile(
        apiKey: String,
        audioFile: File,
        language: LanguageHint,
        prompt: String,
    ): String = withContext(Dispatchers.IO) {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", "gpt-transcribe")
            .addFormDataPart("file", "dictation.wav", audioFile.asRequestBody(WAV))
            .apply {
                if (prompt.isNotBlank()) addFormDataPart("prompt", prompt)
                language.code?.let { addFormDataPart("languages[]", it) }
            }
            .build()
        val request = authorizedRequest(apiKey, TRANSCRIPTIONS_URL)
            .post(multipart)
            .build()
        client.newCall(request).await().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) throw OpenAiException(errorMessage(response.code, body))
            JSONObject(body).optString("text").trim()
        }
    }

    suspend fun transcribeLive(
        apiKey: String,
        scope: CoroutineScope,
        recorder: PcmAudioRecorder,
        language: LanguageHint,
        prompt: String,
        waitForStop: suspend () -> Unit,
        onReady: () -> Unit,
        onPartial: (String) -> Unit,
    ): String {
        val session = LiveSession(apiKey, language, prompt, onPartial)
        try {
            session.connect()
            recorder.start(scope, session::sendAudio)
            withTimeout(CONNECT_TIMEOUT_MS) { session.ready.await() }
            onReady()
            waitForStop()
            recorder.stop()
            session.commit()
            return withTimeout(FINAL_TIMEOUT_MS) { session.completed.await() }.trim()
        } finally {
            recorder.stop()
            session.close()
        }
    }

    private inner class LiveSession(
        private val apiKey: String,
        private val language: LanguageHint,
        private val prompt: String,
        private val onPartial: (String) -> Unit,
    ) : WebSocketListener() {
        val ready = CompletableDeferred<Unit>()
        val completed = CompletableDeferred<String>()
        private val lock = Any()
        private val pendingAudio = ArrayDeque<ByteArray>()
        private val aggregate = StringBuilder()
        private val configured = AtomicBoolean(false)
        private var socket: WebSocket? = null

        fun connect() {
            val request = authorizedRequest(
                apiKey,
                "$REALTIME_URL?model=gpt-live-transcribe",
            ).build()
            socket = client.newWebSocket(request, this)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.send(sessionUpdate(language, prompt).toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val event = runCatching { JSONObject(text) }.getOrNull() ?: return
            when (event.optString("type")) {
                "session.updated", "transcription_session.updated" -> {
                    configured.set(true)
                    val queued = synchronized(lock) {
                        buildList {
                            while (pendingAudio.isNotEmpty()) add(pendingAudio.removeFirst())
                        }
                    }
                    queued.forEach(::sendNow)
                    ready.complete(Unit)
                }
                "conversation.item.input_audio_transcription.delta" -> {
                    val delta = event.optString("delta")
                    if (delta.isNotEmpty()) {
                        val current = synchronized(aggregate) {
                            aggregate.append(delta).toString()
                        }
                        onPartial(current)
                    }
                }
                "conversation.item.input_audio_transcription.completed" -> {
                    val transcript = event.optString("transcript").ifBlank {
                        synchronized(aggregate) { aggregate.toString() }
                    }
                    onPartial(transcript)
                    completed.complete(transcript)
                }
                "error" -> fail(OpenAiException(readRealtimeError(event)))
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            fail(OpenAiException(response?.let { "OpenAI вернул ${it.code}" } ?: t.userMessage(), t))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!completed.isCompleted) fail(OpenAiException("Соединение закрыто до получения текста"))
        }

        fun sendAudio(bytes: ByteArray) {
            if (configured.get()) {
                sendNow(bytes)
            } else {
                synchronized(lock) {
                    pendingAudio.addLast(bytes)
                    while (pendingAudio.size > MAX_QUEUED_CHUNKS) pendingAudio.removeFirst()
                }
            }
        }

        private fun sendNow(bytes: ByteArray) {
            val event = JSONObject()
                .put("type", "input_audio_buffer.append")
                .put("audio", Base64.encodeToString(bytes, Base64.NO_WRAP))
            if (socket?.send(event.toString()) != true) {
                fail(OpenAiException("Не удалось отправить звук"))
            }
        }

        fun commit() {
            check(socket?.send("{\"type\":\"input_audio_buffer.commit\"}") == true) {
                "Соединение уже закрыто"
            }
        }

        fun close() {
            socket?.close(1000, "dictation complete")
            socket = null
        }

        private fun fail(error: Throwable) {
            ready.completeExceptionally(error)
            completed.completeExceptionally(error)
        }
    }

    private fun sessionUpdate(language: LanguageHint, prompt: String): JSONObject {
        val transcription = JSONObject()
            .put("model", "gpt-live-transcribe")
            .put("delay", "minimal")
        if (prompt.isNotBlank()) transcription.put("prompt", prompt)
        language.code?.let { transcription.put("languages", JSONArray().put(it)) }
        val input = JSONObject()
            .put("format", JSONObject().put("type", "audio/pcm").put("rate", PcmAudioRecorder.SAMPLE_RATE))
            .put("transcription", transcription)
            .put("turn_detection", JSONObject.NULL)
        return JSONObject()
            .put("type", "session.update")
            .put(
                "session",
                JSONObject()
                    .put("type", "transcription")
                    .put("audio", JSONObject().put("input", input)),
            )
    }

    private fun authorizedRequest(apiKey: String, url: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("User-Agent", "OpenWhispr-Android/0.1.0")

    private fun readRealtimeError(event: JSONObject): String {
        val error = event.optJSONObject("error")
        return error?.optString("message")?.takeIf { it.isNotBlank() }
            ?: "OpenAI отклонил поток аудио"
    }

    private fun errorMessage(code: Int, body: String): String {
        val apiMessage = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return apiMessage?.takeIf { it.isNotBlank() } ?: "OpenAI вернул ошибку $code"
    }

    private suspend fun Call.await(): Response = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resumeWith(Result.success(response))
            }
        })
        continuation.invokeOnCancellation { cancel() }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is java.net.UnknownHostException -> "Нет подключения к интернету"
        is java.net.SocketTimeoutException -> "OpenAI не ответил вовремя"
        else -> message ?: "Ошибка сети"
    }

    companion object {
        private const val TRANSCRIPTIONS_URL = "https://api.openai.com/v1/audio/transcriptions"
        private const val REALTIME_URL = "wss://api.openai.com/v1/realtime"
        private const val CONNECT_TIMEOUT_MS = 12_000L
        private const val FINAL_TIMEOUT_MS = 25_000L
        private const val MAX_QUEUED_CHUNKS = 300
        private val WAV = "audio/wav".toMediaType()
    }
}

class OpenAiException(message: String, cause: Throwable? = null) : IOException(message, cause)
