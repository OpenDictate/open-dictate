package com.opendictate.app.data

import android.content.Context
import android.os.SystemClock
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.opendictate.app.network.DriveSyncException
import com.opendictate.app.network.GoogleDriveSettingsClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DriveSyncState(val enabled: Boolean = false, val busy: Boolean = false,
    val lastSyncedAt: Long = 0, val needsAuthorization: Boolean = false, val failed: Boolean = false,
    val settingsRevision: Long = 0)

/** Process-owned: includes dictionary edits from the share and selection activities. */
class GoogleDriveSync(context: Context, private val replacements: ReplacementStore) {
    private val settings = SettingsStore(context)
    private val authorization = Identity.getAuthorizationClient(context)
    private val client = GoogleDriveSettingsClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private var generation = 0
    private var schedule = SettingsSyncSchedule()
    private val mutableState = MutableStateFlow(DriveSyncState(enabled = settings.driveSyncEnabled))
    val state = mutableState.asStateFlow()
    private var changeJob: kotlinx.coroutines.Job? = null
    private val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "sync_local_revision" && settings.driveSyncEnabled) {
            schedule.changed(SystemClock.elapsedRealtime())
            pump()
        }
    }

    init {
        // Seed before the first local edit, including migration from the original five fields.
        runCatching { settings.syncDocument() }
        context.getSharedPreferences("opendictate_settings", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(listener)
        scope.launch { while (true) { requestSync(); delay(60_000) } }
    }

    fun connect(token: String) {
        settings.driveSyncEnabled = true
        mutableState.value = DriveSyncState(enabled = true)
        schedule.refresh()
        pump(token)
    }

    fun disconnect() {
        generation++
        settings.driveSyncEnabled = false
        changeJob?.cancel()
        schedule = SettingsSyncSchedule()
        mutableState.value = DriveSyncState()
    }

    fun authorizationFailed() {
        mutableState.value = mutableState.value.copy(busy = false, failed = true, needsAuthorization = true)
    }

    fun requestSync() {
        if (!settings.driveSyncEnabled) return
        schedule.refresh()
        pump()
    }

    fun replacementsChanged() { settings.recordReplacementChange() }

    private fun pump(suppliedToken: String? = null) {
        if (!settings.driveSyncEnabled || mutex.isLocked || schedule.busy) return
        changeJob?.cancel()
        val now = SystemClock.elapsedRealtime()
        if (!schedule.begin(now)) {
            schedule.delayUntilReady(now)?.let { wait ->
                changeJob = scope.launch { delay(wait); pump() }
            }
            return
        }
        val run = generation
        scope.launch {
            try { performSync(suppliedToken) }
            finally {
                if (generation == run) schedule.finish()
                pump()
            }
        }
    }

    private suspend fun performSync(suppliedToken: String?) = mutex.withLock {
        if (!settings.driveSyncEnabled) return@withLock
        val runGeneration = generation
        mutableState.value = mutableState.value.copy(busy = true, failed = false)
        var token: String? = suppliedToken
        try {
            if (token == null) token = suspendCancellableCoroutine { continuation ->
                authorization.authorize(request()).addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        if (result.hasResolution() || result.accessToken == null) continuation.resumeWithException(DriveSyncException(401))
                        else continuation.resume(result.accessToken)
                    }
                }.addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(DriveSyncException(401)) }
            }
            val accessToken = token ?: throw DriveSyncException(401)
            val remote = client.download(accessToken, settings.syncDeviceId)
            if (runGeneration != generation) return@withLock
            // A pull must not publish an unfinished dictionary edit made during the download.
            if (schedule.hasPendingChange) {
                schedule.refresh()
                mutableState.value = mutableState.value.copy(busy = false)
                return@withLock
            }
            val merged = settings.mergeSyncDocument(remote.document, replacements)
            // Refresh open editors as soon as incoming preferences apply, before the upload suspends.
            mutableState.value = mutableState.value.copy(settingsRevision = mutableState.value.settingsRevision + 1)
            if (runGeneration != generation) return@withLock
            if (remote.needsUpload(merged)) {
                client.upload(accessToken, settings.syncDeviceId, remote.ownFileId, merged)
            }
            if (runGeneration == generation) mutableState.value = mutableState.value.copy(enabled = true, busy = false, failed = false,
                needsAuthorization = false, lastSyncedAt = System.currentTimeMillis())
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (error is DriveSyncException && error.status == 401) token?.let {
                authorization.clearToken(ClearTokenRequest.builder().setToken(it).build())
            }
            if (runGeneration == generation) mutableState.value = mutableState.value.copy(busy = false,
                failed = true, needsAuthorization = error is DriveSyncException && error.status == 401)
        }
    }

    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        fun request(): AuthorizationRequest = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE))).build()
    }
}
