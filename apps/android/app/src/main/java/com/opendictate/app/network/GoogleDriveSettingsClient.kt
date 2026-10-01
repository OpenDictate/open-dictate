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
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    data class Remote(val document: SettingsSyncDocument, val ownFileId: String?)

    suspend fun download(token: String, deviceId: String): Remote = withContext(Dispatchers.IO) {
        var document = SettingsSyncDocument()
        var ownId: String? = null
        var page: String? = null
        var count = 0
        do {
            val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("spaces", "appDataFolder")
                .addQueryParameter("q", "trashed = false and name contains 'opendictate-settings-v1-'")
                .addQueryParameter("fields", "nextPageToken,files(id,name,size)")
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
                document = document.merge(SettingsSyncDocument.fromJson(send(Request.Builder().url(urlData).build(), token)))
                if (name == fileName(deviceId) && (ownId == null || id < ownId)) ownId = id
            }
            page = listing.optString("nextPageToken").takeIf { it.isNotEmpty() }
        } while (page != null)
        Remote(document, ownId)
    }

    suspend fun upload(token: String, deviceId: String, fileId: String?, document: SettingsSyncDocument) = withContext(Dispatchers.IO) {
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
