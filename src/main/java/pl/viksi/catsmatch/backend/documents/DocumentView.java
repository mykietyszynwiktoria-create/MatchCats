package pl.viksi.catsmatch.backend.documents;

import java.time.Instant;

public record DocumentView(Long id, Integer catId, String filename, String mediaType,
    CatDocument.Kind kind, CatDocument.Visibility visibility, int bytes, Instant createdAt) {
    // Uploading a file does not verify its issuer or contents.
    public String getVerification() { return "OWNER_UPLOADED"; }
}
