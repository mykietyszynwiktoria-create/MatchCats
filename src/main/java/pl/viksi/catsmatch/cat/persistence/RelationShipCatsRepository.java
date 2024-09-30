package pl.viksi.catsmatch.cat.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.viksi.catsmatch.cat.domain.RelationshipCat;

@Component
public class RelationShipCatsRepository {

    @Autowired
    JpaRelationshipCatRepository jpaRelationshipCatRepository;

    public void createRelationship(int id, int matchedCatId) {
        var relationship = new RelationshipCat(id, matchedCatId);

        jpaRelationshipCatRepository.save(relationship);


    }
}
