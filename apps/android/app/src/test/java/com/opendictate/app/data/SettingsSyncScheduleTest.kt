package com.opendictate.app.data

import org.junit.Assert.*
import org.junit.Test

class SettingsSyncScheduleTest {
    @Test fun `typing is batched and a minute pull cannot bypass the quiet period`() {
        val queue = SettingsSyncSchedule()
        queue.changed(0)
        queue.changed(2_000)
        queue.refresh()
        assertFalse(queue.begin(3_000))
        assertEquals(2_000L, queue.delayUntilReady(3_000))
        assertTrue(queue.begin(5_000))
        queue.finish()
        assertFalse(queue.begin(6_000))
    }

    @Test fun `changes made during a request remain queued and wait until typing stops`() {
        val queue = SettingsSyncSchedule()
        queue.refresh()
        assertTrue(queue.begin(0))
        queue.changed(100)
        assertTrue(queue.hasPendingChange)
        assertFalse(queue.begin(4_000))
        queue.finish()
        assertFalse(queue.begin(2_000))
        assertTrue(queue.begin(3_100))
        assertFalse(queue.hasPendingChange)
        queue.changed(3_200) // Also covers an edit while an upload is in flight.
        queue.finish()
        assertTrue(queue.begin(6_200))
    }

    @Test fun `startup reopen and polling requests coalesce while busy and errors retry on next request`() {
        val queue = SettingsSyncSchedule()
        queue.refresh()
        assertTrue(queue.begin(0))
        repeat(3) { queue.refresh() }
        assertFalse(queue.begin(10))
        queue.finish()
        assertTrue(queue.begin(20))
        queue.finish() // A failed operation must not spin in an immediate retry loop.
        assertFalse(queue.begin(30))
        queue.refresh()
        assertTrue(queue.begin(60_000))
    }
}
