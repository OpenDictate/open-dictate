# Privacy

OpenDictate does not operate a backend and does not include analytics or ads.

## macOS

- Your OpenAI API key is stored in a non-synchronizing, device-only Keychain
  item, available while your login keychain is unlocked. It is never saved in
  preferences, the app bundle, logs or release artifacts.
- The API key copy button places the entered key, or the saved Keychain key
  when the field is empty, on the system clipboard only when you click it.
  The clipboard item is marked confidential for compatible clipboard utilities.
  Other software on the Mac can still access clipboard data.
- Dictation sends microphone audio directly to OpenAI. Live streams 24 kHz
  PCM in memory; Accurate uploads a WAV constructed in memory. Audio is
  discarded after completion, failure or cancellation and is never written to disk.
- Microphone access is used only for a session you start. Accessibility reads
  the focused editable control and its selection, and inserts transcription
  there. Secure text fields are excluded; unrelated screen contents are not collected.
- Ordinary global shortcuts use registered hotkeys. When you choose Globe/Fn
  or a modifier-only shortcut, AppKit monitors key codes and modifier flags
  using the existing Accessibility permission. These monitors never read typed
  characters, log keystrokes or require Input Monitoring permission. Recording
  a shortcut reads its label only inside the focused settings control and ends
  when focus changes. Changing focus or editing the target
  prevents further automatic insertion. A completed result remains available
  to copy explicitly from the menu.
- Some editors require a final clipboard paste. In that case, only the
  transcript is placed on the clipboard; the previous clipboard contents are
  restored after insertion if no other app or user action changed them.
  Explicit copy keeps the transcript on the system clipboard. Other software
  on the Mac can access clipboard data.
- Voice editing sends only the selected text (or the whole focused field when
  nothing is selected) and your spoken instruction to OpenAI. Text Responses
  requests set `store: false`.
- The last 500 completed transcripts are stored locally in
  `~/Library/Application Support/OpenDictate/history.json`. You can disable
  saving future history and delete existing items or all history separately.
  The directory and file are owner-only. macOS backup software may include
  this folder according to your own backup configuration.
- Ordinary history search runs on-device. Only explicit AI search sends its
  query and saved transcript text directly to OpenAI. Search results are
  held in memory, and Responses requests set `store: false`.
- Dictionary terms, excluded app bundle identifiers and preferences stay in
  UserDefaults locally. Dictionary terms are sent to OpenAI as transcription
  hints only when you start a session. Exclusions are checked before capture.
- Launch at login is optional and uses macOS Login Items. OpenDictate does
  not install a background daemon or a network service.

## Android

- Your OpenAI API key is encrypted locally with Android Keystore.
- When the settings screen opens, OpenDictate may use that key to fetch the
  available model IDs directly from OpenAI. It caches only model IDs locally
  and checks again after 24 hours when the settings screen opens. You can also
  refresh the list manually.
- Microphone audio is sent directly to OpenAI during dictation or when you
  explicitly retry the latest recording.
- The latest dictation is kept as one WAV file in app-private storage, even if
  transcription fails. Starting another dictation replaces it. The floating
  menu's Last dictation action sends that audio to OpenAI again. Text
  transformation recordings remain temporary and are deleted after use.
- Successful dictation transcripts are stored in the application's private
  local database so you can review and delete them. They are not included in
  Android backups.
- Fuzzy history search runs entirely on the device. AI history search runs only
  when you explicitly tap its button and sends the search query and transcript
  text directly to OpenAI. OpenDictate sets API response storage to off for these
  requests and does not persist the response.
- If the original text field is no longer available when processing finishes,
  OpenDictate copies the completed result to the Android system clipboard so it
  is not lost. Clipboard access and retention are controlled by Android and
  other software on the device.
- You can explicitly copy a saved transcript from the history screen. The
  floating menu's Paste last action inserts into the focused field without
  changing the clipboard.
- Accessibility is used to find the focused editable control, detect the input
  method window, and update its text. When you explicitly start a text
  transformation, the selected text—or the focused field's text when there is
  no selection—and your spoken instruction are sent directly to OpenAI. Other
  screen contents are not collected, persisted, or transmitted.
- The app picker reads names and icons of launchable apps installed on the
  device. Your choice of apps where the dictation button is hidden is stored
  locally as package names and is not sent to OpenAI.
- A transformation may return a short answer or error as a private Android
  notification. OpenDictate does not persist that message; Android notification
  history is controlled by the device's system settings.
- Android backup is disabled for the application.
- When you choose Add to OpenDictate dictionary from Android's text selection
  menu, the floating button's long-press menu, or Android's Share menu, only
  the selected or shared text is saved in app-private preferences. The
  dictionary is sent directly to OpenAI as transcription context during later
  dictations.

OpenAI processes API requests under its API data usage policies. Review those
policies before using the application with sensitive information.
