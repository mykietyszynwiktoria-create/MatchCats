package pl.viksi.catsmatch.backend.documents;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "mc_documents")
public class CatDocument {
    public enum Kind { PEDIGREE, GENETIC_TEST, AWARD, HEALTH, OTHER }
    public enum Visibility { PRIVATE, BREEDERS }
    public enum VerificationStatus { OWNER_UPLOADED, REVIEW_REQUESTED, VERIFIED, REJECTED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "cat_id", nullable = false) public Integer catId;
    @Column(nullable = false, length = 200) public String filename;
    @Column(name = "media_type", nullable = false, length = 40) public String mediaType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public Kind kind;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public Visibility visibility;
    @Enumerated(EnumType.STRING) @Column(name="verification_status", nullable=false, length = 24)
    public VerificationStatus verificationStatus;
    @Column(name="verification_requested_at") public Instant verificationRequestedAt;
    @Column(name="verification_reviewed_at") public Instant verificationReviewedAt;
    @Column(name="verification_note", length=1000) public String verificationNote;
    @Column(nullable = false) public int bytes;
    @Column(nullable = false, columnDefinition = "bytea") public byte[] content;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    protected CatDocument() {}
    public CatDocument(int catId, String filename, String mediaType, Kind kind, byte[] content) {
        this.catId = catId;
        this.filename = filename;
        this.mediaType = mediaType;
        this.kind = kind;
        this.content = content;
        bytes = content.length;
        visibility = Visibility.PRIVATE;
        verificationStatus = VerificationStatus.OWNER_UPLOADED;
        createdAt = Instant.now();
    }
}
