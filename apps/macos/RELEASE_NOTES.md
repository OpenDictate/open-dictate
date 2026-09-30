OpenDictate for macOS **0.2.0-rc.3** expands keyboard shortcut customization and
includes the compact dictation indicator. Requires macOS 14+ on Apple Silicon or Intel.

- Record arbitrary key combinations instead of choosing from a fixed list.
  Supports letters, symbols, navigation, numpad keys and **F1–F20**, with or
  without Control, Option, Shift and Command.
- Assign **Globe/Fn**, modifier-only taps, or Globe/Fn with an ordinary key.
  Modifier taps fire on release and do not activate when used with another key.
- Configure dictation and voice editing independently, including Shift in either
  shortcut. Existing settings migrate automatically; defaults remain
  **Option+Space** and **Option+Shift+Space**.
- Select F-keys or Globe/Fn directly from the key menu, cancel recording with
  Escape, or reset both shortcuts. Failed registrations preserve working bindings.
- The compact recording indicator shows a microphone and red finish button.
  Click the button or press your dictation shortcut to finish and submit without
  taking focus from the editor. Preparation and processing show a native spinner.
- **Escape** still cancels and restores an unchanged field. Live, Accurate,
  voice editing, dictionary and history remain available.

For F1–F12, enable standard function keys in macOS Keyboard settings or hold Fn.
For Globe/Fn taps, set **Keyboard → Press 🌐 key to → Do Nothing** and allow
Accessibility. Some external keyboards handle Fn internally and do not send it
to macOS. System-reserved keys may remain unavailable; Globe/Fn and modifier-only
shortcuts do not suppress existing macOS or other-app actions.

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.3-universal.dmg`, open it and drag OpenDictate
to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks
launching it, use **System Settings → Privacy & Security → Open Anyway**
for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or
analytics. No Input Monitoring permission is requested. API usage is billed
separately by OpenAI. Check the `.sha256` attachment to verify the installer.

Android releases retain their existing `v*` tags. macOS releases use `macos-v*`.
