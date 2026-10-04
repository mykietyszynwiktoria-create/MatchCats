package pl.viksi.catsmatch.backend.documents;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentRepository extends JpaRepository<CatDocument, Long> {
    long countByCatId(Integer catId);

    @Query("""
        select new pl.viksi.catsmatch.backend.documents.DocumentView(
            d.id, d.catId, d.filename, d.mediaType, d.kind, d.visibility, d.bytes, d.createdAt,
            d.verificationStatus, d.verificationRequestedAt, d.verificationReviewedAt, d.verificationNote)
        from CatDocument d where d.catId = :cat and (:owner = true or d.visibility = :shared)
        order by d.id
        """)
    List<DocumentView> metadata(@Param("cat") Integer cat, @Param("owner") boolean owner,
        @Param("shared") CatDocument.Visibility shared);
}
