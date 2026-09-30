# OpenDictate for macOS

A native menu bar application built with SwiftUI, AppKit, AVAudioEngine,
Accessibility, Keychain and URLSession. No third-party dependencies or backend.

## Development

Requires macOS 14+ and Xcode 16+ (Swift 5.9 package, compatible with Swift 6).

```bash
swift test --package-path apps/macos
apps/macos/scripts/package.sh
open dist/macos/OpenDictate.app
```

Open `Package.swift` in Xcode for source editing and debugging. Run the bundled
app for permission testing: a bare Swift executable has a different TCC identity.
The universal build targets `arm64` and `x86_64`, with a macOS 14 deployment target.

## Runtime

- `AppModel` owns a single session, immutable settings, cancellation, permissions
  and result delivery. Session UUIDs discard late callbacks.
- `TextTarget` captures the focused Accessibility element, process, value and
  UTF-16 selection. Every insertion rechecks focus, text and caret. Partials
  replace the original selection. Cancel restores only an untouched target.
- `HotKeys` uses exclusive Carbon registration for ordinary key combinations.
  Globe/Fn combinations and modifier-only taps use AppKit event monitors with
  the existing Accessibility permission, without requesting Input Monitoring.
  Only key codes and modifier flags are inspected; typed characters are not read
  or stored. Holding a shortcut does not repeatedly toggle dictation. Escape is
  registered only during recording/processing (or when explicitly assigned).
- `AudioRecorder` copies tap buffers and converts on a serial worker to mono
  PCM16 at 24 kHz. Chunks are 40 ms. Live audio stays off disk; Accurate builds
  a WAV in memory. Both stop below the provider's 25 MB upload limit.
- `LiveTranscriptionSession` opens immediately, bounds early audio to 300 chunks,
  flushes after `session.updated`, serializes sends and explicitly commits.
  Overflow fails visibly instead of silently dropping speech.
- `OpenAIClient` uses ephemeral HTTPS sessions. Voice edits and explicit AI
  history searches use structured Responses output with `store: false`.
- `RecordingIndicator` owns a nonactivating panel that cannot become key/main.
  From preparation through recording it shows a microphone and red finish button
  (44 × 22 pt); processing shows only a native spinner (22 × 22 pt). Clicking the
  red button calls the same stop/submit path as the shortcut without moving focus.
- The HUD never takes keyboard focus. Changing fields, typing, changing the
  caret, revoking Accessibility or sleeping detaches/stops a recording.
  Final results remain available to copy, without insertion into a new field.

Supported editors expose an Accessibility string value and selected text range.
Writable values receive live updates through selected-text replacement when supported,
with a verified whole-value fallback. The caret is placed after value readback. Other accessible editors receive a final
paste using the clipboard. Editors without usable Accessibility metadata need
manual Copy last transcript. Password fields are deliberately excluded.

### Keyboard shortcuts

In Settings, click either shortcut and press the desired combination. Letters,
symbols, navigation, numpad and F1–F20 keys work with any combination of Control,
Option, Shift and Command, or without modifiers. Dictation and voice editing are
configured independently; their defaults remain Option+Space and Option+Shift+Space.
Existing preferences migrate automatically. Escape cancels shortcut recording;
use the adjacent key menu to assign Escape, an F-key, or Globe/Fn directly.
During an active dictation, unmodified Escape always cancels even if assigned.
Conflicting registrations leave the previous shortcut working and show an error.

Press and release modifiers to assign a modifier-only shortcut, including
Globe/Fn. Modifier-only shortcuts fire on release; using the modifier with another
key does not start dictation. Globe/Fn with an ordinary key is also supported.
These monitored shortcuts do not suppress system/other-app actions. In macOS
Keyboard settings, set “Press 🌐 key to” to “Do Nothing” to avoid input-source,
emoji or system dictation actions. Accessibility must be allowed. Some external
keyboards handle Fn in firmware and never send its events to macOS.

