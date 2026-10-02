# MatchCats

MatchCats helps pedigree cat breeders browse profiles and contact owners. The project contains a bilingual Sky Garden browser interface integrated with a Java/PostgreSQL backend. A separate local demonstration is available at /?demo=1.

## Current status

- Frontend: PL/EN registration, login/recovery, settings, cat profiles/photos, search, private conversations, pairing proposals and document management connected to server APIs.

- Backend: session authentication, password changes/recovery, account deletion, login throttling, profiles, photos, matching, conversations and private/shared documents.

- Database: PostgreSQL with Flyway migrations. Existing legacy records are not automatically migrated.

- Outstanding: email-provider activation, email-address verification, moderation/reporting, notifications, production operations and Windows/Android/iOS store packages. Document uploads do not verify authenticity.

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

The API starts on port 8080. `GET /health` reports application startup; it is not a comprehensive readiness or database-health probe. See [backend API notes](docs/BACKEND.md) for session cookies, CSRF tokens, profile JSON and endpoint permissions.

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
