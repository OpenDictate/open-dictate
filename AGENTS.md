# OpenDictate agent guide

## Product contract

OpenDictate is a single-module Android app that inserts OpenAI speech
transcriptions into the currently focused text field. An accessibility overlay
appears only while an IME window and an editable node are both present. A tap
starts dictation; a second tap stops it.

The user chooses between two paths:

- `LIVE` uses `gpt-live-transcribe` over a Realtime WebSocket and applies
  partial transcripts while the user speaks.
- `ACCURATE` records a temporary WAV file and sends it to
  `/v1/audio/transcriptions` with `gpt-transcribe` after recording stops.

There is no backend. Audio goes directly from the device to OpenAI. The user
supplies the API key, which is encrypted with Android Keystore. Preserve this
privacy boundary: never log keys, audio, transcripts, focused-field contents,
or raw OpenAI payloads; never add analytics or persistent audio storage without
an explicit product decision.

## Runtime flow and ownership

1. `OpenDictateAccessibilityService` detects the keyboard and focused editable
   node, owns `DictationOverlayView`, snapshots the original text and selection,
   and inserts transcript updates with accessibility actions.
2. `DictationForegroundService` owns one recording session, microphone
   lifecycle, notification, model selection, cleanup, and OpenAI client calls.
3. `DictationStateBus` is the in-process handoff between those services. Session
   IDs prevent stale results from an earlier dictation from reaching a newer
   target.
4. `OpenAiTranscriptionClient` implements the file and Realtime protocols.
5. `PcmAudioRecorder` emits 24 kHz, mono, PCM16 chunks every 40 ms. `WavFile`
   adds the container only for the accurate path.

Partial live text is cumulative. `EditableTextSnapshot.compose()` always
replaces the selection captured at session start, so every delta replaces the
previous partial result instead of appending it. Keep the target-package check
before `ACTION_SET_TEXT`; text must not be inserted after focus moves to another
app. Hiding the keyboard stops an active session and removes the overlay.

## Latency invariants

Treat the live path as a tight loop:

- Keep microphone chunks small and stream them without disk I/O.
- Open the WebSocket immediately. Audio produced before `session.updated` is
  held in the bounded in-memory queue and flushed as soon as configuration is
  acknowledged.
- Keep Realtime turn detection disabled; the foreground service explicitly
  commits when the user stops.
- Apply partial transcripts immediately through `DictationStateBus`.
- Keep blocking audio and network work off the main thread.

Measure before changing chunk size, queue bounds, timeouts, or overlay debounce.
Optimizing throughput at the cost of first-text latency is a regression for this
product.

## Project map

- `app/src/main/java/com/opendictate/app/service/`: accessibility integration,
  foreground dictation lifecycle, text composition, and session state.
- `app/src/main/java/com/opendictate/app/network/`: OpenAI HTTP/WebSocket client.
- `app/src/main/java/com/opendictate/app/audio/`: PCM capture and WAV encoding.
- `app/src/main/java/com/opendictate/app/data/`: encrypted credentials and local
  preferences.
- `app/src/main/java/com/opendictate/app/ui/`: Compose setup screen and state.
- `app/src/test/`: JVM tests for pure text and WAV logic.
- `.github/workflows/build.yml`: main/PR verification and debug APK artifact.
- `.github/workflows/release.yml`: signed APK publication for `v*` tags.

The build uses AGP 9 built-in Kotlin. Keep the Compose compiler plugin, but do
not re-add `org.jetbrains.kotlin.android`. `compileSdk` is 37 while `targetSdk`
remains 36; changing the target opts the app into new runtime behavior and must
be reviewed separately.

## Change workflow

Before editing, trace the affected path end to end. Changes to transcription or
Accessibility usually cross the overlay, session state, foreground service,
and client rather than belonging to one class in isolation. Verify current
OpenAI protocol fields against official OpenAI documentation whenever changing
model names, endpoints, Realtime events, or audio configuration.

Run the smallest relevant tests while iterating, then finish with:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew lintRelease assembleRelease
```

The work is complete when both commands pass, no generated build output is
committed, and behavior-specific coverage exists for pure logic. Exercise
overlay visibility, focus changes, cancellation, and transcript insertion on a
real device or emulator for Accessibility changes; JVM tests cannot validate
those system interactions.

## Releases and secrets

Release signing is configured only when `SIGNING_KEY`, `KEY_ALIAS`,
`KEY_PASSWORD`, and `STORE_PASSWORD` are present. `SIGNING_KEY` is a base64 JKS.
Never commit a keystore, decoded signing material, API key, or local properties.
Tags matching `v*` trigger the release workflow; confirm its green run and the
signed APK attachment before declaring a release complete.

Temporary WAV files must remain cache-only and be deleted in `finally`. Keep
`android:allowBackup="false"`, cleartext traffic disabled, and accessibility
scope limited to keyboard detection and focused-field insertion. Review
`PRIVACY.md` and `SECURITY.md` whenever data handling, permissions, networking,
or storage changes.
