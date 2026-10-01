package com.opendictate.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.Identity
import com.opendictate.app.OpenDictateApplication
import com.opendictate.app.R
import com.opendictate.app.data.GoogleDriveSync

@Composable
internal fun GoogleDriveSyncSettings() {
    val context = LocalContext.current
    val sync = (context.applicationContext as OpenDictateApplication).driveSync
    val state by sync.state.collectAsState()
    var authorizing by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        authorizing = false
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val token = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data).accessToken
                if (token != null) sync.connect(token) else sync.authorizationFailed()
            } catch (_: Exception) { sync.authorizationFailed() }
        }
    }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.drive_sync_description), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(when {
            authorizing -> R.string.drive_sync_authorizing
            state.busy -> R.string.drive_sync_busy
            state.needsAuthorization -> R.string.drive_sync_sign_in_again
            state.failed -> R.string.drive_sync_failed
            !state.enabled -> R.string.drive_sync_off
            state.lastSyncedAt > 0 -> R.string.drive_sync_done
            else -> R.string.drive_sync_waiting
        }), style = MaterialTheme.typography.bodySmall,
            color = if (state.failed) MaterialTheme.colorScheme.error else SettingsMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !state.busy && !authorizing, onClick = {
                if (state.enabled && !state.needsAuthorization) sync.requestSync()
                else {
                    val activity = context.findActivity()
                    if (activity == null) sync.authorizationFailed()
                    else {
                        authorizing = true
                        Identity.getAuthorizationClient(activity).authorize(GoogleDriveSync.request())
                            .addOnSuccessListener { result ->
                                if (result.hasResolution()) {
                                    val intent = result.pendingIntent
                                    if (intent != null) launcher.launch(IntentSenderRequest.Builder(intent).build())
                                    else { authorizing = false; sync.authorizationFailed() }
                                } else {
                                    authorizing = false
                                    result.accessToken?.let(sync::connect) ?: sync.authorizationFailed()
                                }
                            }.addOnFailureListener { authorizing = false; sync.authorizationFailed() }
                    }
                }
            }) { Text(stringResource(if (state.enabled && !state.needsAuthorization) R.string.drive_sync_now else R.string.drive_sync_connect)) }
            if (state.enabled) TextButton(onClick = sync::disconnect) { Text(stringResource(R.string.drive_sync_disconnect)) }
        }
        Text(stringResource(R.string.drive_sync_privacy), style = MaterialTheme.typography.bodySmall, color = SettingsMuted)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
