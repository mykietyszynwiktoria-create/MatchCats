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

## Private conversations

`POST /cats/{catId}/contact` opens or reuses one conversation between the signed-in breeder and an available cat's owner. The caller must have a breeder profile. Self-contact and unavailable cats return 400. Both directions use the same ordered owner pair. Account row locks serialize simultaneous creation, and PostgreSQL also enforces a unique pair.

`GET /chats` lists only the signed-in account's conversations. `GET /chats/{id}` and `GET /chats/{id}/messages` require participation; another account receives 403. Message history is ordered by ID ascending, with zero-based pages (default size 50, maximum 100). Conversation lists default to 20, newest ID first. All routes require authentication; writes also require CSRF.

`POST /chats/{id}/messages` accepts `{ "text": "Hello" }` and returns 201. Text is required, cannot be blank and has a 4000-character maximum before trimming. The server derives the author from the authenticated account; submitted author IDs are ignored. Messages are stored in PostgreSQL, not browser storage. Clients must render text as plain text, never untrusted HTML. Responses contain participant IDs, context cat ID, timestamps and message data, without account emails or password hashes.

Migration `V3__conversations_and_messages.sql` adds conversations/messages. Removing a cat clears the conversation's context reference and preserves its messages. This module does not implement real-time delivery, notifications, read receipts, blocking/reporting or attachments. Existing participants can continue an existing conversation even if a cat later becomes unavailable.

## Candidate filtering and selected pairs

`GET /cats/{sourceId}/candidates` is restricted to the source cat's owner. It returns available cats of the same case-insensitive breed and opposite sex, from other owners, with owner-declared HEALTHY status. The source must also be available and HEALTHY. This is profile filtering, not genetic or veterinary suitability assessment. Breed names remain user-entered text. Pagination is zero-based, default size 20, maximum 100; results use ascending IDs.

`POST /cats/{sourceId}/matches` accepts `{ "candidateId": 123 }`. The service checks source ownership and both cats' current eligibility, locks both cat rows in ID order, persists a unique normalized cat pair and opens/reuses its owners' private conversation. Success returns 200 for both first and repeated selections. A reverse selection reuses the same pair. The record represents a proposed pairing for discussion, not mutual consent or confirmed breeding. No message is sent automatically.

`GET /cats/{sourceId}/matches` lists previously selected pairs for the source owner, including `conversationId`. Only the two owners can see the corresponding pair through their own cat's route. Deleting either cat deletes its saved pair; its conversation and messages remain. Changing availability/breed later can make an old pair ineligible; stored pairs remain historical proposals and are not automatically approved. A repeat POST always rechecks eligibility.

Migration `V4__cat_pairs.sql` enforces ordered unique cat pairs. API tests verify filtering, persistence, reversed selections, access control, invalid input and deletion. A separate concurrency test verifies two simultaneous opposite-direction selections produce one pair and one conversation.

## Still outstanding

Verification of breeder documents, password recovery, account lifecycle, real-time chat and moderation, production deployment, and API integration with the prototype panels remain unfinished in the replacement backend. Store installers for Windows, Android and iOS are also not yet implemented.

## Cat documents

`POST /cats/{catId}/documents` accepts multipart form fields `file` and `kind`. Only the cat owner can upload. Kinds are `PEDIGREE`, `GENETIC_TEST`, `AWARD`, `HEALTH`, `OTHER`. New files are PRIVATE. Maximum size is 5 MiB and maximum count is 10 per cat; a cat row lock serializes quota checks. File bytes are stored in PostgreSQL BYTEA by migration V5; deletion of a cat cascades to its documents. No original filename is used as a disk path.

Allowed content signatures are PDF (`%PDF-`), PNG and JPEG. The server detects media type from these bytes, ignoring the submitted MIME type and extension. This is a signature check only, not a parser, malware scanner or certificate authentication. Empty files, unsupported signatures, filenames containing paths/control characters and filenames over 200 characters return 400. Oversize files return 413; exceeding the count returns 409 DOCUMENT_LIMIT. All uploaded documents are labelled OWNER_UPLOADED, never independently verified.

`GET /cats/{catId}/documents` returns metadata without file bytes. Owners see all their cat's documents; other signed-in accounts must have a breeder profile and see only BREEDERS documents. `PUT /documents/{id}/visibility` accepts `{ "visibility": "PRIVATE" }` or `BREEDERS` and requires cat ownership. BREEDERS means all signed-in accounts with a breeder profile, not selected chat participants; no anonymous public sharing is implemented. An owner can revoke access by changing back to PRIVATE. Previously downloaded copies cannot be recalled.

`GET /documents/{id}/download` returns bytes only to the owner or permitted breeders. The response uses attachment disposition with a server-generated filename, no-store caching and nosniff. `DELETE /documents/{id}` requires cat ownership and returns 204. Every write requires CSRF and every route requires authentication. Clients must display metadata as plain text. The document entity and its bytes are never returned as JSON.

Before production: add malware scanning or safe file processing, global storage quotas/rate limits, backup/retention handling and certificate verification procedures. Document upload does not prove pedigree, genetic suitability or veterinary fitness.

Verification on 2 October 2026: the complete Java 21 PostgreSQL test/build passed with 19 tests and zero failures/errors. Four document API tests cover persistence, metadata, PDF/PNG/JPEG signatures, owner/other-account access, sharing/revocation, CSRF, malformed input, size/count limits and deletion. A built-JAR check passed 26 real HTTP requests, including multipart uploads, byte-for-byte download, visibility changes and servlet-level oversize rejection. Its temporary accounts were removed from the isolated development database.

## Verification for the profile module

On 2 October 2026, Java 21 test/build passed against PostgreSQL 16: eight tests, zero failures/errors. A separate live HTTP check passed 17 requests covering account/session/CSRF setup, profile persistence, filters, stale-update conflict, deletion and logout. The local Docker Compose configuration also passed `docker compose config --quiet`; this validates its configuration, not a local Docker database startup.

## Verification for conversations and pair selection

On 2 October 2026, the complete Java 21 test/build passed against PostgreSQL 16: 15 tests, zero failures/errors. This includes two simultaneous-request tests for conversation and pair uniqueness. A separate built-JAR HTTP check passed 32 requests with real session cookies and CSRF tokens, covering registration/login, candidate listing, reversed pair selection, message exchange, third-party read/write denial, missing-CSRF denial, cat deletion and logout. Its three temporary accounts were removed from the isolated development database after the check. The prototype frontend was not connected or changed by this backend module.
