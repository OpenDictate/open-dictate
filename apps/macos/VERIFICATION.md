# macOS verification

## 0.2.0-rc.4 — indicator and insertion caret

Verified on macOS 26.6.2, Apple Silicon, on 2026-09-30.

- All 41 Swift tests pass. New regression tests exercise cumulative selection
  replacement, UTF-16 caret placement, cancellation, focus/cursor/edit guards,
  and value-only editors that finish a text update after acknowledging AXValue.
  The native NSTextView Accessibility setters verify real text/selection behavior;
  mocked transport tests cover the deferred value/caret-reset sequence.
- Production-view raster tests verify the red finish control during preparation,
  the 44 × 22 pt capsule, 14 pt circle and 7 pt visible trailing inset.
  Offscreen NSHostingView captures were visually inspected for preparation,
  recording and the 22 × 22 pt processing spinner. No focus is taken by rendering.
- Android debug tests/lint/assembly and release lint/assembly pass on the merged
  main-branch code.
- Stable/RC version validation and universal arm64/x86_64 packaging pass;
  bundle build is 5. Ad-hoc signature, DMG integrity and SHA-256 verification pass.
  The release remains locally signed and is not notarized.

The desktop automation did not reliably activate the disposable editor, and
application screenshots were unavailable in this session. The debug bundle was
built, but cross-process insertion, the startup finish-button interaction and
physical hotkeys require a manual check. No API key or history was read by the
smoke checks; no OpenAI request or audio capture was made. Existing secure-field,
permission, third-party clipboard/editor, Spaces and hardware limitations below
still apply. The startup stop path switches the HUD to processing immediately
and waits for recorder setup before submitting once.

## 0.2.0-rc.3 — shortcut customization

Verified on macOS 26.6.2, Apple Silicon, on 2026-09-30.

- All 34 Swift tests pass, including after merging the compact indicator.
  Stable/RC release-version validation also passes. The 11 new shortcut tests cover F1–F20, navigation
  and arbitrary key combinations, synthetic Fn-flag normalization, legacy
  preference migration, independent shortcut persistence, duplicate detection,
  Fn/modifier-tap release and chord guards, autorepeat, and real Carbon
  registration conflict rollback/cleanup.
- In the isolated debug bundle, recorded F5 without modifiers, Shift+Command+F5
  and Command+Q; Command+Q was captured without quitting the application.
  Duplicate assignments kept the previous shortcut and showed a localized error.
  Escape cancelled recording; the key menu assigned Globe/Fn; reset restored
  the defaults. English and Russian settings were inspected in the native window.
- The disposable AppKit editor received synthetic cumulative text and cancellation
  restored its original value and selection. The sequence passed with ordinary
  shortcuts and with the Globe/Fn monitor configured. No API key/history was read,
  OpenAI request made, or audio persisted. The installed app was reopened afterward.
- Android debug tests/lint/assembly and release lint/assembly pass.
- Universal arm64/x86_64 packaging, ad-hoc signature and DMG integrity pass;
  the installer has a SHA-256 attachment. The full release version is
  0.2.0-rc.3; the numeric bundle version is 0.2.0, build 4. This release is not notarized.

Automation delivers keys to an app, so it cannot validate physical global Carbon
hotkeys or synthesize the Globe/Fn key. Fn tap/chord behavior has deterministic
tests, but Apple/external-keyboard event delivery, macOS reserved shortcuts,
permission denial/revocation, Spaces, full-screen apps and Intel/macOS 14 remain
manual compatibility checks. The existing focus/cursor/secure-field guard tests
remain green; provider model access and transcription accuracy are not tested.


## 0.2.0-rc.1

Verified on macOS 26.6.2, Apple Silicon, on 2026-09-30.

- Native captures verify the 88 × 44 pt recording capsule with only a
  microphone and red finish button, and the 44 × 44 pt processing indicator
  with only a spinner. Preparation uses the same spinner layout; idle hides
  the panel.
- A local debug preview uses the production panel and view without recording
  or networking. Clicking its finish button replaced the accessible button
  with a busy indicator. The disposable editor retained its focused text field
  before and after the click.
- 23 Swift tests and the stable/RC release-version validation checks pass.
  Android debug tests/lint/assembly and release lint/assembly pass.
- Universal release packaging verifies arm64/x86_64, the ad-hoc signature and
  DMG integrity. The full RC version appears in the app and installer name;
  the bundle short version remains numeric, with build number 2.

The preview validates the HUD interaction and state transition. Actual OpenAI
submission is not exercised; the finish action uses the existing session stop
path. The compatibility limitations below still apply.

## 0.1.0

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
