OpenDictate for macOS **0.2.1** fixes focused-field detection in Electron editors. Requires macOS 14+ on Apple Silicon or Intel.

- Fixes Option+Space opening Settings with “Place the cursor in an editable text field” in T3 Code even when its text editor has focus.
- Prepares supported Accessibility flags when entering an app and before capturing a field, allowing Electron to expose its focused editor without VoiceOver.
- Preserves password-field rejection, app exclusions and the focus, text and cursor checks before final insertion. Preparation does not read unrelated UI contents or activate windows.

### Install

Download `OpenDictate-macOS-0.2.1-universal.dmg`, open it and drag OpenDictate to Applications. Quit an existing copy before replacing it. Existing settings and your Keychain API key are retained. Allow Microphone and Accessibility if prompted.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
