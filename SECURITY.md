# Security

Please report vulnerabilities privately through GitHub Security Advisories.
Do not include API keys, recordings, or other sensitive data in public issues.

OpenWispr intentionally never logs authorization headers, API keys, audio,
transcript contents, transformation instructions, or focused-field text.
Release signing material is expected to live only in GitHub Actions secrets.

Transcript history is stored only in the app-private SQLite database. Android
backup remains disabled. History search results are kept in memory, and AI
history search requests use the OpenAI Responses API with storage disabled.
