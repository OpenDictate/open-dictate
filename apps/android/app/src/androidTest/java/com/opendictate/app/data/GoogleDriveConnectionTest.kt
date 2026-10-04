package com.opendictate.app.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.opendictate.app.network.GoogleDriveSettingsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class GoogleDriveConnectionTest {
    @Test fun connectionWaitsBeforeAnyApplyOrUploadAndCloudChoiceRechecksLatestData() = runBlocking {
        withContext(Dispatchers.Main) {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val prefix = "connection-test-${UUID.randomUUID()}"
            val names = mutableSetOf<String>()
            val context = object : ContextWrapper(base) {
                override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                    names += "$prefix-$name"
                    return base.getSharedPreferences("$prefix-$name", mode)
                }
            }
            var cloud = "Cloud"
            var writes = 0
            val http = OkHttpClient.Builder().addInterceptor { chain ->
                val request = chain.request()
                val body = when {
                    request.method != "GET" -> { writes++; "{}" }
                    request.url.queryParameter("alt") == "media" -> SettingsSyncDocument(mapOf(
                        "dictionary" to SettingsSyncEntry(cloud, 1, "mac"))).toJson()
                    else -> """{"files":[{"id":"cloud-file","name":"opendictate-settings-v1-mac.json"}]}"""
                }
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body(body.toResponseBody("application/json".toMediaType())).build()
            }.build()
            val settings = SettingsStore(context)
            settings.prompt = "Offline edit"
            val replacements = ReplacementStore(context)
            val sync = GoogleDriveSync(context, replacements, GoogleDriveSettingsClient(http), { "synthetic" }, false)
            suspend fun waitFor(predicate: (DriveSyncState) -> Boolean) = withTimeout(5000) { sync.state.first(predicate) }
            try {
                val before = settings.syncDocument()
                sync.connect("synthetic")
                waitFor { it.needsSourceSelection }
                assertEquals("Offline edit", settings.prompt)
                assertEquals(before, settings.syncDocument())
                assertEquals(0, writes)
                assertTrue(settings.driveConnectionPending)
                sync.requestSync()
                assertEquals(0, writes)
                cloud = "Changed on another device"
                sync.chooseSource(SettingsSyncDocument.ConnectionSource.CLOUD)
                waitFor { it.needsSourceSelection }
                assertEquals(0, writes)
                assertEquals("Offline edit", settings.prompt)
                sync.chooseSource(SettingsSyncDocument.ConnectionSource.CLOUD)
                waitFor { it.lastSyncedAt > 0 }
                assertEquals(cloud, settings.prompt)
                assertEquals(1, writes)
                assertFalse(settings.driveConnectionPending)
                // A later reconnect must ask again; dismissing it preserves both sources.
                settings.prompt = "Another offline edit"
                sync.disconnect()
                sync.connect("synthetic")
                waitFor { it.needsSourceSelection }
                sync.disconnect()
                assertFalse(settings.driveSyncEnabled)
                assertFalse(settings.driveConnectionPending)
                assertFalse(sync.state.value.needsSourceSelection)
                assertEquals("Another offline edit", settings.prompt)
                assertEquals(1, writes)
            } finally {
                sync.disconnect()
                names.forEach { base.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
            }
        }
    }
}
