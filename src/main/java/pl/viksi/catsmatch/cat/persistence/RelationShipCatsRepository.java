package pl.viksi.catsmatch.cat.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.cat.domain.RelationshipCat;

@Component
public class RelationShipCatsRepository {

    @Autowired
    JpaRelationshipCatRepository jpaRelationshipCatRepository;

    public void createRelationship(int idCat, int matchedCatId) {
        var relationship = new RelationshipCat(idCat, matchedCatId);

        jpaRelationshipCatRepository.save(relationship);


    }
}
