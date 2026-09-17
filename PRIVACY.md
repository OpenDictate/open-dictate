# Privacy

OpenWhispr does not operate a backend and does not include analytics or ads.

- Your OpenAI API key is encrypted locally with Android Keystore.
- Microphone audio is sent directly to OpenAI only while dictation is active.
- Temporary recordings used by GPT Transcribe are deleted after each request.
- Accessibility is used to find the focused editable control, detect the input
  method window, and insert the transcript. Screen contents are not collected,
  persisted, or transmitted.
- Android backup is disabled for the application.

OpenAI processes API requests under its API data usage policies. Review those
policies before using the application with sensitive information.

