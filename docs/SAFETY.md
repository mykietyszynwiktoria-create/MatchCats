# Safety and human moderation

The integrated interface supports contact blocks, reports of cat profiles or individual messages, a moderator queue, account suspension and reinstatement. These features do not independently verify breeders, pedigrees, genetic tests or uploaded documents.

## Breeder workflow

On another breeder's cat profile, choose **Block contact** or **Report profile**. In a conversation, report an individual received message or block the other participant. **Settings → Safety: reports and blocks** shows the user's block list and submitted report statuses.

A block prevents new messages, contact creation and pair proposals in both directions, including an existing saved pair. Each participant can remove only their own block. Existing conversation history and public cat profiles remain accessible so the reporting context is preserved. Candidate searches omit blocked contacts. A message send and block update acquire account locks in the same order, preventing a successful send after a committed block.

Reports require a reason and a 10–2000-character description. The server determines the reported account from the target, not from a submitted owner ID. Received-message reports require conversation membership; users cannot report their own content. Submitted reports do not automatically suspend accounts. The interface explicitly explains when no moderator has been assigned. Reports are limited to 20 per account per day; each account can maintain up to 500 outgoing blocks.

## Assign a moderator

Create the intended moderator account normally, then obtain its numeric ID from authenticated `GET /users/me`. Set `MODERATOR_ACCOUNT_IDS` on the server to that existing account ID, or a comma-separated list. Restart the application. In Docker development, configure the value in the uncommitted `.env` file; Compose forwards it to the application.

The default value is empty: no account has moderator privileges. Never guess account IDs or configure an ID before confirming its owner. Registration and account-update payloads cannot grant privileges, and a client-provided role does not override this server-side allowlist. After login, assigned accounts see the moderator link in Settings, even without a cattery profile. Do not enable a public service until actual people and a review/appeal process have been assigned.

## Review workflow

Moderators can view open, resolved or dismissed reports, including a snapshot of the reported text and the reporter's description. Ordinary breeders see only their own descriptions and statuses; they do not receive private moderator notes, report snapshots or another person's reports.

Each open report can be resolved without suspension, dismissed, or resolved with suspension of the reported account. A reason is mandatory. A second decision on the same report returns a conflict. Moderators cannot suspend accounts listed as moderators through this workflow; changing those privileges requires separate operator review.

Suspension retains account data, prevents login, invalidates existing sessions, hides cats from search/details, and stops new contact and messages. Existing conversations remain readable by the active participant. A moderator may reinstate an account with a recorded reason. Suspension and reinstatement create private audit entries with the moderator ID and time. The suspended-account screen currently lists at most 100 accounts; operators can reinstate a known ID through the protected API.

## Retention and deletion

Reports store snapshots of the reported profile text or message, not copies of uploaded files. Account deletion removes blocks and ordinary profile/conversation data; report and audit references become null while evidence may remain temporarily. This is disclosed in the report form and account-deletion screen. A scheduled hourly task deletes reports and moderation audit records older than 90 days, including open reports. This technical default must be reflected in the product's final privacy and operating policies. Backup retention and handling of appeals still need an operational policy before deployment.

## API

- `GET /safety/capabilities`: moderator capability and whether any moderator IDs are configured.
- `GET /safety/contact/{accountId}`: contact-block status and whether the requester owns a block.
- `GET /safety/blocks`, `PUT /safety/blocks/{accountId}`, `DELETE /safety/blocks/{accountId}`.
- `POST /safety/reports`: targetType (`CAT` or `MESSAGE`), targetId, reason (`SPAM`, `HARASSMENT`, `FRAUD`, `OTHER`), details.
- `GET /safety/reports`: requester-only, paginated results.
- `GET /moderation/reports`: protected queue, status filter and pagination.
- `POST /moderation/reports/{id}/decision`: status (`RESOLVED` or `DISMISSED`), action (`NONE` or `SUSPEND_ACCOUNT`), note.
- `GET /moderation/suspended` and `POST /moderation/accounts/{id}/reinstate` with a note.

All routes require authentication. Mutating requests also require CSRF protection. No moderator is assigned by installing the code alone.

## Validation

Six backend tests cover bidirectional blocks, saved-pair bypass attempts, ownership, CSRF, report privacy, forbidden moderator access, suspension/login restrictions, reinstatement/audit entries, limits and retention. The complete backend suite passes 37 tests.

`npm run test:safety` runs a separate application on localhost port 8085 against a database whose URL must end in `/matchcats_test`. It creates its own temporary moderator, restarts that isolated server with the moderator ID, exercises real PL/EN interfaces and restores suspended test users before cleanup. Browser checks cover reporting, escaped evidence, blocking/unblocking, denied moderator access, session revocation, reinstatement and responsive safety screens. Temporary report evidence remains in the test database until expiry; it is never created in the development database by this runner. Existing demo/live browser checks also remain in CI.
