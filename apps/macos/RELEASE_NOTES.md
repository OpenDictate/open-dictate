OpenDictate for macOS **0.2.0-rc.7** improves transcript insertion and keyboard shortcut settings. Requires macOS 14+ on Apple Silicon or Intel.

- Both recording indicators and their processing spinners are **25% larger**. The microphone capsule is now 55 × 27.5 pt; the waveform capsule is 62.5 × 27.5 pt.
- Click a shortcut in Settings and press **Escape** to clear it. Either or both shortcuts can remain unassigned across restarts. Dictation and voice editing remain available from the menu; Escape still cancels an active session. Use the adjacent key menu to assign Escape itself.
- Transcript delivery waits briefly for editors to expose acknowledged Accessibility text and cursor changes. Live partials are serialized and the app no longer treats its own pending cursor update as a user focus change. Focus changes and unrelated edits still block delivery.
- The indicator's finish control uses native, non-focusing mouse handling and submits after the click is released. Cancellation waits for an acknowledged insertion to settle before restoring the original selection.

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.7-universal.dmg`, open it and drag OpenDictate to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.

Android releases retain their existing `v*` tags. macOS releases use `macos-v*`.
