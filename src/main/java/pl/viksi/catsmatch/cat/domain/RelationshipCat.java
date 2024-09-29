package pl.viksi.catsmatch.cat.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

@Entity
@Table(name = "relationshipscats")
public class RelationshipCat {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public Integer relationshipCatsId;
    int firstCatId;
    int secondCatId;
    int possibleChatId;

}
