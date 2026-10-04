package pl.viksi.catsmatch.backend.documents;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocumentRepository extends JpaRepository<CatDocument, Long> {
    long countByCatId(Integer catId);

    @Query("select d.catId from CatDocument d where d.id = :id")
    Optional<Integer> catId(@Param("id") long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from CatDocument d where d.id = :id")
    Optional<CatDocument> locked(@Param("id") long id);

    @Query("""
        select new pl.viksi.catsmatch.backend.documents.DocumentView(
            d.id, d.catId, d.filename, d.mediaType, d.kind, d.visibility, d.bytes, d.createdAt,
            d.verificationStatus, d.verificationRequestedAt, d.verificationReviewedAt, d.verificationNote)
        from CatDocument d, Cat c where c.id = d.catId and c.ownerId <> :reviewer
            and d.verificationStatus = :status
        order by d.verificationRequestedAt, d.id
        """)
    Page<DocumentView> reviewQueue(@Param("reviewer") int reviewer,
        @Param("status") CatDocument.VerificationStatus status, Pageable pageable);

    @Query("""
        select new pl.viksi.catsmatch.backend.documents.DocumentView(
            d.id, d.catId, d.filename, d.mediaType, d.kind, d.visibility, d.bytes, d.createdAt,
            d.verificationStatus, d.verificationRequestedAt, d.verificationReviewedAt,
            case when :owner = true then d.verificationNote else null end)
        from CatDocument d where d.catId = :cat and (:owner = true or d.visibility = :shared)
        order by d.id
        """)
    List<DocumentView> metadata(@Param("cat") Integer cat, @Param("owner") boolean owner,
        @Param("shared") CatDocument.Visibility shared);
}
