# Backend implementation progress

## Accounts and configuration

The original source compiled, but its only context test failed because CatRepository opened a hard-coded database connection during construction. The new backend uses Spring-managed repositories and environment-based database configuration. Legacy implementation packages are excluded from application scanning while their replacements are introduced and tested.

The backend now targets Java 21, Spring Boot 3.5.16 and Gradle 8.14.3. Database changes are versioned with Flyway. A fresh database is required; do not run these migrations over an existing legacy database without an explicit data migration plan. Legacy records have not been copied or deleted.

Account APIs use validated registration data, BCrypt password hashing, session authentication, CSRF protection and controlled JSON responses. Password hashes are excluded from response objects and logging. Authentication does not use a password stored in browser localStorage.

Clients first call GET /auth/csrf and send its token in the returned header for state-changing requests, including registration and login. Fetch a fresh CSRF token after login/logout. Keep the session cookie; send credentials with browser requests. Use HTTPS and COOKIE_SECURE=true when deploying.

This file is extended after each checked backend module. The prototype frontend is still separate until API integration is implemented.

## Breeder and cat profiles

The replacement backend now provides persisted breeder profiles and cat creation, reading, updating, deletion and filtered listing. All these routes require a signed-in session. State-changing requests also require a CSRF token.

| Method | Path | Purpose |
| --- | --- | --- |
| GET / PUT | `/owners/me` | Read or save the signed-in breeder's profile |
| GET | `/owners/{id}` | Read a breeder's public cattery information |
| GET | `/owners/me/cats` | List the signed-in breeder's cats |
| POST | `/cats` | Create a cat owned by the signed-in breeder |
| GET | `/cats` | Filter cats by breed, sex, city and availability |
| GET | `/cats/{id}` | Read one cat |
| PUT | `/cats/{id}` | Replace the owner's cat profile using its current version |
| DELETE | `/cats/{id}` | Delete the owner's cat |

Breeder fields are `kennel`, `city`, `country` and `bio`. Cat fields are `name`, `breed`, `sex`, `health`, `birthDate`, `city`, `country`, `description` and `available`. Sex values are `MALE` and `FEMALE`; health values are `UNKNOWN`, `HEALTHY` and `SICK`. Dates use `YYYY-MM-DD` and cannot be in the future. Names and locations cannot be blank. Required descriptions/bios can be empty strings, with a maximum of 2000 characters.

Create a breeder profile before creating a cat. Owner IDs are assigned from the authenticated account, never from submitted JSON. Other authenticated breeders can read a profile but cannot edit/delete its cats. Public breeder responses exclude account email, names and password hashes.

Listing returns `{items,total,page,size}`. Pages start at zero; the default size is 20 and the maximum is 100. Breed and city filters are case-insensitive exact matches after trimming, not partial text search or translation. Results are ordered by ID for predictable pagination. Races/breeds remain user-entered text at this stage; a shared bilingual breed catalogue is future work.

For an update, send `{ "version": 0, "profile": { ...all cat fields... } }` using the version returned by GET. `@Version` detects concurrent edits; a stale write returns `409 STALE_VERSION`. A successful update returns the incremented version. Input validation returns 400, missing resources 404, and forbidden edits 403.

As an application rule, an available profile must have owner-declared `HEALTHY` status; the same restriction is enforced in PostgreSQL. This is not independent veterinary verification, pedigree certification, or approval to breed. No automatic age/genetic suitability assessment is performed.

Flyway migration `V2__breeders_and_cats.sql` adds two tables and links cats to breeders and breeders to accounts. It does not read, change or migrate legacy tables. SQL queries use JPA bound parameters rather than concatenating submitted filter text.

Implementation order: entities describe stored data; migration defines database structure; repositories provide persistence; service applies ownership and business rules; controller exposes HTTP routes; integration tests exercise requests against PostgreSQL. See `Cat.java`, `Breeder.java`, `CatService.java`, `CatController.java` and `CatApiTests.java` under `backend/cats` and `src/test`.

## Running locally

Use Java 21 and an empty development PostgreSQL database. Set `DB_URL`, `DB_USER` and `DB_PASSWORD` for the database, then run `./gradlew bootRun` (Windows: `.\gradlew.bat bootRun`). The updated Docker Compose configuration starts PostgreSQL 16 on localhost using a separate named volume; follow the root README for .env and PowerShell setup.

For tests, use a separate empty database and set `TEST_DB_URL`, `TEST_DB_USER` and `TEST_DB_PASSWORD`. Run `./gradlew test build`. Defaults target the isolated local test database at port 55432; tests are not intended to run against production data. Test methods roll back their fixtures, while Flyway schema migrations remain applied to that test database.

## Still outstanding

Matching, persisted conversations, document uploads/permissions, password recovery, account lifecycle, production deployment, and API integration with the prototype panels remain unfinished in the replacement backend. Store installers for Windows, Android and iOS are also not yet implemented.

## Verification for the profile module

On 2 October 2026, Java 21 test/build passed against PostgreSQL 16: eight tests, zero failures/errors. A separate live HTTP check passed 17 requests covering account/session/CSRF setup, profile persistence, filters, stale-update conflict, deletion and logout. The local Docker Compose configuration also passed `docker compose config --quiet`; this validates its configuration, not a local Docker database startup.
