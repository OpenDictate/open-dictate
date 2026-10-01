OpenDictate for macOS **0.2.0-rc.19** fixes dictation stopping when you type. Requires macOS 14+ on Apple Silicon or Intel.

- **Type during dictation:** letters, digits, spaces and cursor movement in the original field no longer stop ordinary Live or Accurate dictation. Finish dictation to insert the transcript at the latest cursor/selection while preserving your typed text.
- Changing fields or applications still stops automatic delivery. Voice editing still requires its original source text and selection to remain unchanged. Changes during clipboard staging reject insertion; the transcript remains available to copy.

Includes the settings shortcut fix from rc.18:

- **Close Settings:** Command-W closes the current settings window; Command-Shift-W closes all regular application windows. Settings can be reopened from the menu bar. The recording indicator stays visible.

Includes the waveform spacing improvements from rc.17:

- **Waveform proportions:** the capsule is approximately 62.5 × 28.32 pt, surrounding a 38.63 × 17.5 pt waveform at full level. Both capsule height / waveform height and capsule width / waveform width equal the golden ratio (≈1.618).
- The maximum bar height stays 17.5 pt, with approximately 5.41 pt above and below. The ten bars spread slightly farther apart to balance horizontal spacing.
- Processing retains the compact circular spinner introduced in rc.15.

Includes the original Android application icon restored in rc.16:

- Android uses its original vector icon again: a white outlined microphone with a hollow center on a black background.
- macOS renders that same Android vector directly into its iconset, with no added shadows, gradients or bevels.

Includes the waveform improvements from rc.15:

- **Waveform indicator:** louder audio produces taller bars, with a 50% larger height range than before rc.15.
- **Transcription indicator:** the waveform capsule shrinks to a circle around the native spinner, restoring its original vertical spacing.

Includes the multilingual picker from rc.13:

- **Speech languages** now offers 64 languages with checkboxes. Select several without closing the list, search by English, Russian or native name (or language code), and choose **Automatic detection** to clear all hints.
- Language choices save automatically, apply to both Live and Accurate, and preserve the previous Automatic, Russian, English, Ukrainian and Russian + English settings on upgrade.
- Language changes apply to the next dictation. The picker is unavailable while a session is active.

Includes the clipboard delivery improvements from rc.12:

- Accurate, Live, voice editing and **Paste last transcript** now use one final clipboard paste in all applications, including Google Chrome and T3 Code. Editors that acknowledge Accessibility text writes without applying them no longer prevent clipboard delivery.
- Live continues streaming recognition while you speak; text is inserted into the field after recording stops. Cancellation before delivery leaves the field unchanged.
- Delivery rechecks the original application, exact field, text and UTF-16 selection. Focus changes prevent insertion; ordinary dictation now uses the latest text and selection. Password fields and excluded applications remain protected.
- Paste is sent once and verified by reading back text and caret. If the editor refuses the paste, the completed transcript remains available to copy from the menu.
- Previous clipboard items and formats are restored if unchanged. Temporary transcript items are marked confidential for compatible clipboard utilities.

- Includes the automatic Google Drive synchronization from rc.11: completed settings edits upload after three seconds without further changes, with periodic cloud checks. [Setup and conflict behavior](https://github.com/OpenDictate/open-dictate/blob/main/docs/google-drive-sync.md).

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.19-universal.dmg`, open it and drag OpenDictate to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
