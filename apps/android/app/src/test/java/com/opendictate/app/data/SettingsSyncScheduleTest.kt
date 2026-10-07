package com.opendictate.app.data

import org.junit.Assert.*
import org.junit.Test

class SettingsSyncScheduleTest {
    @Test fun testTypingWaitsFifteenSecondsAfterLastEditEvenWithAutomaticPull() {
        val queue = SettingsSyncSchedule()
        queue.changed(now = 0); queue.changed(now = 2_000)
        queue.refreshIfStale(now = 3_000)
        assertFalse(queue.begin(now = 16_999))
        assertEquals(14_000L, queue.delayUntilReady(now = 3_000))
        assertTrue(queue.begin(now = 17_000))
        queue.finish(now = 17_100, completion = SettingsSyncSchedule.Completion.SUCCESS)
        assertFalse(queue.begin(now = 18_000))
    }
    @Test fun testEditsDuringRequestRemainQueuedAndDoNotRunConcurrently() {
        val queue = SettingsSyncSchedule()
        queue.refresh(); assertTrue(queue.begin(now = 0))
        queue.changed(now = 100)
        assertTrue(queue.hasPendingChange)
        assertFalse(queue.begin(now = 20_000))
        queue.finish(now = 1_000, completion = SettingsSyncSchedule.Completion.DEFERRED)
        assertFalse(queue.begin(now = 15_099))
        assertTrue(queue.begin(now = 15_100))
        queue.changed(now = 16_000)
        queue.finish(now = 17_000, completion = SettingsSyncSchedule.Completion.SUCCESS)
        assertFalse(queue.begin(now = 30_999))
        assertTrue(queue.begin(now = 31_000))
    }
    @Test fun testBackgroundChecksWaitFifteenMinutesAfterSuccess() {
        val queue = SettingsSyncSchedule()
        queue.refreshIfStale(now = 0, maxAge = SettingsSyncSchedule.BACKGROUND_INTERVAL)
        assertTrue(queue.begin(now = 0))
        queue.finish(now = 100, completion = SettingsSyncSchedule.Completion.SUCCESS)
        assertEquals(900_000L, queue.delayUntilReady(now = 100, includePeriodic = true))
        queue.refreshIfStale(now = 900_099, maxAge = SettingsSyncSchedule.BACKGROUND_INTERVAL)
        assertFalse(queue.begin(now = 900_099))
        queue.refreshIfStale(now = 900_100, maxAge = SettingsSyncSchedule.BACKGROUND_INTERVAL)
        assertTrue(queue.begin(now = 900_100))
    }
    @Test fun testReopenAndWakeOnlyCheckStaleDataAndCoalesceWhileBusy() {
        val queue = SettingsSyncSchedule()
        queue.refresh(); assertTrue(queue.begin(now = 0))
        queue.refreshIfStale(now = 10)
        queue.finish(now = 100, completion = SettingsSyncSchedule.Completion.SUCCESS)
        queue.refreshIfStale(now = 300_099)
        assertFalse(queue.begin(now = 300_099))
        queue.refreshIfStale(now = 300_100)
        assertTrue(queue.begin(now = 300_100))
        queue.refreshIfStale(now = 400_000)
        queue.finish(now = 400_100, completion = SettingsSyncSchedule.Completion.SUCCESS)
        assertFalse(queue.begin(now = 400_100))
    }
    @Test fun testFailuresBackOffAndAutomaticChecksOrEditsCannotBypassRetry() {
        val queue = SettingsSyncSchedule()
        queue.refresh(); var time = 0L
        for (delay in longArrayOf(60_000, 300_000, 900_000, 1_800_000, 1_800_000)) {
            assertTrue(queue.begin(now = time))
            queue.finish(now = time, completion = SettingsSyncSchedule.Completion.RETRY)
            queue.changed(now = time)
            queue.refreshIfStale(now = time + 30_000)
            assertEquals(delay, queue.delayUntilReady(now = time))
            assertFalse(queue.begin(now = time + delay - 1))
            time += delay
        }
        assertTrue(queue.begin(now = time))
        queue.finish(now = time, completion = SettingsSyncSchedule.Completion.SUCCESS)
        queue.refresh(); assertTrue(queue.begin(now = time))
        queue.finish(now = time, completion = SettingsSyncSchedule.Completion.RETRY)
        assertEquals(60_000L, queue.delayUntilReady(now = time))
    }
    @Test fun testAuthorizationPausesAllAutomaticWorkUntilExplicitRequest() {
        val queue = SettingsSyncSchedule()
        queue.refresh(); assertTrue(queue.begin(now = 0))
        queue.finish(now = 0, completion = SettingsSyncSchedule.Completion.AUTHORIZATION)
        queue.changed(now = 100); queue.refreshIfStale(now = 900_000)
        queue.networkRestored(now = 900_000)
        assertFalse(queue.begin(now = 900_000))
        assertNull(queue.delayUntilReady(now = 900_000, includePeriodic = true))
        queue.refresh(); assertTrue(queue.begin(now = 900_000))
    }
    @Test fun testNetworkRecoverySkipsFreshDataButResumesEditsAndFailedWork() {
        val queue = SettingsSyncSchedule()
        queue.refresh(); assertTrue(queue.begin(now = 0))
        queue.finish(now = 0, completion = SettingsSyncSchedule.Completion.SUCCESS)
        queue.networkRestored(now = 100)
        assertFalse(queue.begin(now = 100))
        queue.changed(now = 200); queue.networkRestored(now = 300)
        assertFalse(queue.begin(now = 15_199))
        assertTrue(queue.begin(now = 15_200))
        queue.finish(now = 15_200, completion = SettingsSyncSchedule.Completion.RETRY)
        queue.networkRestored(now = 16_000)
        assertTrue(queue.begin(now = 16_000))
        queue.finish(now = 16_000, completion = SettingsSyncSchedule.Completion.SUCCESS)
        queue.networkRestored(now = 316_000)
        assertTrue(queue.begin(now = 316_000))
    }
    @Test fun testManualRetryBypassesBackoffButPreservesUnfinishedEdits() {
        val queue = SettingsSyncSchedule()
        queue.refresh(); assertTrue(queue.begin(now = 0))
        queue.finish(now = 0, completion = SettingsSyncSchedule.Completion.RETRY)
        queue.changed(now = 100); queue.refresh()
        assertFalse(queue.begin(now = 15_099))
        assertTrue(queue.begin(now = 15_100))
    }
}
