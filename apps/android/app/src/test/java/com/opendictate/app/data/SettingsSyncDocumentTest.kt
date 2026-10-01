package com.opendictate.app.data

import org.junit.Assert.*
import org.junit.Test

class SettingsSyncDocumentTest {
    private fun document(key: String, value: String, time: Long, device: String) =
        SettingsSyncDocument(mapOf(key to SettingsSyncEntry(value, time, device)))

    @Test fun `separate preferences merge and concurrent conflicts converge regardless of order`() {
        val android = document("dictionary", "Android\nНикита", 100, "android")
            .merge(document("textModel", "gpt-6-sol", 120, "android"))
        val mac = document("dictionary", "Mac", 100, "mac")
            .merge(document("mode", "live", 130, "mac"))
        val merged = android.merge(mac)
        assertEquals(merged, mac.merge(android))
        assertEquals(merged, merged.merge(android).merge(mac))
        assertEquals("Mac", merged.entries["dictionary"]?.value)
        assertEquals("gpt-6-sol", merged.entries["textModel"]?.value)
        assertEquals("live", merged.entries["mode"]?.value)
    }

    @Test fun `clearing dictionary survives stale offline replicas`() {
        val old = document("dictionary", "OpenDictate", 1, "mac")
        val cleared = old.record(mapOf("dictionary" to ""), "android", 2)
        assertEquals("", cleared.merge(old).entries["dictionary"]?.value)
    }

    @Test fun `new device defaults yield to cloud and local changes use a monotonic clock`() {
        val seed = SettingsSyncDocument().record(mapOf("mode" to "accurate"), "android", 0, seed = true)
        val cloud = document("mode", "live", 1000, "mac")
        val merged = seed.merge(cloud)
        assertEquals("live", merged.entries["mode"]?.value)
        val changed = merged.record(mapOf("mode" to "accurate"), "android", 10)
        assertEquals(1001L, changed.entries["mode"]?.modifiedAt)
        assertEquals(changed, changed.record(mapOf("mode" to "accurate"), "android", 2000))
        assertTrue(seed.promoteSeeds("android", 100).entries["mode"]!!.modifiedAt > 0)
    }

    @Test fun `wire format round trips unicode preserves future sections and exports only allowlisted values`() {
        val fixture = """{"schemaVersion":1,"entries":{"dictionary":{"value":"Никита\nOpenDictate 📝","modifiedAt":1760000000000,"deviceId":"mac"},"future.color":{"value":"dark","modifiedAt":2,"deviceId":"mac"}}}"""
        val parsed = SettingsSyncDocument.fromJson(fixture)
        assertEquals(parsed, SettingsSyncDocument.fromJson(parsed.toJson()))
        val updated = parsed.record(mapOf("textModel" to "gpt-6-sol", "apiKey" to "private-test-key", "history" to "private-test-history"), "android", 1)
        assertEquals("dark", updated.entries["future.color"]?.value)
        assertFalse(updated.toJson().contains("private-test"))
    }

    @Test fun `future versions malformed entries and oversized data fail closed`() {
        listOf("{\"schemaVersion\":2,\"entries\":{}}", "{\"schemaVersion\":1.5,\"entries\":{}}", "{\"schemaVersion\":\"1\",\"entries\":{}}", "{\"schemaVersion\":1,\"entries\":{\"mode\":{\"value\":false,\"modifiedAt\":1,\"deviceId\":\"a\"}}}",
            "{\"schemaVersion\":1,\"entries\":{\"mode\":{\"value\":\"live\",\"modifiedAt\":-1,\"deviceId\":\"a\"}}}", "x".repeat(1_048_577)).forEach { value ->
            assertThrows(Exception::class.java) { SettingsSyncDocument.fromJson(value) }
        }
    }
}
