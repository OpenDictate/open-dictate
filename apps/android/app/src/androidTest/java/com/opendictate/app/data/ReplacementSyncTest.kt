package com.opendictate.app.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ReplacementSyncTest {
    private fun scoped(block: (Context, SettingsStore, ReplacementStore) -> Unit) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "sync-test-${UUID.randomUUID()}"
        val names = mutableSetOf<String>()
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                names += "$prefix-$name"
                return base.getSharedPreferences("$prefix-$name", mode)
            }
        }
        try {
            val settings = SettingsStore(context)
            val replacements = ReplacementStore(context)
            replacements.onChange = { settings.recordReplacementChange() }
            settings.syncDocument()
            block(context, settings, replacements)
        } finally { names.forEach { base.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() } }
    }

    private fun remote(): SettingsSyncDocument {
        val rule = WordReplacement(id = "00000000-0000-0000-0000-000000000001", source = "cat", replacement = "dog")
        val json = """{"rules":[{"replacement":"dog","source":"cat","enabled":true,"id":"${rule.id}"}],"schemaVersion":1}"""
        return SettingsSyncDocument(mapOf("dictionary" to SettingsSyncEntry("Cloud", 42, "mac"),
            "wordReplacements" to SettingsSyncEntry(json, 42, "mac"),
            "wordReplacementsEnabled" to SettingsSyncEntry("true", 42, "mac")))
    }

    @Test fun importRefreshesExistingStoreAndKeepsSessionSnapshotAndIncomingTimestamp() = scoped { _, settings, store ->
        val active = store.engine()
        val remote = remote()
        settings.mergeSyncDocument(remote, store)
        assertEquals("cat", active.apply("cat"))
        assertEquals("dog", store.engine().apply("cat"))
        assertEquals("00000000-0000-0000-0000-000000000001", store.state.value.document.rules.single().id)
        settings.prompt = "Unrelated edit"
        assertEquals(remote.entries["wordReplacements"], settings.syncDocument().entries["wordReplacements"])
    }

    @Test fun localEditDeleteAndMasterSwitchDefeatOlderRemoteWithoutChangingActiveRules() = scoped { _, settings, store ->
        val remote = remote()
        settings.mergeSyncDocument(remote, store)
        val active = store.engine()
        val rule = store.state.value.document.rules.single()
        assertTrue(store.save(rule.id, "cat", "bird"))
        settings.mergeSyncDocument(remote, store)
        assertEquals("bird", store.engine().apply("cat"))
        assertEquals("dog", active.apply("cat"))
        store.delete(rule); store.setEnabled(false)
        settings.mergeSyncDocument(remote, store)
        assertTrue(store.state.value.document.rules.isEmpty())
        assertFalse(store.state.value.enabled)
        assertEquals("false", settings.syncDocument().entries["wordReplacementsEnabled"]?.value)
    }

    @Test fun invalidReplacementDocumentOrSwitchDoesNotPartiallyApply() = scoped { _, settings, store ->
        val initial = settings.prompt
        listOf("wordReplacements" to """{"schemaVersion":2,"rules":[]}""",
            "wordReplacementsEnabled" to "invalid").forEach { (key, value) ->
            val remote = remote().copy(entries = remote().entries + (key to SettingsSyncEntry(value, 42, "mac")))
            val before = settings.syncDocument()
            assertThrows(Exception::class.java) { settings.mergeSyncDocument(remote, store) }
            assertEquals(initial, settings.prompt)
            assertTrue(store.state.value.document.rules.isEmpty())
            assertEquals(before, settings.syncDocument())
        }
    }

    @Test fun originalJournalSeedsNewFieldsWithoutReplacingExistingClocks() = scoped { context, settings, _ ->
        val old = SettingsSyncDocument(mapOf("dictionary" to SettingsSyncEntry(settings.prompt, 70, "android"),
            "future.preference" to SettingsSyncEntry("preserved", 80, "android")))
        context.getSharedPreferences("opendictate_settings", Context.MODE_PRIVATE).edit()
            .putString("sync_document", old.toJson()).commit()
        val migrated = settings.syncDocument()
        assertEquals(old.entries["dictionary"], migrated.entries["dictionary"])
        assertEquals(old.entries["future.preference"], migrated.entries["future.preference"])
        assertEquals(0L, migrated.entries["wordReplacements"]?.modifiedAt)
        assertEquals(0L, migrated.entries["wordReplacementsEnabled"]?.modifiedAt)
    }

    @Test fun oversizedLocalEditStopsSyncAndCanBeCorrectedWithoutLosingTheNextEdit() = scoped { _, settings, _ ->
        val before = settings.syncDocument()
        settings.prompt = "x".repeat(262_145)
        assertThrows(Exception::class.java) { settings.syncDocument() }
        settings.prompt = "Recovered"
        val recovered = settings.syncDocument()
        assertEquals("Recovered", recovered.entries["dictionary"]?.value)
        assertTrue(recovered.entries.getValue("dictionary").modifiedAt > before.entries.getValue("dictionary").modifiedAt)
    }
}
