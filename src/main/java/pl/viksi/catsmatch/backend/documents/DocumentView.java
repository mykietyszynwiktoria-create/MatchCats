package pl.viksi.catsmatch.backend.documents;

import java.time.Instant;

public record DocumentView(Long id, Integer catId, String filename, String mediaType,
    CatDocument.Kind kind, CatDocument.Visibility visibility, int bytes, Instant createdAt,
    CatDocument.VerificationStatus verification, Instant verificationRequestedAt, Instant verificationReviewedAt,
    String verificationNote) {
}
