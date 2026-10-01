OpenDictate for macOS **0.2.0-rc.11** uses clipboard insertion in every supported editor. Requires macOS 14+ on Apple Silicon or Intel.

- Accurate, Live, voice editing and **Paste last transcript** now use one final clipboard paste in all applications, including Google Chrome and T3 Code. Editors that acknowledge Accessibility text writes without applying them no longer prevent clipboard delivery.
- Live continues streaming recognition while you speak; text is inserted into the field after recording stops. Cancellation before delivery leaves the field unchanged.
- Delivery rechecks the original application, exact field, text and UTF-16 selection. Focus changes or user edits prevent insertion. Password fields and excluded applications remain protected.
- Paste is sent once and verified by reading back text and caret. If the editor refuses the paste, the completed transcript remains available to copy from the menu.
- Previous clipboard items and formats are restored if unchanged. Temporary transcript items are marked confidential for compatible clipboard utilities.

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.11-universal.dmg`, open it and drag OpenDictate to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
