# Google Drive settings synchronization

Connect Google Drive in Settings on Android and macOS, using the same Google
account. Sync transfers the dictionary, word replacement rules and enabled
states, Live/Accurate mode and selected Live, Accurate and text model IDs. API keys, audio, transcripts/history, app exclusions
and permissions are excluded. Model access still depends on each device's
OpenAI API key and local model catalogue.

Changes sync after a short delay, at startup and every minute while the app
process runs. **Sync now** retries immediately. There is no background daemon or
Android scheduled worker. Errors preserve local settings for retry.

## Google Cloud setup

The OpenDictate sync project is `opendictate-sync-20261001` (project number
`951388859601`). Google Drive API and the `drive.appdata` consent scope are
configured there. The consent screen currently uses External / Testing and the
owner's Google account is registered as a test user. The following OAuth clients
were created on 2026-10-01; each device still needs user sign-in and consent:

| Client | OAuth client ID |
| --- | --- |
| Android release | `951388859601-vc6mquegouncri0q502a77qpuimkag7m.apps.googleusercontent.com` |
| Android debug | `951388859601-7qpmqaqh1bqrtrc68g7mdus5mc065t8p.apps.googleusercontent.com` |
| Android preview | `951388859601-dkeaepvgt48ovur5etg7cclpinik3f9c.apps.googleusercontent.com` |
| macOS Desktop | `951388859601-rl3ituvc93tpg2715kbumrnbci6d4ueh.apps.googleusercontent.com` |

Desktop credentials are kept outside Git. Do not add the downloaded client secret
or user tokens to this document.

Android registration fingerprints prepared on 2026-10-01:

| Build | Package | Certificate SHA-1 |
| --- | --- | --- |
| Local debug | `com.opendictate.app.debug` | `8E:F4:A9:1A:C9:28:D1:BA:03:68:6A:74:79:1B:0C:DC:1B:B3:69:CB` |
| Signed release | `com.opendictate.app` | `40:4E:DE:59:44:8D:90:86:75:7B:60:87:7D:07:8E:1E:0E:6B:06:6D` |
| Signed preview | `com.opendictate.app.preview` | Same release certificate when using the repository's release signing configuration |

The release fingerprint was read from the public signed `v0.11.0-rc.5` APK.
Other developers' debug certificates and a future Play App Signing certificate
need their own Android OAuth registrations in this same project.

Before sign-in can work, configure one Google Cloud project:

1. Enable the **Google Drive API** and configure the OAuth consent screen with
   `https://www.googleapis.com/auth/drive.appdata`. In Testing mode, add the
   Google accounts that will test synchronization.
2. Create an **Android** OAuth client for every package/signing certificate used:
   `com.opendictate.app`, `com.opendictate.app.debug` and/or
   `com.opendictate.app.preview`. Register the actual certificate SHA-1. Use
   `./gradlew signingReport` for local fingerprints; register release/Play signing
   certificates separately. Google Identity Services identifies the app from its
   package and signature, so no Android client ID is embedded. Google Play
   services must be available on the device.
3. Create a **Desktop app** OAuth client in the **same project**. On macOS enter
   its client ID and secret in Settings → Google Drive. The ID stays in local
   preferences; the secret and refresh token use device-only, non-synchronizing
   Keychain storage. Do not commit credentials. Google's Desktop client secret
   is not a confidential server secret; PKCE protects the code exchange too.
4. Connect Google Drive on both platforms and accept the app-data permission.
   macOS opens the system browser and validates PKCE S256 and OAuth state through
   a temporary callback listener bound only to `127.0.0.1`. Android uses native
   Google Identity Services AuthorizationClient consent resolution.

The macOS browser sign-in waits up to ten minutes. If it expires, use **Connect
Google Drive** to start a new attempt; reloading an expired local callback cannot
complete sign-in. Cancellation and expiry close the temporary listener without
saving credentials.

The OAuth clients must belong to the same Cloud project to share the application
data space. No permission to ordinary Drive files is requested. There is no backend.

## Storage and conflicts

