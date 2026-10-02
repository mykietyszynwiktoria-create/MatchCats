# Pair proposals and mutual consent

A saved cat pair is now a proposal, initially `PENDING`. The other cat's owner must explicitly accept or decline it. Selecting the same pair in the opposite direction does not grant consent. The new **Proposals** navigation item lists incoming and outgoing proposals belonging to the authenticated breeder, with pagination and manual refresh.

## Transitions

- `PENDING` → `ACCEPTED` or `DECLINED`: recipient only.
- `PENDING` → `WITHDRAWN`: proposer only.
- `ACCEPTED` → `WITHDRAWN`: either participant.
- `DECLINED` and `WITHDRAWN` are final for the saved pair. Re-selecting reuses the record and conversation; it does not send a new invitation or reopen consent.

Acceptance represents agreement to further discussion and planning. It does not certify breed, pedigree, genetics, health or veterinary suitability. Existing conversations remain usable after a decision, subject to contact blocks and account suspension.

## Migration and permissions

`V11__pairing_consent.sql` adds status, proposer and decision time. Earlier saved pairs remain pending with no proposer; they cannot be accepted until an owner explicitly selects the candidate again. Historical one-sided selections are never converted into mutual consent.

The server obtains the requester from the session. Only current owners of the two cats may act. Only the recipient may accept; a hidden UI button is not the authorization check. Acceptance rechecks availability, owner-declared health, breed, sex and contact permissions. Blocking prevents acceptance; authorized decline/withdrawal remains possible. Cat deletion removes the saved pair through the existing cascade. This module is not a durable veterinary or breeding-contract audit.

Both cats are locked in ID order before the proposal state is loaded. Simultaneous opposite responses are serialized: the second sees the committed state and receives `409 PROPOSAL_CHANGED`. Mutation routes require authentication and CSRF. Client-provided capabilities are not trusted.

## API

- Existing `POST /cats/{sourceId}/matches` sends or reuses a proposal and conversation.
- Existing `GET /cats/{sourceId}/matches` lists proposals for an owned cat.
- `GET /matches?page=0&size=20` lists only proposals involving owned cats.
- `POST /matches/{id}/decision` accepts `{ "action": "ACCEPT" }`, `DECLINE` or `WITHDRAW`.

Responses include `status`, `proposedBy`, `decidedAt`, `canDecide` and `canWithdraw`, alongside existing pair/conversation fields. Null `proposedBy` indicates a migrated proposal requiring explicit sending. Lists accept size 1–100 and nonnegative pages. The UI displays 20 per page; older per-cat candidate screens retain their existing maximum-100 saved-proposal preview and link to the paginated inbox.

## Validation and boundaries

The backend suite has 40 tests, including sender/recipient/outsider permissions, CSRF, state persistence, legacy records, changed cat availability, blocked acceptance and simultaneous conflicting decisions against PostgreSQL. Earlier safety-test assertions now isolate their own report IDs/account rather than assuming the shared test database is empty.

`npm run test:live` now exercises actual proposal acceptance, withdrawal and decline with two temporary accounts, in addition to existing account/cat/document/chat scenarios. Layout checks cover ten authenticated routes in PL/EN at 320, 390, 768 and 1440 pixels. Tests delete only their explicitly created accounts. No email or push notifications are sent by this module; users refresh the proposal list manually. Native-device testing and production email, notifications, document verification and deployment remain release work.
