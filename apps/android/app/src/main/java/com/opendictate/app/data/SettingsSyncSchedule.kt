package com.opendictate.app.data

/** One network operation at a time; edits made during it remain queued. Times are monotonic milliseconds. */
class SettingsSyncSchedule {
    private var changeDueAt: Long? = null
    private var refreshRequested = false
    var busy = false
        private set
    val hasPendingChange: Boolean get() = changeDueAt != null

    fun changed(now: Long) { changeDueAt = now + 3_000 }
    fun refresh() { refreshRequested = true }
    fun delayUntilReady(now: Long): Long? = changeDueAt?.let { maxOf(0, it - now) }

    fun begin(now: Long): Boolean {
        if (busy || (changeDueAt?.let { it > now } == true)) return false
        if (!refreshRequested && changeDueAt == null) return false
        changeDueAt = null
        refreshRequested = false
        busy = true
        return true
    }

    fun finish() { busy = false }
}
