# Production operations

This document describes the current safe operating boundary for MatchCats.

## Before starting the app

1. Set a strong `DB_PASSWORD` outside the repository.
2. Set `APP_CORS_ORIGINS` to the exact HTTPS origins that may call the API.
3. Set `COOKIE_SECURE=true` when HTTPS is active.
4. Keep `FORWARD_HEADERS_STRATEGY=framework` behind a trusted HTTPS reverse proxy.
5. Configure `PUBLIC_BASE_URL` with an HTTPS URL before enabling email delivery.
6. Keep SMTP credentials and moderator account IDs in environment configuration, never in commits.

## Health checks

- `GET /health` confirms that the application process started.
- `GET /health/ready` confirms that the application can query PostgreSQL.
- A deployment should receive traffic only after `/health/ready` returns HTTP 200 with `status=UP` and `database=UP`.

## Restarting

The application uses graceful shutdown with a configurable `SHUTDOWN_TIMEOUT` (20 seconds by default). A controlled restart lets in-flight requests finish before the process exits. If readiness fails after a restart, keep the previous instance available and inspect the application and database logs before retrying.

## Backups and recovery

The current repository includes development snapshot and restore checks. Production still needs encrypted off-site PostgreSQL backups, a retention policy, access restrictions, restore drills with populated documents/photos, and a documented recovery time objective. A file copy of the running database is not a production backup.

## Current limitations

Real SMTP delivery, mobile/OS push notifications, signed Windows/Android/iOS packages, production hosting and store accounts are not configured in this development release.

