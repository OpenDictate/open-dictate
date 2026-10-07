package com.opendictate.app.data

/** Batches edits and throttles automatic checks using monotonic milliseconds. */
class SettingsSyncSchedule {
    enum class Completion { SUCCESS, RETRY, AUTHORIZATION, DEFERRED }
    private var changeDueAt: Long? = null
    private var refreshRequested = false
    private var lastSuccessAt: Long? = null
    private var retryDueAt: Long? = null
    private var failures = 0
    private var authorizationRequired = false
    var busy = false
        private set
    val hasPendingChange: Boolean get() = changeDueAt != null

    fun changed(now: Long) { changeDueAt = now + EDIT_DELAY }
    /** Explicit user requests may bypass retry backoff, but still batch unfinished edits. */
    fun refresh() { refreshRequested = true; retryDueAt = null; authorizationRequired = false }
    fun refreshIfStale(now: Long, maxAge: Long = FRESHNESS_INTERVAL) {
        if (busy || authorizationRequired) return
        if (lastSuccessAt?.let { now - it >= maxAge } != false) refreshRequested = true
    }
    fun networkRestored(now: Long) {
        if (busy || authorizationRequired) return
        if (hasPendingChange || retryDueAt != null || lastSuccessAt?.let { now - it >= FRESHNESS_INTERVAL } != false) {
            refreshRequested = true; retryDueAt = null
        }
    }
    fun delayUntilReady(now: Long, includePeriodic: Boolean = false): Long? {
        if (busy || authorizationRequired) return null
        if (refreshRequested || changeDueAt != null || retryDueAt != null) {
            return maxOf(0, maxOf(changeDueAt ?: now, retryDueAt ?: now) - now)
        }
        return if (includePeriodic) maxOf(0, (lastSuccessAt?.let { it + BACKGROUND_INTERVAL } ?: now) - now) else null
    }
    fun begin(now: Long): Boolean {
        if (busy || authorizationRequired || changeDueAt?.let { it > now } == true || retryDueAt?.let { it > now } == true) return false
        if (!refreshRequested && changeDueAt == null && retryDueAt == null) return false
        changeDueAt = null; refreshRequested = false; retryDueAt = null; busy = true
        return true
    }
    fun finish(now: Long, completion: Completion) {
        busy = false
        when (completion) {
            Completion.SUCCESS -> { lastSuccessAt = now; failures = 0; retryDueAt = null }
            Completion.RETRY -> {
                val delays = longArrayOf(60_000, 300_000, 900_000, 1_800_000)
                retryDueAt = now + delays[minOf(failures, delays.lastIndex)]
                failures = minOf(failures + 1, delays.size)
            }
            Completion.AUTHORIZATION -> { authorizationRequired = true; retryDueAt = null }
            Completion.DEFERRED -> Unit
        }
    }
    companion object {
        const val EDIT_DELAY = 15_000L
        const val BACKGROUND_INTERVAL = 900_000L
        const val FRESHNESS_INTERVAL = 300_000L
    }
}
