# Security Policy

## Secrets

Never commit model API keys, JWT secrets, database passwords or production URLs. The Android client must only call the Ktor backend; provider credentials belong in backend environment variables.

If a key is exposed in an issue, screenshot, terminal log or commit, revoke it at the provider immediately. Removing the text from a later commit does not invalidate the leaked credential.

## Reporting

For a public GitHub repository, report security issues through GitHub Private Vulnerability Reporting instead of a public issue. Include the affected endpoint, reproduction steps and expected impact; do not include live credentials or personal data.

## Production checklist

- Set a unique high-entropy `JWT_SECRET`.
- Use HTTPS and keep Release cleartext traffic disabled.
- Replace local audio storage with private object storage and signed URLs.
- Add upload type/size validation, malware scanning and content moderation.
- Move rate limiting to a shared store such as Redis for multi-instance deployment.
- Back up PostgreSQL and use versioned database migrations.
