OpenDictate for macOS **0.2.4** updates the recording microphone in the menu bar. Requires macOS 14+ on Apple Silicon or Intel.

- Shows a filled white microphone on a red circle while recording.
- Centers the visible microphone silhouette inside the circle, with equal opposite insets in light and dark menu bars.
- Keeps the menu-bar item width stable when recording starts or stops.
- Removes the Eject helper from the installer. Use Command + E in the disk image's Finder window to eject it.

### Install

Download `OpenDictate-macOS-0.2.4-universal.dmg`, open it and drag OpenDictate to Applications. Quit an existing copy before replacing it. Existing settings and your Keychain API key are retained. Allow Microphone and Accessibility if prompted.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
