OpenDictate for macOS **0.2.0-rc.10** adds optional Google Drive settings synchronization. Requires macOS 14+ on Apple Silicon or Intel.

- Connect **Google Drive** in Settings to sync the dictionary, word replacement rules and enabled states, dictation mode and selected models with Android. Changes sync automatically while the app runs; **Sync now** retries immediately.
- Rules keep their IDs, spelling and individual enabled states across devices. The master replacement switch syncs independently. Active dictation keeps the settings and rules captured at recording start.
- Conflicts merge per preference; the latest complete dictionary or replacement list wins. Clearing rules stays cleared when an older device reconnects. Older sync clients preserve the new fields.
- macOS sign-in uses the system browser with PKCE and a ten-minute timeout. OAuth credentials use a separate device-only Keychain item. Errors keep local settings available, and oversized settings can be corrected without restarting.
- Only selected settings go to Google’s hidden app-data folder. OpenAI API keys, audio and history stay out of sync. Disconnect keeps local/cloud settings. Replacement rules remain separate from recognition dictionary hints.

### Google Drive setup

Use a Desktop OAuth client from the same Google Cloud project as the Android app. Enter its client ID and secret in Settings, then connect the same Google account on both devices. See [setup and conflict behavior](https://github.com/OpenDictate/open-dictate/blob/main/docs/google-drive-sync.md). The configured OpenDictate project currently uses **Testing** with the owner’s account registered as a test user.

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.10-universal.dmg`, open it and drag OpenDictate to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.

Android releases retain their existing `v*` tags. macOS releases use `macos-v*`.
