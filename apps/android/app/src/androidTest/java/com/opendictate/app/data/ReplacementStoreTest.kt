package com.opendictate.app.data

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ReplacementStoreTest {
    @Test fun editsPersistAndSessionsKeepTheirRules() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "replacement-test-${UUID.randomUUID()}"
        try {
            val store = ReplacementStore(context, name)
            assertTrue(store.save(null, " cat ", " dog "))
            val rule = store.state.value.document.rules.single()
            val active = store.engine()
            assertFalse(store.save(null, "CAT", "duplicate"))
            assertTrue(store.save(rule.id, "cat", "bird"))
            assertEquals("dog", active.apply("cat"))
            assertEquals("bird", ReplacementStore(context, name).engine().apply("cat"))
            store.setEnabled(false)
            assertEquals("cat", ReplacementStore(context, name).engine().apply("cat"))
            store.setEnabled(true)
            store.toggle(store.state.value.document.rules.single(), false)
            assertEquals("cat", ReplacementStore(context, name).engine().apply("cat"))
            store.delete(rule)
            assertTrue(ReplacementStore(context, name).state.value.document.rules.isEmpty())
        } finally { context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit() }
    }
    @Test fun corruptDataIsKeptAndCannotBeOverwritten() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "replacement-test-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            prefs.edit().putString("document", "not-json").commit()
            val store = ReplacementStore(context, name)
            assertTrue(store.state.value.storageError)
            assertFalse(store.save(null, "cat", "dog"))
            assertEquals("not-json", prefs.getString("document", null))
        } finally { prefs.edit().clear().commit() }
    }
}
