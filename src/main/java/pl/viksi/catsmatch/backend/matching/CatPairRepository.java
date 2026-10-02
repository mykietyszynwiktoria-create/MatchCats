package pl.viksi.catsmatch.backend.matching;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CatPairRepository extends JpaRepository<CatPair, Long> {
    interface PairCats { Integer getFirstCatId(); Integer getSecondCatId(); }
    @Query("select p.firstCatId as firstCatId, p.secondCatId as secondCatId from CatPair p where p.id = :id")
    Optional<PairCats> catIds(@Param("id") Long id);
    Optional<CatPair> findByFirstCatIdAndSecondCatId(Integer first, Integer second);

    @Query("select p from CatPair p where p.firstCatId = :cat or p.secondCatId = :cat")
    Page<CatPair> belongingTo(@Param("cat") Integer cat, Pageable pageable);

    @Query("select p from CatPair p where p.firstCatId in (select c.id from Cat c where c.ownerId = :owner) or p.secondCatId in (select c.id from Cat c where c.ownerId = :owner)")
    Page<CatPair> belongingToOwner(@Param("owner") Integer owner, Pageable pageable);
}
