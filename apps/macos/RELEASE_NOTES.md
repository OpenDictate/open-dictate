OpenDictate for macOS **0.2.0** is the stable release of the 0.2 series. Requires macOS 14+ on Apple Silicon or Intel.

- Dictation continues while typing, moving the cursor or switching windows and apps. Stopping captures the active eligible field and inserts one final result in both Live and Accurate modes.
- Final clipboard delivery works across supported editors, restores the previous clipboard when unchanged and keeps completed results available to copy. Voice editing retains its original-source focus guards and safe cancellation.
- Choose a compact microphone or waveform recording indicator that never takes keyboard focus, with a spinner during processing.
- Configure independent dictation and voice-editing shortcuts, including Globe/Fn and modifier-only taps.
- Choose System, Light or Dark appearance and select speech languages from a searchable list.
- Manage local word replacements and optionally sync the dictionary, replacements, dictation mode and models with Android through Google Drive.
- Optional Accurate punctuation correction is off by default and stays device-local. Transcription and correction results retain their punctuation without a separate final-period override.

This stable release supersedes **0.2.0-rc.24** and can replace an existing RC installation.

### Install

Download `OpenDictate-macOS-0.2.0-universal.dmg`, open it and drag OpenDictate to Applications. Quit an existing copy before replacing it. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. When punctuation correction is enabled, the recognized transcript also goes directly to OpenAI with Responses storage disabled. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
