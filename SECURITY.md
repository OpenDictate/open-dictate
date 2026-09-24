# Security

Please report vulnerabilities privately through GitHub Security Advisories.
Do not include API keys, recordings, or other sensitive data in public issues.

OpenDictate intentionally never logs authorization headers, API keys, audio,
transcript contents, transformation instructions, or focused-field text.
Model discovery calls OpenAI directly and stores only model IDs in app-private
preferences. The model list API does not supply endpoint capabilities or prices,
so the picker keeps the transcription aliases and up to five recent general GPT
text models, excluding Astra and specialized models.
Release signing material is expected to live only in GitHub Actions secrets.

Transcript history is stored only in the app-private SQLite database. Android
backup remains disabled. History search results are kept in memory, and AI
history search requests use the OpenAI Responses API with storage disabled.
The latest dictation audio is kept in one app-private WAV file and replaced by
the next dictation. It is excluded from Android backup with the rest of the app.

Completed text is copied to the Android system clipboard when it cannot be
inserted into the original field. Users can also explicitly copy an item from
history. The floating menu's Paste last action inserts text directly without
changing the clipboard. The fallback clipboard item is marked sensitive so
Android can conceal its preview; users should still treat the system clipboard
as shared device state.
