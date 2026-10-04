# Document verification workflow

Uploading a pedigree, genetic test, award or health file never proves that the file is authentic. Every new file starts as `OWNER_UPLOADED` and remains private until the owner shares it with breeders.

The owner can request review with `POST /documents/{id}/verification-request`. The request is authenticated, protected by CSRF and allowed only for the cat owner. It changes the status to `REVIEW_REQUESTED`; repeating a pending request is idempotent. The interface explains that requesting review gives configured moderators access to this file, including private files. A rejected file can be requested again. A verified document cannot be requested again through this owner endpoint.

V14 stores the status, request/review timestamps and a note. The status is shown in both Polish and English in the document list. These labels describe the workflow; they are not a claim that MatchCats has independently confirmed a pedigree, laboratory result or veterinary decision. Moderator assignment and an evidence-based review procedure still need to be configured before public launch.

## Moderator queue and decisions

Access uses the existing `MODERATOR_ACCOUNT_IDS` allowlist and active-account checks. Ordinary roles do not grant review access. No real account is promoted automatically. Assign an accountable reviewer and establish issuer checks, response times and an appeal procedure before public launch.

- `GET /moderation/documents?page=0&size=20` returns up to 20 pending metadata records, excluding the reviewer's own files; no file content is returned.
- `GET /moderation/documents/{id}/download` permits private file access only while the file awaits review, and refuses the reviewer's own file. Downloads use attachment, `no-store` and `nosniff` headers. Existing hidden/suspended cat visibility rules apply.
- `POST /moderation/documents/{id}/decision` accepts `status` (`VERIFIED` or `REJECTED`), `requestedAt` (the request timestamp from the queue) and a nonblank `note` up to 1000 characters. It requires moderator access and CSRF, refuses self-review and returns updated metadata.

Owners see the current explanation; other breeders receive shared metadata and status without the note. Approval covers this file, not a guarantee of health or breeding suitability. Check the issuer and document the evidence supporting a decision without unnecessary personal data in owner-visible notes.

## Consistency and lifecycle

V15 adds audit records containing reviewer, request timestamp, decision time, outcome and explanation, plus a pending-queue index. Owner changes and decisions lock the cat before the document. Decisions compare the request timestamp and reject superseded or completed requests with `409 DOCUMENT_REVIEW_CHANGED`; reload the queue before retrying. Request timestamps use PostgreSQL-compatible microsecond precision.

Current state and audit insertion share a transaction. A unique constraint allows at most one decision per document/request. Previous decisions remain on resubmission. Deleting the document, cat or owning account cascades to its audit records. Deleting the reviewer clears its account reference in retained records. No separate review evidence uploads, public audit endpoint or automated appeal mechanism exist yet. Replacing a file means deleting the old file and uploading a new one, which starts unverified.

## Validation

Backend tests cover owner access, CSRF, invalid fields, private moderator downloads, owner-only explanations, duplicate/stale decisions, rejection/resubmission/approval, self-review prevention and audit cleanup. The isolated browser safety runner exercises the screens in Polish/English at 320, 390, 768 and 1440 pixel widths, with temporary accounts and the dedicated `matchcats_test` database. Real moderator accounts and production data are not used.
