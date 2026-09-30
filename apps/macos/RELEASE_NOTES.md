OpenDictate is now available for macOS 14 and later, on Apple Silicon and Intel.

- Press **Option + Space** to start dictation, then press it again to finish.
- **Live** inserts text as you speak; **Accurate** transcribes the completed recording.
- Press **Option + Shift + Space** to edit selected text with a spoken instruction.
- **Escape** cancels and restores the original text when the field is unchanged.
- Includes a dictionary, local searchable history, explicit AI history search,
  configurable shortcuts, excluded apps, login launch and English/Russian settings.
- Runs in the menu bar. Keys are stored in Keychain, audio stays in memory,
  and requests go directly to OpenAI. There is no backend or analytics.

### Install

Download `OpenDictate-macOS-0.1.0-universal.dmg`, open it and drag OpenDictate
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
