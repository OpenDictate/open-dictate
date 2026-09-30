# OpenDictate

Dictate into the app you're already using, on **Android and macOS**.
OpenDictate sends audio directly to OpenAI using your own API key. There is
no backend, analytics or account to create.

| | Android | macOS |
|---|---|---|
| Start / finish | Tap the keyboard overlay | **Option + Space** |
| Voice editing | Long-press → Edit text | **Option + Shift + Space** |
| Installation | Signed APK | Universal DMG, Apple Silicon + Intel |
| Requires | Android 8+ | macOS 14+ |
| Downloads | [Android releases](https://github.com/OpenDictate/open-dictate/releases/latest) | [macOS 0.1.0](https://github.com/OpenDictate/open-dictate/releases/tag/macos-v0.1.0) |

## Features

- **Live** (`gpt-live-transcribe`) inserts text as you speak.
- **Accurate** (`gpt-transcribe`) transcribes a completed recording.
- Voice-directed editing of the selection, or the whole field without a selection,
  using GPT-6 Luna or GPT-6 Sol.
- A transcription dictionary, local history, on-device search, and explicit AI search.
- English/Russian settings, automatic speech language detection and language hints.
- Focus guards prevent delayed text from being inserted in another field or app.
- Encrypted credentials: Android Keystore on Android, Keychain on macOS.

## Install on macOS

The next minor release is available as [0.2.0-rc.1](https://github.com/OpenDictate/open-dictate/releases/tag/macos-v0.2.0-rc.1),
with a compact recording indicator. The stable download remains 0.1.0.

1. Download the universal DMG from the [macOS release](https://github.com/OpenDictate/open-dictate/releases/tag/macos-v0.1.0).
2. Open it, drag **OpenDictate** into **Applications**, and eject the disk image.
3. Launch OpenDictate. This release is locally signed and **not notarized**.
   If macOS blocks launching it, choose **System Settings → Privacy & Security →
   Open Anyway** for OpenDictate, then launch it again.
4. Save your own OpenAI API key and allow **Microphone** and **Accessibility**
   with the setup buttons. Reopen the app if macOS requests it.
5. Focus a text field in another app. Press **Option + Space** to start and again
   to finish. **Escape** cancels. Add **Shift** to edit text with a spoken instruction.

OpenDictate lives in the menu bar. Its menu provides Settings, mode selection,
copy/paste of the latest transcript and adding selected text to the dictionary.
Shortcuts, excluded apps and launch at login are configurable.

macOS recordings stay in memory and are discarded after use; there is no
recording archive or audio retry. Completed transcripts remain available in
local history, which can be disabled or deleted. Editors without writable
Accessibility text use a final clipboard paste. Previous clipboard contents
are restored if unchanged. Secure fields are excluded.

API billing is separate from a ChatGPT subscription. A model must be available
to your OpenAI API project. See [macOS documentation](apps/macos/README.md).

## Install on Android

1. Download and install the latest signed APK from [Android Releases](https://github.com/OpenDictate/open-dictate/releases/latest).
2. Save your OpenAI API key, grant microphone access and enable the OpenDictate
   service in the system accessibility settings.
3. Open a text field and tap the overlay to dictate. Long-press for voice editing,
   the dictionary, last recording retry and paste-last actions.

The overlay appears while the keyboard and an editable field are present.
Swipe right to dismiss it until the keyboard closes. Add dictionary words from
Android's text selection or Share menu. Android retains one private recording
for retry; the next dictation replaces it.

Preview APKs install separately as `com.opendictate.app.preview`. Disable the
stable accessibility service before enabling the preview service.

## Repository and builds

```text
apps/android/   Android application, Gradle wrapper and tests
apps/macos/     Native SwiftUI/AppKit application, Swift package and tests
docs/          Public website and platform documentation
.github/       Platform verification and release workflows
```

Android requires JDK 17+ and Android SDK 37. The root wrapper forwards to
`apps/android` so existing commands keep working:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew lintRelease assembleRelease
```

APKs are under `apps/android/app/build/outputs/apk/`.
macOS requires Xcode 16+:

```bash
swift test --package-path apps/macos
apps/macos/scripts/package.sh
open dist/macos/OpenDictate.app
```

The packaging script builds both architectures, creates and verifies a signed
`.app`, and packages a DMG and SHA-256 checksum in `dist/macos/`.
Build output is ignored by Git. No third-party Swift dependencies are needed.

## Releases

- Android: `v*` tags publish signed APKs through `release.yml`. Signing uses
  `SIGNING_KEY`, `KEY_ALIAS`, `KEY_PASSWORD` and `STORE_PASSWORD` Actions secrets.
  Preview builds retain the weekly/manual `preview.yml` workflow.
- macOS: `macos-v*` tags publish universal DMGs and checksums through
  `macos-release.yml`. The version must match `apps/macos/VERSION`.
  macOS releases do not replace the latest Android download.
- Local macOS packaging also supports Developer ID signing and notarization
  when an identity and notary profile are supplied; see the platform README.

## Privacy and security

The only external service is OpenAI. Voice editing sends only the selected
text (or whole focused field) and the recorded instruction. AI history search
sends history only when explicitly requested. Unrelated screen contents are
not collected. See [PRIVACY.md](PRIVACY.md) and [SECURITY.md](SECURITY.md).

## License

MIT. See [third-party notices](THIRD_PARTY_NOTICES.md).
