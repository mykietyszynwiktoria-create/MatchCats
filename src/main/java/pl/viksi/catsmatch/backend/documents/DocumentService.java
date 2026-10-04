package pl.viksi.catsmatch.backend.documents;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import pl.viksi.catsmatch.backend.safety.SafetyService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.viksi.catsmatch.backend.cats.*;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
public class DocumentService {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_FILES = 10;
    public record Sharing(@NotNull CatDocument.Visibility visibility) {}
    public record Decision(@NotNull CatDocument.VerificationStatus status,
        @NotNull Instant requestedAt, @NotBlank @Size(max=1000) String note) {}
    private final DocumentRepository documents;
    private final CatService cats;
    private final CatRepository catRepository;
    private final SafetyService safety;
    private final org.springframework.jdbc.core.JdbcTemplate db;

    public DocumentService(DocumentRepository documents, CatService cats, CatRepository catRepository, SafetyService safety,
        org.springframework.jdbc.core.JdbcTemplate db) {
        this.documents = documents;
        this.cats = cats;
        this.catRepository = catRepository;
        this.safety = safety;
        this.db = db;
    }

    private Cat lockedOwned(int catId, Authentication auth) {
        Cat cat = catRepository.lockCats(List.of(catId)).stream().findFirst()
            .orElseThrow(() -> ApiException.missing("Cat"));
        if (!cat.ownerId.equals(cats.userId(auth))) throw ApiException.forbidden();
        return cat;
    }

