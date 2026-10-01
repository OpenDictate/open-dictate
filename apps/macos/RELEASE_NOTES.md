OpenDictate for macOS **0.2.0-rc.20** adds optional punctuation correction for Accurate dictation. Requires macOS 14+ on Apple Silicon or Intel.

- **Correct punctuation in Accurate:** a new switch in Settings → Dictation, off by default. After recognition, GPT-6 Luna corrects punctuation and capitalization before local replacements and the final-period preference. It adds a separate OpenAI request and processing time.
- Live and voice editing skip punctuation correction. Failed requests or responses that alter words, order, numbers or addresses preserve the original transcription. Cancellation prevents delivery.
- The switch synchronizes with Android through the existing Google Drive connection. Older installations default to off; existing cloud values win on first connection.
- Includes the prior fixes for typing/cursor movement during dictation, Command-W in Settings, clipboard delivery, the waveform indicator and the multilingual speech-language picker.

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.20-universal.dmg`, open it and drag OpenDictate to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. When punctuation correction is enabled, the recognized transcript also goes directly to OpenAI with Responses storage disabled. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
