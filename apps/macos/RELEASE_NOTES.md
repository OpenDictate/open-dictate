OpenDictate for macOS **0.4.0** adds safe settings-source selection when connecting Google Drive. Requires macOS 14+ on Apple Silicon or Intel.

- When cloud and local settings differ, choose **Cloud settings** (the default) or **Local settings** before either source changes. The choice covers dictionary, word replacements, dictation mode and selected models.
- Cloud settings replace synced values on this Mac. Local settings are published to Google Drive and received by other connected devices. Dictionaries and replacement lists are selected as a whole.
- Cancel leaves local and cloud settings unchanged. A pending choice blocks automatic synchronization, including after network errors or restarting the app.
- Settings are downloaded again before applying your choice; if either source changed while you were choosing, the app asks again. Empty clouds and matching values connect without an extra step.
- After connecting, automatic synchronization continues as before. Existing connections are preserved; reconnect to choose a source again. API keys, audio and history remain excluded.

### Install

Download `OpenDictate-macOS-0.4.0-universal.dmg`, open it and drag OpenDictate to Applications. Quit an existing copy before replacing it. Existing settings and your Keychain API key are retained. Allow Microphone and Accessibility if prompted.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
