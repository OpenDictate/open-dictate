OpenDictate for macOS **0.2.2** improves the menu-bar recording indicator and installer. Requires macOS 14+ on Apple Silicon or Intel.

- Keeps the microphone in the menu bar throughout dictation. During recording, only its filled head turns red; the cradle and stem retain the normal menu-bar foreground color.
- Removes the waveform replacement and the processing ellipsis. Transcribing shows the ordinary microphone without changing the status-item width.
- Adds an Eject helper to the disk image so the installer can be closed after copying the app to Applications.

### Install

Download `OpenDictate-macOS-0.2.2-universal.dmg`, open it and drag OpenDictate to Applications. Quit an existing copy before replacing it. Existing settings and your Keychain API key are retained. Allow Microphone and Accessibility if prompted.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
