# Integrated breeder interface

The server JAR now contains the Sky Garden frontend. Open the server root URL (default `http://localhost:8080/`) after starting PostgreSQL and the application. The frontend and API share an origin; no separate static server is required. `/?demo=1` explicitly opens the older localStorage demonstration.

The default interface uses real session authentication and database APIs. Registration includes password confirmation and input validation. Login uses the username, not email. Invalid usernames and passwords share a generic error. Passwords and session credentials are not stored in localStorage. Login rotates the session and CSRF token; logout and expired-session handling return the interface to sign-in. PL/EN, accessible error alerts, disabled submit buttons and network timeouts are included.

After registration, save the cattery profile before adding cats or contacting breeders. The interface supports owned cat creation/editing/deletion, server filters and pagination, candidate proposals, persisted conversations and messages, account and breeder settings, and private/shared document uploads, downloads and deletion. Messages require manual refresh; they are not a real-time notification service.

## Validation

`frontend/tests/panels.cjs` checks the separate demonstration. `frontend/tests/live.cjs` exercises a built application with two temporary real accounts: registration, mismatched passwords, invalid credentials, cattery/cat persistence, private/shared documents, downloads, messages, safe text rendering, proposals, owner restrictions, logout, expired sessions, PL/EN, responsive auth forms and network failures.

Use a dedicated local database for the live test. It creates accounts beginning with `uie2e` and writes an account manifest to the OS temporary directory (override with `MATCHCATS_TEST_MANIFEST`). The current test removes only the two accounts it creates through the password-confirmed deletion API. It does not remove pre-existing data. `MATCHCATS_TEST_URL` defaults to `http://127.0.0.1:8084` and only local hosts are accepted. Run `npm run test:live` with the app running. `CHROME_PATH` can select an installed Chrome executable.

## Release boundaries

This is an integrated browser application, not a store package. Password recovery code, authenticated password changes, account deletion and revocation of earlier sessions are implemented. Email delivery is disabled until SMTP is configured. Email-address verification, moderator assignment/operations, notifications, operational monitoring/backups and production deployment remain release work. Store developer accounts, a production host/domain and an email provider have not been configured. Use HTTPS and secure cookies in production. Uploaded documents remain owner-supplied, independently unverified files.

## Photos and account safety

Owners can upload one JPEG/PNG photograph per cat (5 MiB, 24 megapixel input limit). The server decodes and re-encodes reduced JPEG pixels with a maximum edge of 1600, without original metadata. This does not currently normalize EXIF orientation; rotate incorrectly oriented photographs before uploading. Only authenticated users can read photos; only the owner can replace/delete them. Photo data is deleted with its cat.

Changing a password requires the current password and invalidates earlier sessions through an account security version. Changing the email also requires the current password and removes pending reset tokens. Account deletion requires the current password and an explicit UI confirmation. It deletes owned profiles, files and conversations through database cascades, including conversation history seen by the other participant. This is explained before confirmation.

Login is limited per remote address/username (10 attempts per 10 minutes) and per address (100 attempts per 10 minutes). The limiter is in-process; multiple production instances require a shared limiter and a correctly configured trusted proxy. No password is stored by the limiter.

## Password recovery email

Configure MAIL_ENABLED=true, MAIL_FROM, PUBLIC_BASE_URL, SMTP_HOST, SMTP_PORT, SMTP_USER and SMTP_PASSWORD on the server. The public base URL must use HTTPS, except localhost/127.0.0.1 development URLs. SMTP uses required STARTTLS by default with 5-second timeouts. Secrets belong in environment configuration, never commits. Docker Compose forwards these settings from .env.

POST /auth/password/request accepts email and language (pl/en). A configured service returns the same 202 response for known and unknown addresses. It sends a 15-minute link through the configured email provider for a known account. If delivery is unconfigured/unavailable, it returns EMAIL_UNAVAILABLE. POST /auth/password/reset accepts a single-use token and password, stores only token hashes and revokes existing sessions. CSRF is required for both routes. New requests replace older links; expired/consumed links are rejected. Tests use a mocked mail sender and do not send real emails; provider delivery still needs verification after configuration.

## Validation on 2 October 2026

The local Java 21/PostgreSQL build passes 37 backend tests. Live browser scenarios exercise two independent accounts, photos, documents, messages, proposals, password changes, deletion and failure states. Layout checks cover ten authenticated routes in PL/EN at 320, 390, 768 and 1440 pixels plus four account/recovery screens. Demo regression checks cover seven routes in both languages at the same widths. These are browser tests, not physical-device/native-store tests.

The Docker Compose configuration validates locally. Docker image build/start is not verified on this Windows host because its Docker daemon is unavailable. The JAR is built and running locally. GitHub CI includes backend and browser checks.

## Safety workflows

Contact blocks, private profile/message reports, moderator decisions, account suspension/reinstatement and 90-day evidence retention are implemented. See [Safety operations](SAFETY.md) for assignment, permissions, cleanup and the dedicated end-to-end test. No moderator has been assigned to the development application until the owner confirms an existing account.

## Mutual consent for proposals

The Proposals inbox supports recipient acceptance/decline and withdrawal by either participant after acceptance. Earlier saved pairs need an explicit proposal and never acquire automatic consent. See [Pairing consent](PAIRING_CONSENT.md) for state transitions, migration, access rules and validation.