The hidden `appDataFolder` contains one `opendictate-settings-v1-<device>.json`
replica per installation. Each installation reads all replicas with pagination,
merges them and updates only its own file. Concurrent uploads cannot overwrite
another device's file. Empty dictionaries are explicit values, so older offline
replicas cannot resurrect a cleared dictionary.

Each preference merges independently. The latest `modifiedAt` wins; `deviceId`
and then `value` break ties deterministically. Clocks advance past the largest
observed timestamp even if the device clock moves backwards. New-device defaults
have timestamp zero and yield to existing cloud values on first connection.
**The dictionary is one preference:** simultaneous dictionary edits do not combine
words; the latest dictionary replaces the earlier one. The replacement rule list
is also one preference: simultaneous edits choose the latest complete list,
including each rule’s ID and enabled state. Clearing exports an explicit empty
list, so stale replicas cannot restore deleted rules. The master replacement
switch merges independently from the list. Offline changes use device clocks,
so keep system clocks reasonably accurate.

Disconnect stops future sync and removes macOS OAuth credentials. Local settings
and cloud files remain. Android leaves token caching to Google Play services.
An upload already accepted by Google may finish during disconnection. Disconnect
does not revoke Google's grant: use Google Account → third-party connections.
Delete replicas through Google Drive → Settings → Manage apps → OpenDictate →
Delete hidden app data.

## Extending the format

Version 1 has an `entries` map whose keys currently are `dictionary`, `mode`,
`liveModel`, `accurateModel`, `textModel`, `wordReplacements` and
`wordReplacementsEnabled`:

```json
{
  "schemaVersion": 1,
  "entries": {
    "dictionary": {
      "value": "OpenDictate\nExample",
      "modifiedAt": 1760000000000,
      "deviceId": "installation-uuid"
    }
  }
}
```

Add namespaced keys to the explicit export allowlists and storage adapters on both
platforms for new settings. Unknown entries survive merges and round trips.
Unsupported schema versions fail closed rather than being overwritten. Values
are strings; structured future sections can encode JSON inside their value.
The `wordReplacements` value contains the shared version-1 replacement JSON
(`schemaVersion` and `rules` with `id`, `source`, `replacement`, `enabled`).
`wordReplacementsEnabled` is the string `true` or `false`. Existing five-field
journals seed these additions at timestamp zero so new defaults yield to cloud
values. Imported JSON is preserved verbatim to avoid platform-specific encoding
creating a new edit. Remote rules are validated before any settings are applied.

Clearing needs an explicit value, because removing a key leaves older replicas intact.

Limits: 1 MiB per file, 100 replicas, 1,024 entries and 256 KiB per value.
Malformed or oversized data stops synchronization without uploading a partial
merge. Credentials, provider payloads, dictionary contents and replacement rules are
never logged.
Network work runs separately from audio and away from the main thread/actor.

## Verification

On 2026-10-01, the local macOS build completed browser sign-in with the configured
Desktop client and a real test account, uploaded its initial replica, then
completed **Sync now** using stored credentials to read and update that replica.
Real Android-to-macOS convergence has not yet been checked on a signed device.

Unit/mock tests cover convergence, separate settings, conflicts, clearing,
clock rollback, first-link defaults, unknown-key preservation, export allowlists,
schema rejection, pagination, private-folder creation, own-file-only updates and
safe auth errors. macOS tests also cover local edits during download and active
session model snapshots, replacement import, deletion, switches, migration from
five-field journals and invalid-rule rejection. A shared JSON fixture verifies
the Android/macOS replacement wire format.

Real-account checks require configured clients: connect both platforms, edit
each synced preference, clear the dictionary, edit different/the same settings
offline, retry after a network failure, deny consent, cancel browser sign-in,
revoke access and disconnect during sync. Check that active dictation retains its
captured settings, hotkeys, focus guards, cancellation and insertion behavior.

References: [Drive app data](https://developers.google.com/workspace/drive/api/guides/appdata),
[Android authorization](https://developer.android.com/identity/authorization),
[Desktop OAuth and PKCE](https://developers.google.com/identity/protocols/oauth2/native-app).
