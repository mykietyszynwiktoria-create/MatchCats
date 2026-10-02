# Integrated breeder interface

The server JAR now contains the Sky Garden frontend. Open the server root URL (default `http://localhost:8080/`) after starting PostgreSQL and the application. The frontend and API share an origin; no separate static server is required. `/?demo=1` explicitly opens the older localStorage demonstration.

The default interface uses real session authentication and database APIs. Registration includes password confirmation and input validation. Login uses the username, not email. Invalid usernames and passwords share a generic error. Passwords and session credentials are not stored in localStorage. Login rotates the session and CSRF token; logout and expired-session handling return the interface to sign-in. PL/EN, accessible error alerts, disabled submit buttons and network timeouts are included.

After registration, save the cattery profile before adding cats or contacting breeders. The interface supports owned cat creation/editing/deletion, server filters and pagination, candidate proposals, persisted conversations and messages, account and breeder settings, and private/shared document uploads, downloads and deletion. Messages require manual refresh; they are not a real-time notification service.

## Validation

`frontend/tests/panels.cjs` checks the separate demonstration. `frontend/tests/live.cjs` exercises a built application with two temporary real accounts: registration, mismatched passwords, invalid credentials, cattery/cat persistence, private/shared documents, downloads, messages, safe text rendering, proposals, owner restrictions, logout, expired sessions, PL/EN, responsive auth forms and network failures.

Use a dedicated local database for the live test. It creates accounts beginning with `uie2e` and writes an account manifest to the OS temporary directory (override with `MATCHCATS_TEST_MANIFEST`). It does not remove pre-existing data. `MATCHCATS_TEST_URL` defaults to `http://127.0.0.1:8084` and only local hosts are accepted. Run `npm run test:live` with the app running. `CHROME_PATH` can select an installed Chrome executable.

## Release boundaries

This is an integrated browser application, not a store package. Password recovery/email verification, account deletion, abuse reporting/moderation, notifications, operational monitoring/backups and production deployment remain release work. Store developer accounts, a production host/domain and an email provider have not been configured. Use HTTPS and secure cookies in production. Uploaded documents remain owner-supplied, independently unverified files.
