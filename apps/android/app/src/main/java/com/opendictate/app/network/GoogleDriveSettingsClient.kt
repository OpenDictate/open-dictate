package com.opendictate.app.network

import com.opendictate.app.data.SettingsSyncDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class DriveSyncException(val status: Int = 0) : IOException("Google Drive synchronization failed")

class GoogleDriveSettingsClient(
    private val client: OkHttpClient = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(45, TimeUnit.SECONDS).build(),
) {
    private data class CachedReplica(val version: String, val document: SettingsSyncDocument)
    private var replicas = emptyMap<String, CachedReplica>()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    data class Remote(val document: SettingsSyncDocument, val ownFileId: String?) {
        fun needsUpload(merged: SettingsSyncDocument): Boolean = ownFileId == null || merged != document
    }

    suspend fun download(token: String, deviceId: String, forceRefresh: Boolean = false): Remote = withContext(Dispatchers.IO) {
        if (forceRefresh) replicas = emptyMap()
        val nextReplicas = mutableMapOf<String, CachedReplica>()
        var document = SettingsSyncDocument()
        var ownId: String? = null
        var page: String? = null
        var count = 0
        do {
            val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("spaces", "appDataFolder")
                .addQueryParameter("q", "trashed = false and name contains 'opendictate-settings-v1-'")
                .addQueryParameter("fields", "nextPageToken,files(id,name,size,version)")
                .addQueryParameter("pageSize", "100")
            page?.let { url.addQueryParameter("pageToken", it) }
            val listing = JSONObject(send(Request.Builder().url(url.build()).build(), token))
            val files = listing.getJSONArray("files")
            for (index in 0 until files.length()) {
                val file = files.getJSONObject(index)
                val name = file.getString("name")
                if (!name.matches(Regex("opendictate-settings-v1-[A-Za-z0-9-]+\\.json"))) continue
                if (++count > 100 || file.optString("size", "0").toLong() > SettingsSyncDocument.MAXIMUM_BYTES) throw DriveSyncException()
                val id = file.getString("id")
                val urlData = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                    .addPathSegment(id).addQueryParameter("alt", "media").build()
                val version = file.optString("version").takeIf { it.isNotEmpty() }
                val cached = replicas[id]?.takeIf { version != null && it.version == version }
                val replica = cached?.document ?: SettingsSyncDocument.fromJson(send(Request.Builder().url(urlData).build(), token))
                document = document.merge(replica)
                if (version != null) nextReplicas[id] = CachedReplica(version, replica)
                if (name == fileName(deviceId) && (ownId == null || id < ownId)) ownId = id
            }
            page = listing.optString("nextPageToken").takeIf { it.isNotEmpty() }
        } while (page != null)
        replicas = nextReplicas
        Remote(document, ownId)
    }

    suspend fun upload(token: String, deviceId: String, fileId: String?, document: SettingsSyncDocument) = withContext(Dispatchers.IO) {
        if (fileId != null) replicas = replicas - fileId
        val json = document.toJson()
        require(json.toByteArray().size <= SettingsSyncDocument.MAXIMUM_BYTES)
        val request = if (fileId == null) {
            val metadata = JSONObject().put("name", fileName(deviceId)).put("parents", org.json.JSONArray(listOf("appDataFolder")))
            val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
                .addPart(metadata.toString().toRequestBody(jsonType)).addPart(json.toRequestBody(jsonType)).build()
            Request.Builder().url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart").post(body)
        } else {
            val url = "https://www.googleapis.com/upload/drive/v3/files".toHttpUrl().newBuilder()
                .addPathSegment(fileId).addQueryParameter("uploadType", "media").build()
            Request.Builder().url(url).patch(json.toRequestBody(jsonType))
        }
        send(request.build(), token)
    }

    private fun send(request: Request, token: String): String = client.newCall(request.newBuilder()
        .header("Authorization", "Bearer $token").build()).execute().use { response ->
        if (!response.isSuccessful) throw DriveSyncException(response.code)
        val body = response.body
        val buffer = okio.Buffer()
        while (body.source().read(buffer, 8192) != -1L) {
            if (buffer.size > SettingsSyncDocument.MAXIMUM_BYTES) throw DriveSyncException()
        }
        buffer.readUtf8()
    }

    private fun fileName(deviceId: String) = "opendictate-settings-v1-$deviceId.json"
}
