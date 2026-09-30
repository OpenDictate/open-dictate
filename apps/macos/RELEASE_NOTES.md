Release candidate for OpenDictate 0.2.0, for macOS 14 and later on Apple Silicon and Intel.

- A compact dark indicator replaces the large recording panel.
- While recording, it shows only a microphone and a red finish button.
- Click the red button or press **Option + Space** to finish and submit.
  The panel keeps keyboard focus in your editor.
- After submission, only a native spinner remains. The indicator disappears
  when dictation completes or is cancelled.
- **Escape** still cancels and restores an unchanged field.
- Live, Accurate, voice editing, dictionary and history remain available.

This is a **prerelease**, published separately from the stable 0.1.0 download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.1-universal.dmg`, open it and drag OpenDictate
to Applications. Launch it, save your own OpenAI API key, and allow Microphone
and Accessibility in the app's setup screen.

This release is **locally signed and not notarized**. If Gatekeeper blocks
launching it, use **System Settings → Privacy & Security → Open Anyway**
for OpenDictate, then launch it again. The disk image includes bilingual instructions.

For editors without writable Accessibility text, insertion briefly uses the
clipboard and restores its previous contents if unchanged. Focus changes never
send text to a different field; a completed result stays available from the menu.

API usage is billed separately by OpenAI. Check the `.sha256` attachment to
verify the downloaded installer.

Android releases retain their existing `v*` tags. macOS releases use `macos-v*`.
