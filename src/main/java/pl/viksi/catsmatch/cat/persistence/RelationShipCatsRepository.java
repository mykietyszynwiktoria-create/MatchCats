package pl.viksi.catsmatch.cat.persistence;

import org.hibernate.annotations.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RelationShipCatsRepository {

    @Autowired
    JpaRelationshipCatRepository jpaRelationshipCatRepository;

    public void createRelationship(int id, int matchedCatId) {

    }
}
