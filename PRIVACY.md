# Privacy

OpenWispr does not operate a backend and does not include analytics or ads.

- Your OpenAI API key is encrypted locally with Android Keystore.
- Microphone audio is sent directly to OpenAI only while dictation is active.
- Temporary recordings used by GPT Transcribe are deleted after each request.
- Accessibility is used to find the focused editable control, detect the input
  method window, and update its text. When you explicitly start a text
  transformation, the selected text—or the focused field's text when there is
  no selection—and your spoken instruction are sent directly to OpenAI. Other
  screen contents are not collected, persisted, or transmitted.
- A transformation may return a short answer or error as a private Android
  notification. OpenWispr does not persist that message; Android notification
  history is controlled by the device's system settings.
- Android backup is disabled for the application.

OpenAI processes API requests under its API data usage policies. Review those
policies before using the application with sensitive information.
