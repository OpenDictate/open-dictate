OpenDictate for macOS **0.2.0-rc.6** adds a choice of recording indicators,
including a live microphone-volume waveform. Requires macOS 14+ on Apple Silicon or Intel.

- In **Settings → Recording indicator**, choose **Microphone** or **Waveform**.
  The choice is saved; existing installations keep the microphone indicator.
- The new waveform is a small black capsule with ten white bars showing recent
  microphone volume, from oldest on the left to newest on the right. Silence
  produces short bars; louder input produces taller bars. This is real input
  level history, not an artificial animation.
- Click anywhere on the waveform capsule or use your dictation shortcut to
  finish. It shows a spinner during processing and never takes keyboard focus.
  Escape cancels as usual. Hide either style using **Show recording status**.
- Choose **System**, **Light** or **Dark** appearance from the theme button.
  The choice applies to all settings pages and is saved across restarts.
  Existing installations retain their dark appearance until you choose another theme.
- The indicator is half as wide and half as tall: **44 × 22 pt**, with balanced
  spacing around the microphone and red finish button.
- The finish button appears as soon as the session starts, including preparation.
  Clicking it during startup immediately shows processing and submits once ready.
- Text insertion keeps the cursor at the end of the inserted text. Live partials
  replace the previous transcript; cancellation restores the original selection.
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
  taking focus from the editor. Only processing shows the smaller native spinner (**22 × 22 pt**).
- **Escape** still cancels and restores an unchanged field. Live, Accurate,
  voice editing, dictionary and history remain available.

For F1–F12, enable standard function keys in macOS Keyboard settings or hold Fn.
For Globe/Fn taps, set **Keyboard → Press 🌐 key to → Do Nothing** and allow
Accessibility. Some external keyboards handle Fn internally and do not send it
to macOS. System-reserved keys may remain unavailable; Globe/Fn and modifier-only
shortcuts do not suppress existing macOS or other-app actions.

This is a **prerelease**, published separately from the stable macOS download.

### Install

Download `OpenDictate-macOS-0.2.0-rc.6-universal.dmg`, open it and drag OpenDictate
to Applications. Save your own OpenAI API key and allow Microphone and Accessibility.

This release is **locally signed and not notarized**. If Gatekeeper blocks
launching it, use **System Settings → Privacy & Security → Open Anyway**
for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or
analytics. No Input Monitoring permission is requested. API usage is billed
separately by OpenAI. Check the `.sha256` attachment to verify the installer.

Android releases retain their existing `v*` tags. macOS releases use `macos-v*`.
