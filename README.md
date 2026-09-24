# OpenDictate for Android

OpenDictate turns speech into text in any Android app. When the keyboard opens,
a small dictation button appears above it. Text is inserted directly into the
active input field through the Android Accessibility API.

Visit the [OpenDictate website](https://opendictate.github.io/open-dictate/) or
download the latest APK from [GitHub Releases](https://github.com/OpenDictate/open-dictate/releases/latest).

## Features

- **GPT Live Transcribe** (`gpt-live-transcribe`) — streams text as you speak
  with minimal latency.
- **GPT Transcribe** (`gpt-transcribe`) — accurately transcribes a completed recording.
- Voice-directed text transformation through the Responses API, with
  **GPT-6 Luna** (`gpt-6-luna`) as the default and **GPT-6 Sol** (`gpt-6-sol`) selectable.
- Text transformations stop after 30 seconds. The model can occasionally answer
  a direct question or explain a failed edit in a private Android notification.
- Your own OpenAI API key, encrypted with Android Keystore.
- Local transcript history with deletion, on-device fuzzy search, and explicit
  AI semantic search through the Responses API.
- No backend, analytics, or stored recordings.
- A floating dictation button that appears only while the keyboard is open,
  can be dismissed with a right swipe, and reveals text editing on long-press.
- Completed text falls back to the system clipboard if the original input field
  is no longer available.
- Russian and English speech recognition, or automatic language detection.

## Installation

1. Download the APK from the **Releases** page and install it.
2. Open OpenDictate and save your OpenAI API key.
3. Grant microphone access.
4. Enable the OpenDictate service in the system accessibility settings.
5. Open an input field in any app. Tap the floating button for dictation, or
   long-press it and choose **Edit text** to speak an instruction that transforms
   selected text (or the whole field when nothing is selected). Swipe the button
   right to hide it until the keyboard closes.

> Version 0.6.0 introduces the new `com.opendictate.app` package identity. It
> installs separately from earlier builds, so enable the new accessibility
> service and enter the API key again after installation.

> Accessibility access is used to detect the open keyboard and update the
> focused field. Text is sent to OpenAI only when you explicitly start a
> transformation; unrelated screen contents are not collected.

## Building

Requires JDK 17 and Android SDK 37.0.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
```

The debug APK will be in `app/build/outputs/apk/debug/`.

## Releases

The `.github/workflows/release.yml` workflow builds and publishes a signed APK
for `v*` tags other than preview tags. Tags with a prerelease suffix, such as
`v0.6.6-rc.1`, publish as GitHub prereleases and use the regular
`com.opendictate.app` application ID. The repository requires four
Actions secrets:

- `SIGNING_KEY` — a base64-encoded JKS file;
- `KEY_ALIAS`;
- `KEY_PASSWORD`;
- `STORE_PASSWORD`.

The `.github/workflows/preview.yml` workflow publishes a signed prerelease from
`main` every Sunday when commits have landed since the previous release. It can
also be run manually. Preview tags follow the next-patch SemVer form
`vX.Y.Z-preview.N` (for example, `v0.5.1-preview.123`). The preview APK uses the
`com.opendictate.app.preview` application ID and installs alongside the stable app.
It has separate settings; disable the stable accessibility service before
enabling the preview service to avoid showing two overlays.

## Privacy and security

The API key is encrypted using hardware-backed Android Keystore when supported
by the device. Transcript history stays in the app-private local database.
Audio, text chosen for transformation, and history explicitly submitted to AI
search are sent directly from your phone to OpenAI. See
[PRIVACY.md](PRIVACY.md) and [SECURITY.md](SECURITY.md) for details.

## License

MIT
