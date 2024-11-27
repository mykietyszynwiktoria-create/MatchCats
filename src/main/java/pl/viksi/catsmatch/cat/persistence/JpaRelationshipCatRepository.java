package pl.viksi.catsmatch.cat.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.viksi.catsmatch.cat.domain.RelationshipCat;

public interface JpaRelationshipCatRepository extends JpaRepository<RelationshipCat, Integer> {

    boolean existsByFirstCatIdAndSecondCatId(int firstCatId, int secondCatId);


}
