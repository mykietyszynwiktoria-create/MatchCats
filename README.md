# MatchCats

MatchCats helps pedigree cat breeders browse profiles and contact owners. The project contains a bilingual Sky Garden browser interface integrated with a Java/PostgreSQL backend. A separate local demonstration is available at /?demo=1.

## Current status

- Frontend: PL/EN registration, login/recovery, settings, cat profiles/photos, search, private conversations, pair proposals with recipient consent/withdrawal and document management connected to server APIs.

- Backend: session authentication, password changes/recovery, account deletion, login throttling, profiles, photos, matching, conversations and private/shared documents.

- Database: PostgreSQL with Flyway migrations. Existing legacy records are not automatically migrated.

- Safety: bidirectional contact blocks, private reports, restricted moderator review, account suspension/reinstatement and 90-day evidence retention. See [safety operations](docs/SAFETY.md). Moderator assignment is disabled until configured for an existing account.

- Outstanding: email-provider activation, email-address verification, actual moderator assignment and operations, production operations and Windows/Android/iOS store packages. In-app notifications and an installable web shell are now implemented; native store packaging remains. Billing is currently a safe FREE/PREMIUM foundation with checkout disabled until a provider is configured. Document uploads do not verify authenticity.

An owner-declared health status or profile is not independent verification of health, pedigree or breeding suitability.

## Start the development database

Requires Docker Desktop and Java 21. Commands below use PowerShell from the repository root.

```powershell

Copy-Item .env.example .env

# Edit .env and choose a local password before starting PostgreSQL.

docker compose up -d postgres

```

The database listens only on `127.0.0.1:5432`. This Compose configuration uses a new named PostgreSQL 16 volume, separate from the previous legacy configuration. It does not migrate or delete old databases. If port 5432 is already occupied, change the host port and use the same port in DB_URL.

Compose reads `.env` automatically; a standalone Java process does not. Set the matching values in your PowerShell session:

```powershell

$env:DB_URL='jdbc:postgresql://localhost:5432/matchcats'

$env:DB_USER='matchcats'

$env:DB_PASSWORD='<the password you put in .env>'

.\gradlew.bat bootRun

```

The API starts on port 8080. `GET /health` reports application startup. `GET /health/ready` additionally checks the PostgreSQL connection and returns 503 when the database is unavailable. See [backend API notes](docs/BACKEND.md) for session cookies, CSRF tokens, profile JSON and endpoint permissions. Billing endpoints and activation safeguards are documented in [billing readiness](docs/BILLING.md).

## Run tests and build

Use a separate test database. The following command creates it once in the local development server:

```powershell

docker compose exec postgres createdb -U matchcats matchcats_test

$env:TEST_DB_URL='jdbc:postgresql://localhost:5432/matchcats_test'

$env:TEST_DB_USER='matchcats'

$env:TEST_DB_PASSWORD='<the password you put in .env>'

.\gradlew.bat test build

```

If the test database already exists, skip its creation. Never point test configuration at a production database. Reports are generated in `build/reports/tests/test/index.html`; the executable server JAR is generated in `build/libs`. A server JAR is not a Windows installer, Android APK or iOS application.

GitHub Actions runs tests/build with Java 21 and a temporary PostgreSQL 16 database for backend-related pushes and pull requests. Test reports are available as workflow artifacts. The existing separate workflow mirrors GitHub commits to GitLab.

## Interface preview

See [frontend instructions](frontend/README.md). Normal application routes use authenticated server requests and PostgreSQL. The separate `/?demo=1` interface stores sample data in browser localStorage. The live interface maps form fields to backend contracts and sends record versions when updating cats.

## Implementation

New backend code is under `src/main/java/pl/viksi/catsmatch/backend`; old packages remain excluded from runtime scanning while features are replaced. Schema changes are under `src/main/resources/db/migration`. Integration tests are under `src/test/java/pl/viksi/catsmatch/backend`.

Published repository explanations and commit messages are English. Polish and English remain supported interface languages.


## Integrated application

Open http://localhost:8080/ after starting the application. The JAR serves its interface and API together. Set SERVER_ADDRESS explicitly when deploying; local development defaults to 127.0.0.1. For a Docker development setup, run `docker compose up --build` after configuring `.env`; the app and PostgreSQL host ports are bound to localhost.

See [Frontend integration](docs/FRONTEND_INTEGRATION.md) for browser scenarios, account lifecycle, SMTP settings and release boundaries. Store packages and a public deployment are not included in this development build.

Pair proposals now require an explicit recipient response. See [Pairing consent](docs/PAIRING_CONSENT.md) for migration, permissions and testing.

Conversation unread counts persist per account. Bilingual 404/403/network/server views use native cat illustrations; see [Unread messages and errors](docs/UNREAD_AND_ERRORS.md).

Inbox verification adds a private account status and single-use 24-hour links. See [Email verification](docs/EMAIL_VERIFICATION.md) for API permissions, SMTP requirements and testing limits.

A stopped local database/application snapshot can be checked in isolation with [Local backup restore](docs/LOCAL_BACKUP_RESTORE.md). This is a development check; production backup and recovery policies remain required.

Documents now have an explicit owner-uploaded/review-requested/verified/rejected workflow. See [Document verification](docs/DOCUMENT_VERIFICATION.md); upload alone never proves authenticity.



Installable app metadata and the static shell service worker are documented in [Installable app foundation](docs/INSTALLABLE_APP.md). Device checks are listed in [Device testing](docs/DEVICE_TESTING.md).


For Windows development, [tools/Start-MatchCats.ps1](tools/Start-MatchCats.ps1) checks Docker and Java, starts the local PostgreSQL container, and launches either `bootRun` or the test/build task. It does not install software or delete data.
