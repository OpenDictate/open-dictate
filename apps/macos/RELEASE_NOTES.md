OpenDictate for macOS **0.3.0** adds retranscription of your latest recording. Requires macOS 14+ on Apple Silicon or Intel.

- Choose **Retranscribe last recording** (Перетранскрибировать последнюю запись) in the menu bar to recognize the latest Live or Accurate recording again through Accurate, without recording new audio.
- Uses current Accurate settings, including model, languages, dictionary, punctuation correction and word replacements, without changing your selected dictation mode.
- Keeps the recording available after recognition errors and cancelled retries. Escape cancels processing; duplicate requests are blocked while busy.
- Inserts into the eligible field focused at invocation. If focus changes or no eligible field exists, the result is available to copy and in optional history.
- Retains only the latest ordinary dictation audio in memory until the next ordinary dictation or quitting. Cancelled recordings and voice-edit instructions are not retained for retry. Audio is never saved to disk or synced.

### Install

Download `OpenDictate-macOS-0.3.0-universal.dmg`, open it and drag OpenDictate to Applications. Quit an existing copy before replacing it. Existing settings and your Keychain API key are retained. Allow Microphone and Accessibility if prompted.

This release is **locally signed and not notarized**. If Gatekeeper blocks launching it, use **System Settings → Privacy & Security → Open Anyway** for OpenDictate, then launch it again. The disk image includes bilingual instructions.

Audio stays in memory and goes directly to OpenAI. There is no backend or analytics. No Input Monitoring permission is requested. API usage is billed separately by OpenAI. Check the `.sha256` attachment to verify the installer.
