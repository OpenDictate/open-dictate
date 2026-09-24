# Privacy

OpenDictate does not operate a backend and does not include analytics or ads.

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
- A transformation may return a short answer or error as a private Android
  notification. OpenDictate does not persist that message; Android notification
  history is controlled by the device's system settings.
- Android backup is disabled for the application.

OpenAI processes API requests under its API data usage policies. Review those
policies before using the application with sensitive information.
