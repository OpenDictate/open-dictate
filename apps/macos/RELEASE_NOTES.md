OpenDictate for macOS **0.2.0-rc.8** fixes dictation insertion in T3 Code. Requires macOS 14+ on Apple Silicon or Intel.

- T3 Code now receives the completed transcript through guarded clipboard paste. Its rich editor advertises writable Accessibility attributes but acknowledges writes without changing the text, which previously produced the focus-change message and blocked fallback delivery. Both Live and Accurate insert when recording finishes in T3 Code.
- Delivery still requires the original app, exact focused field, unchanged text and unchanged selection. Moving focus or editing the field keeps the result available to copy instead of inserting into another target. Previous clipboard contents are restored if untouched.
- The microphone indicator's finish button now has equal top, bottom and right spacing.
- The saved API key can be copied explicitly from Settings without displaying it. Copying places it on the system clipboard.

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.8-universal.dmg`, open it and drag OpenDictate to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.

Android releases retain their existing `v*` tags. macOS releases use `macos-v*`.
