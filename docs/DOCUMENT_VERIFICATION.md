# Document verification workflow

Uploading a pedigree, genetic test, award or health file never proves that the file is authentic. Every new file starts as `OWNER_UPLOADED` and remains private until the owner shares it with breeders.

The owner can request review with `POST /documents/{id}/verification-request`. The request is authenticated, protected by CSRF and allowed only for the cat owner. It changes the status to `REVIEW_REQUESTED`; repeating the same request is idempotent. A future moderator workflow can change the status to `VERIFIED` or `REJECTED` and attach a private note. A verified document cannot be requested again through this owner endpoint.

V14 stores the status, request/review timestamps and a note. The status is shown in both Polish and English in the document list. These labels describe the workflow; they are not a claim that MatchCats has independently confirmed a pedigree, laboratory result or veterinary decision. Moderator assignment and an evidence-based review procedure still need to be configured before public launch.

The backend test covers owner permissions, CSRF, idempotent requests and the fact that an upload is not automatically verified. A later release should add moderator review tests, audit entries, retention rules for review evidence and a way for an owner to appeal a rejection.
