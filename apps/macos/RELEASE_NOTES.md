OpenDictate for macOS **0.2.0-rc.22** fixes dictation when switching windows. Requires macOS 14+ on Apple Silicon or Intel.

- Ordinary Live and Accurate dictation keep recording when you switch fields, windows or apps. Press the shortcut again or click the recording control to stop; the active eligible field at that moment becomes the destination.
- Switching focus during processing prevents automatic insertion into a different field. If the field is unavailable, secure or in an excluded app, the completed text remains available through Copy last transcript.
- Voice editing remains bound to its original text and selection. Escape cancels; revoking Accessibility or sleeping also cancels recording.
- Includes optional Accurate punctuation correction, typing/cursor fixes, clipboard delivery and the waveform indicator.

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.22-universal.dmg`, open it and drag OpenDictate to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. When punctuation correction is enabled, the recognized transcript also goes directly to OpenAI with Responses storage disabled. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
