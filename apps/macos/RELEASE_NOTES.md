OpenDictate for macOS **0.2.0-rc.24** simplifies punctuation settings. Requires macOS 14+ on Apple Silicon or Intel.

- Correct punctuation is off by default and appears only in Accurate mode, with a shorter explanation.
- The final-period setting has been removed. Transcription and correction results retain their punctuation without a separate period override.
- Punctuation correction stays device-local; existing choices are preserved and never sync through Google Drive.
- Retains dictation across window changes, final clipboard delivery and the waveform indicator.

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.24-universal.dmg`, open it and drag OpenDictate to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. When punctuation correction is enabled, the recognized transcript also goes directly to OpenAI with Responses storage disabled. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
