# macOS 0.1.0 verification

Verified on macOS 26.6.2, Apple Silicon, on 2026-09-30. The deployment target is
macOS 14; Intel is cross-compiled, not exercised on Intel hardware.

- 23 Swift tests cover UTF-16 composition and delivery guards, audio/WAV
  boundaries, protocol payloads, HTTP responses, WebSocket ordering, bounded
  queues, cancellation, structured edits and history search batching.
- The bundled debug app captured two seconds of real microphone input as
  24 kHz mono PCM16 and discarded it in memory.
- A disposable AppKit editor received synthetic cumulative text through the
  production Accessibility integration. Cancellation restored the original
  text and selected range. A late update after focus changed was rejected.
- Capture of the fixture's secure text field was rejected.
- Dictation, dictionary, empty history and settings were visually inspected in
  the native window; Russian navigation was also inspected at the minimum
  window size. Synthetic key paste into SecureField worked; selecting text in
  Dictionary enabled the standard responder-chain editing commands.
- Packaging verifies both CPU slices, the ad-hoc bundle signature, disk image
  integrity and writes a SHA-256 checksum.
- Android debug tests/lint/assembly and release lint/assembly pass after the
  monorepo migration; source changes from main are preserved.

Local smoke checks use an isolated preference domain and in-memory history.
They never read the user's API key/history or call OpenAI. Tests use mock
provider responses, so they do not establish API-account access, transcription
accuracy or provider latency. The UI automation sends keyboard events directly
to an app rather than through the global Carbon shortcut; the native insertion
checks therefore also have delayed debug menu actions. Physical global hotkeys,
clipboard fallback in third-party editors, login launch, Spaces, and Intel/
macOS 14 hardware remain manual compatibility checks. All debug check actions
are absent from release builds.
