# OpenWispr for Android

OpenWispr turns speech into text in any Android app. When the keyboard opens,
a small dictation button appears above it. Text is inserted directly into the
active input field through the Android Accessibility API.

## Features

- **GPT Live Transcribe** (`gpt-live-transcribe`) — streams text as you speak
  with minimal latency.
- **GPT Transcribe** (`gpt-transcribe`) — accurately transcribes a completed recording.
- Voice-directed text transformation through the Responses API, with
  **GPT-5.6 Luna** (`gpt-5.6-luna`) as the default and Terra or Sol selectable.
- Text transformations stop after 30 seconds. The model can occasionally answer
  a direct question or explain a failed edit in a private Android notification.
- Your own OpenAI API key, encrypted with Android Keystore.
- No backend, analytics, or stored recordings.
- A floating dictation button that appears only while the keyboard is open,
  can be dismissed with a right swipe, and reveals text editing on long-press.
- Russian and English speech recognition, or automatic language detection.

## Installation

1. Download the APK from the **Releases** page and install it.
2. Open OpenWispr and save your OpenAI API key.
3. Grant microphone access.
4. Enable the OpenWispr service in the system accessibility settings.
5. Open an input field in any app. Tap the floating button for dictation, or
   long-press it and choose **Edit text** to speak an instruction that transforms
   selected text (or the whole field when nothing is selected). Swipe the button
   right to hide it until the keyboard closes.

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
for `v*` tags. The repository requires four Actions secrets:

- `SIGNING_KEY` — a base64-encoded JKS file;
- `KEY_ALIAS`;
- `KEY_PASSWORD`;
- `STORE_PASSWORD`.

## Privacy and security

The API key is encrypted using hardware-backed Android Keystore when supported
by the device. Audio and text chosen for transformation are sent directly from
your phone to OpenAI. See
[PRIVACY.md](PRIVACY.md) and [SECURITY.md](SECURITY.md) for details.

## License

MIT
