# Security

Please report vulnerabilities privately through GitHub Security Advisories.
Do not include API keys, recordings, or other sensitive data in public issues.

OpenWispr intentionally never logs authorization headers, API keys, audio,
transcript contents, transformation instructions, or focused-field text.
Release signing material is expected to live only in GitHub Actions secrets.

Transcript history is stored only in the app-private SQLite database. Android
backup remains disabled. History search results are kept in memory, and AI
history search requests use the OpenAI Responses API with storage disabled.

Completed text is copied to the Android system clipboard only when it cannot be
inserted into the original field. The clipboard item is marked sensitive so
Android can conceal its preview; users should still treat the system clipboard
as shared device state.
