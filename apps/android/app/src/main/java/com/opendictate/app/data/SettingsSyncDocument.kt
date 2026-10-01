package com.opendictate.app.data

import org.json.JSONObject

/** Shared with macOS. Unknown keys survive merges; only allowlisted preferences are exported. */
data class SettingsSyncEntry(val value: String, val modifiedAt: Long, val deviceId: String)

data class SettingsSyncDocument(val entries: Map<String, SettingsSyncEntry> = emptyMap()) {
    fun merge(other: SettingsSyncDocument): SettingsSyncDocument {
        val result = entries.toMutableMap()
        other.entries.forEach { (key, incoming) ->
            val existing = result[key]
            if (existing == null || compareValuesBy(incoming, existing,
                    SettingsSyncEntry::modifiedAt, SettingsSyncEntry::deviceId, SettingsSyncEntry::value) > 0) {
                result[key] = incoming
            }
        }
        return SettingsSyncDocument(result)
    }

    fun record(values: Map<String, String>, deviceId: String, now: Long, seed: Boolean = false): SettingsSyncDocument {
        val result = entries.toMutableMap()
        val clock = maxOf(now, (entries.values.maxOfOrNull { it.modifiedAt } ?: 0L) + 1)
        KEYS.forEach { key ->
            val value = values[key] ?: return@forEach
            if (result[key]?.value != value) result[key] = SettingsSyncEntry(value, if (seed) 0 else clock, if (seed) "" else deviceId)
        }
        return SettingsSyncDocument(result)
    }

    fun promoteSeeds(deviceId: String, now: Long): SettingsSyncDocument {
        val clock = maxOf(now, (entries.values.maxOfOrNull { it.modifiedAt } ?: 0L) + 1)
        return SettingsSyncDocument(entries.mapValues { (key, entry) ->
            if (key in KEYS && entry.modifiedAt == 0L) entry.copy(modifiedAt = clock, deviceId = deviceId) else entry
        })
    }

    fun toJson(): String = JSONObject().put("schemaVersion", 1).put("entries", JSONObject().also { objectEntries ->
        entries.forEach { (key, entry) -> objectEntries.put(key, JSONObject()
            .put("value", entry.value).put("modifiedAt", entry.modifiedAt).put("deviceId", entry.deviceId)) }
    }).toString()

    companion object {
        val KEYS = setOf("dictionary", "mode", "liveModel", "accurateModel", "textModel", "wordReplacements", "wordReplacementsEnabled")
        const val MAXIMUM_BYTES = 1_048_576L
        fun fromJson(json: String): SettingsSyncDocument {
            require(json.toByteArray().size <= MAXIMUM_BYTES)
            val root = JSONObject(json)
            val version = root.get("schemaVersion")
            require(version is Number && version.toDouble() == 1.0)
            val entries = root.getJSONObject("entries")
            require(entries.length() <= 1024)
            return SettingsSyncDocument(entries.keys().asSequence().associateWith { key ->
                val entry = entries.getJSONObject(key)
                val value = entry.get("value")
                val timestamp = entry.get("modifiedAt")
                val device = entry.get("deviceId")
                require(value is String && value.toByteArray().size <= 262_144 && device is String &&
                    device.toByteArray().size <= 128 && key.toByteArray().size <= 128 && timestamp is Number)
                val time = timestamp.toLong()
                require(timestamp.toDouble() == time.toDouble() && time >= 0 && time < Long.MAX_VALUE - 1)
                SettingsSyncEntry(value, time, device)
            })
        }
    }
}