For F1–F12, enable “Use F1, F2, etc. keys as standard function keys” under Keyboard
→ Keyboard Shortcuts → Function Keys, or hold Fn when pressing the key. The recorder
normalizes the function flag on F/navigation keys so they remain ordinary keys.
Keys reserved by macOS or intercepted by keyboard firmware cannot be guaranteed.
See [Apple's function-key guide](https://support.apple.com/102439) and
[AppKit event monitoring](https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/EventOverview/MonitoringEvents/MonitoringEvents.html).

Credentials live in a device-only, non-synchronizing Keychain item. Preferences
use the app's UserDefaults domain. The last 500 completed transcripts are saved
in `~/Library/Application Support/OpenDictate/history.json` with owner-only
permissions. Disable history to stop saving future results; delete existing
history separately. Audio is never persisted on macOS.

## Verification

Unit tests exercise UTF-16 selections, cumulative updates, cursor guards,
WAV/chunk boundaries, protocol payloads, ordered early-audio flush and commit,
bounded queues, cancellation, local search and history batching. Mock transport
tests do not require a key or call OpenAI.

For native smoke checks, run `scripts/build-smoke.sh` and
`scripts/build-editor-fixture.sh`, then launch the printed app paths. The smoke
bundle enables local-only checks automatically; other debug bundles accept
`--local-smoke-test`.
This debug-only mode uses the real shortcut and Accessibility insertion with
synthetic text and provides a two-second microphone check in its menu. It makes
no OpenAI requests, never reads your API key or history, and writes no audio.
It uses a separate preference domain. These checks are absent from release builds.

For a network-free indicator preview, also pass `--hud-preview`. It shows the
production panel alone; clicking its finish button switches the preview to the
spinner. Debug menu actions can show either state. Previewing never records audio.

Use a disposable editor document to check cumulative insertion, Escape rollback,
cursor movement, switching fields/apps, secure fields and clipboard fallback.
Test permissions granted/denied, startup, menu actions, both languages, Spaces,
full-screen windows and logout/login. A real OpenAI key is required to validate
provider access and transcription accuracy; mock tests cannot establish either.

## Packaging and releases

`scripts/package.sh` emits `dist/macos/OpenDictate.app`, a universal DMG, and a
SHA-256 checksum. It validates both CPU slices, the bundle signature and the DMG.
The default uses an ad-hoc signature with a stable identifier requirement so
local Accessibility grants can survive rebuilds. This is not a Developer ID
signature or Apple notarization.

For Developer ID packaging, use an identity already present in the login
keychain, plus an existing `notarytool` keychain profile:

```bash
MACOS_SIGNING_IDENTITY='Developer ID Application: Your Name (TEAMID)' \
MACOS_NOTARY_PROFILE='opendictate-notary' apps/macos/scripts/package.sh
```

No certificate, private key or Apple credential belongs in the repository.
To release, update `VERSION`, `Resources/Info.plist` and `RELEASE_NOTES.md`, pass
platform checks, merge to main and push a matching `macos-vX.Y.Z` tag. Verify
the green release workflow, the DMG and checksum attachments before reporting
completion. macOS and Android versions are independent.

Release candidates use `X.Y.Z-rc.N` in `VERSION`, the displayed app version,
DMG filename and `macos-v*` tag. The Apple bundle short version contains only
`X.Y.Z`, per Apple's
[bundle version format](https://developer.apple.com/documentation/bundleresources/information-property-list/cfbundleshortversionstring);
increment the numeric bundle build for each distribution. Release CI
marks RC tags as GitHub prereleases and leaves the stable download unchanged.
`scripts/test-release-version.sh` verifies accepted/rejected version strings.

Protocol references checked on 2026-09-30:
[Realtime transcription](https://developers.openai.com/api/docs/guides/realtime-transcription),
[file transcription](https://developers.openai.com/api/docs/guides/speech-to-text),
[Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs).
