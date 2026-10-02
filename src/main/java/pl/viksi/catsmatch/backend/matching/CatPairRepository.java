package pl.viksi.catsmatch.backend.matching;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CatPairRepository extends JpaRepository<CatPair, Long> {
    Optional<CatPair> findByFirstCatIdAndSecondCatId(Integer first, Integer second);

    @Query("select p from CatPair p where p.firstCatId = :cat or p.secondCatId = :cat")
    Page<CatPair> belongingTo(@Param("cat") Integer cat, Pageable pageable);
}
