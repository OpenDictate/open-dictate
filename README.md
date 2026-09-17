# OpenWhispr for Android

OpenWhispr turns speech into text in any Android app. When the keyboard opens,
a small dictation button appears above it. Text is inserted directly into the
active input field through the Android Accessibility API.

## Features

- **GPT Live Transcribe** (`gpt-live-transcribe`) — streams text as you speak
  with minimal latency.
- **GPT Transcribe** (`gpt-transcribe`) — accurately transcribes a completed recording.
- Your own OpenAI API key, encrypted with Android Keystore.
- No backend, analytics, or stored recordings.
- A floating button that appears only while the keyboard is open.
- Russian and English speech recognition, or automatic language detection.

## Installation

1. Download the APK from the **Releases** page and install it.
2. Open OpenWhispr and save your OpenAI API key.
3. Grant microphone access.
4. Enable the OpenWhispr service in the system accessibility settings.
5. Open an input field in any app and tap the button above the keyboard.

> Accessibility access is used only to detect the open keyboard and insert
> transcripts into the selected field. OpenWhispr does not collect screen contents.

## Building

Requires JDK 17 and Android SDK 37.0.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
```

The debug APK will be in `app/build/outputs/apk/debug/`.

## Releases

The `.github/workflows/release.yml` workflow builds and publishes a signed APK
for `v*` tags. The repository requires four Actions secrets:

- `SIGNING_KEY` — a base64-encoded JKS file;
- `KEY_ALIAS`;
- `KEY_PASSWORD`;
- `STORE_PASSWORD`.

## Privacy and security

The API key is encrypted using hardware-backed Android Keystore when supported
by the device. Audio is sent directly from your phone to OpenAI. See
[PRIVACY.md](PRIVACY.md) and [SECURITY.md](SECURITY.md) for details.

## License

MIT
