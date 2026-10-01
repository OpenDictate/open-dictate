OpenDictate for macOS **0.2.0-rc.13** adds a searchable, multilingual dictation picker. Requires macOS 14+ on Apple Silicon or Intel.

- **Speech languages** now offers 64 languages with checkboxes. Select several without closing the list, search by English, Russian or native name (or language code), and choose **Automatic detection** to clear all hints.
- Language choices save automatically, apply to both Live and Accurate, and preserve the previous Automatic, Russian, English, Ukrainian and Russian + English settings on upgrade.
- Language changes apply to the next dictation. The picker is unavailable while a session is active.

Includes the clipboard delivery improvements from rc.12:

- Accurate, Live, voice editing and **Paste last transcript** now use one final clipboard paste in all applications, including Google Chrome and T3 Code. Editors that acknowledge Accessibility text writes without applying them no longer prevent clipboard delivery.
- Live continues streaming recognition while you speak; text is inserted into the field after recording stops. Cancellation before delivery leaves the field unchanged.
- Delivery rechecks the original application, exact field, text and UTF-16 selection. Focus changes or user edits prevent insertion. Password fields and excluded applications remain protected.
- Paste is sent once and verified by reading back text and caret. If the editor refuses the paste, the completed transcript remains available to copy from the menu.
- Previous clipboard items and formats are restored if unchanged. Temporary transcript items are marked confidential for compatible clipboard utilities.

- Includes the automatic Google Drive synchronization from rc.11: completed settings edits upload after three seconds without further changes, with periodic cloud checks. [Setup and conflict behavior](https://github.com/OpenDictate/open-dictate/blob/main/docs/google-drive-sync.md).

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.13-universal.dmg`, open it and drag OpenDictate to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