    @Transactional
    public DocumentView upload(int catId, Authentication auth, CatDocument.Kind kind, MultipartFile file) throws IOException {
        lockedOwned(catId, auth);
        if (file.isEmpty()) throw ApiException.invalid("Select a non-empty file");
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Maximum file size is 5 MiB");
        }
        if (documents.countByCatId(catId) >= MAX_FILES) {
            throw new ApiException(HttpStatus.CONFLICT, "DOCUMENT_LIMIT", "Maximum 10 documents per cat");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.length() > 200
            || filename.contains("/") || filename.contains("\\")
            || filename.chars().anyMatch(c -> c < 32 || c == 127)) {
            throw ApiException.invalid("Use a filename without paths or control characters, up to 200 characters");
        }
        byte[] content = file.getBytes();
        String mediaType = mediaType(content);
        return view(documents.saveAndFlush(new CatDocument(catId, filename.strip(), mediaType, kind, content)));
    }

    private String mediaType(byte[] content) {
        if (startsWith(content, "%PDF-".getBytes(StandardCharsets.US_ASCII))) return "application/pdf";
        if (startsWith(content, new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10})) return "image/png";
        if (startsWith(content, new byte[]{(byte)255, (byte)216, (byte)255})) return "image/jpeg";
        throw ApiException.invalid("Only files with PDF, PNG or JPEG signatures are accepted");
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length <= signature.length) return false;
        for (int i = 0; i < signature.length; i++) if (content[i] != signature[i]) return false;
        return true;
    }

    private CatDocument document(long id) {
        return documents.findById(id).orElseThrow(() -> ApiException.missing("Document"));
    }

    private CatDocument lockedDocument(long id) {
        int catId = documents.catId(id).orElseThrow(() -> ApiException.missing("Document"));
        catRepository.lockCats(List.of(catId));
        return documents.locked(id).orElseThrow(() -> ApiException.missing("Document"));
    }

    @Transactional(readOnly = true)
    public CatService.PageView<DocumentView> queue(Authentication auth, int page, int size) {
        int reviewer = safety.moderator(auth);
        if (page < 0 || size < 1 || size > 20) throw ApiException.invalid("Invalid page size or number");
        var result = documents.reviewQueue(reviewer, CatDocument.VerificationStatus.REVIEW_REQUESTED, PageRequest.of(page, size));
        return new CatService.PageView<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public CatDocument reviewDownload(long id, Authentication auth) {
        int reviewer = safety.moderator(auth);
        CatDocument d = document(id);
        if (cats.cat(d.catId).ownerId.equals(reviewer) || d.verificationStatus != CatDocument.VerificationStatus.REVIEW_REQUESTED)
            throw ApiException.forbidden();
        return d;
    }

    @Transactional
    public DocumentView decide(long id, Authentication auth, Decision input) {
        int reviewer = safety.moderator(auth);
        if (input.status() != CatDocument.VerificationStatus.VERIFIED && input.status() != CatDocument.VerificationStatus.REJECTED)
            throw ApiException.invalid("Choose VERIFIED or REJECTED");
        CatDocument d = lockedDocument(id);
        if (cats.cat(d.catId).ownerId.equals(reviewer)) throw ApiException.forbidden();
        if (d.verificationStatus != CatDocument.VerificationStatus.REVIEW_REQUESTED
            || !d.verificationRequestedAt.equals(input.requestedAt()))
            throw new ApiException(HttpStatus.CONFLICT, "DOCUMENT_REVIEW_CHANGED", "Reload the review queue before deciding");
        d.verificationStatus = input.status();
        d.verificationReviewedAt = Instant.now();
        d.verificationNote = input.note().strip();
        db.update("INSERT INTO mc_document_reviews(document_id,reviewer_id,requested_at,reviewed_at,status,note) VALUES (?,?,?,?,?,?)",
            d.id, reviewer, java.sql.Timestamp.from(d.verificationRequestedAt), java.sql.Timestamp.from(d.verificationReviewedAt),
            d.verificationStatus.name(), d.verificationNote);
        return view(documents.saveAndFlush(d));
    }

    @Transactional(readOnly = true)
    public List<DocumentView> list(int catId, Authentication auth) {
        Cat cat = cats.cat(catId);
        int owner = cats.userId(auth);
        boolean own = cat.ownerId.equals(owner);
        if (!own) cats.breeder(owner);
        return documents.metadata(catId, own, CatDocument.Visibility.BREEDERS);
    }

    @Transactional(readOnly = true)
    public CatDocument download(long id, Authentication auth) {
        CatDocument document = document(id);
        int owner = cats.userId(auth);
        if (!cats.cat(document.catId).ownerId.equals(owner)) {
            if (document.visibility != CatDocument.Visibility.BREEDERS) throw ApiException.forbidden();
            cats.breeder(owner);
        }
        return document;
    }

    @Transactional
    public DocumentView share(long id, Authentication auth, Sharing input) {
        CatDocument document = lockedDocument(id);
        cats.owned(document.catId, auth);
        document.visibility = input.visibility();
        return view(documents.saveAndFlush(document));
    }

    @Transactional
    public DocumentView requestVerification(long id, Authentication auth) {
        CatDocument document = lockedDocument(id);
        cats.owned(document.catId, auth);
        if (document.verificationStatus == CatDocument.VerificationStatus.VERIFIED)
            throw new ApiException(HttpStatus.CONFLICT, "DOCUMENT_ALREADY_VERIFIED", "This document is already verified");
        if (document.verificationStatus != CatDocument.VerificationStatus.REVIEW_REQUESTED) {
            document.verificationStatus = CatDocument.VerificationStatus.REVIEW_REQUESTED;
            document.verificationRequestedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            document.verificationReviewedAt = null;
            document.verificationNote = null;
        }
        return view(documents.saveAndFlush(document));
    }

    @Transactional
    public void delete(long id, Authentication auth) {
        CatDocument document = lockedDocument(id);
        cats.owned(document.catId, auth);
        documents.delete(document);
        documents.flush();
    }

    private DocumentView view(CatDocument d) {
        return new DocumentView(d.id, d.catId, d.filename, d.mediaType, d.kind, d.visibility, d.bytes, d.createdAt,
            d.verificationStatus, d.verificationRequestedAt, d.verificationReviewedAt, d.verificationNote);
    }
}
