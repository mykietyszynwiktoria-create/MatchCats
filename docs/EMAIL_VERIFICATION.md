# Email verification

Email verification confirms control of an inbox. It does not verify breeder identity, pedigree, genetic certificates or veterinary suitability. Existing and new accounts start unverified; this development release does not gate other features on verification, so local testing remains possible without SMTP.

## API and persistence

- `POST /auth/email/request` requires authentication, CSRF and `{ "language": "pl" }` or `en`. Returns 202; sends only to the current account email. Already verified accounts return 202 without sending.
- `POST /auth/email/confirm` accepts `{ "token": "..." }`, requires CSRF and permits anonymous users. Returns 204 on success, without signing in or switching accounts. Invalid, expired, used, suspended-account and changed-address links return 400 `INVALID_VERIFICATION_LINK`.
- Private account responses include `emailVerified`. This is not a public breeder badge.
- V13 adds `mc_accounts.email_verified` and `mc_email_verifications`. Tokens use 32 cryptographically random bytes; only SHA-256 hashes are stored. A token binds the account and email for 24 hours. Resending replaces the prior token, with a database-backed minimum interval of 60 seconds per account. This interval alone is not a production-wide abuse or deliverability strategy.
- Confirmation, resends and email changes lock the account before token operations. Email changes require the current password, clear verification and delete outstanding links. Account deletion cascades token deletion. Expired rows remain bounded to one per account until replacement or deletion; they cannot be used.
- Mail exceptions roll back token replacement. SMTP acceptance and database commit are not an atomic delivery guarantee; a later transaction failure may leave an unusable emailed link. Request a new link in that case.

## Interface

Settings displays verified/unverified status and a send button. The `/#verify/<token>` route requires explicit confirmation, so merely opening a mail link does not consume it. It can be opened while signed out. Success refers to the account the link was sent for, which may differ from the current signed-in account. The form is removed after success. Expiry/reuse and delivery-unavailable messages are bilingual.

SMTP configuration is shared with password recovery: `MAIL_ENABLED`, `MAIL_FROM`, `PUBLIC_BASE_URL` and `SMTP_*`. Use HTTPS for deployed public URLs; HTTP is accepted only for localhost development. Do not commit SMTP credentials. With delivery disabled or missing, request returns 503 `EMAIL_UNAVAILABLE`, not a false success. A received valid link can still be confirmed when sending is disabled.

## Validation and release limits

Backend tests use a mock mail sender and PostgreSQL, covering hashing, single use, expiry, suspension, resend replacement/cooldown, email changes, authentication, CSRF and rollback on mail failure. Browser regression covers unavailable delivery, invalid links, Polish/English layouts at 320/390/768/1440 pixels and a mocked success response; the latter validates presentation, not real delivery. Opening the screen is checked not to submit confirmation automatically. Password recovery remains covered by its existing tests.

Real SMTP delivery, spam placement, bounce processing, production throttling and native-device/store release checks still require deployment configuration. No real email is sent by these automated tests.
