OpenDictate for macOS **0.4.1** reduces Google Drive sync traffic. Requires macOS 14+ on Apple Silicon or Intel.

- Batch local settings edits after 15 seconds without further changes, and check cloud settings every 15 minutes instead of every minute.
- Opening Settings or waking your Mac checks only when the last successful sync is at least five minutes old. Network recovery resumes pending work.
- Network failures retry after 1, 5, 15 and then 30 minutes; authorization failures pause automatic attempts until sign-in.
- Cache validated cloud replicas only in memory and download them again only when their Drive version changes. Reconnecting and choosing a settings source still read fresh cloud data.
- Manual synchronization remains available and respects unfinished edits. API keys, audio and history remain excluded.

### Install

Download `OpenDictate-macOS-0.4.1-universal.dmg`, open it and drag OpenDictate to Applications. Quit an existing copy before replacing it. Existing settings and your Keychain API key are retained. Allow Microphone and Accessibility if prompted.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
