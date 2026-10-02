OpenDictate for macOS **0.2.3** fixes the installer item order. Requires macOS 14+ on Apple Silicon or Intel.

- Opens the disk image with a fixed left-to-right row: **Applications → Install OpenDictate.txt → OpenDictate.app → Eject.app**.
- Keeps Eject at the far right instead of placing it between Applications and the installation instructions.

### Install

Download `OpenDictate-macOS-0.2.3-universal.dmg`, open it and drag OpenDictate to Applications. Quit an existing copy before replacing it. Existing settings and your Keychain API key are retained. Allow Microphone and Accessibility if prompted.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
