package com.opendictate.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    LaunchedEffect(state.failed, state.needsAuthorization) {
        if (state.failed) {
            Toast.makeText(context, if (state.needsAuthorization) R.string.drive_sync_sign_in_again
                else R.string.drive_sync_failed, Toast.LENGTH_LONG).show()
        }
    }
    val checked = state.enabled && !state.needsAuthorization
    val authorizationStatus = stringResource(R.string.drive_sync_authorizing)
    Row(
        Modifier.fillMaxWidth()
            .toggleable(value = checked, enabled = !authorizing, role = Role.Switch, onValueChange = { enabled ->
                if (!enabled) sync.disconnect()
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
            })
            .semantics { if (authorizing) stateDescription = authorizationStatus }
            .heightIn(min = 80.dp)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_google_drive_24), null, Modifier.size(24.dp), tint = Color.Unspecified)
        Spacer(Modifier.width(14.dp))
        Text(stringResource(R.string.drive_sync_provider), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = !authorizing)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
