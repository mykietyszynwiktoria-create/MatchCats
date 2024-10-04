package pl.viksi.catsmatch.cat.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "relationshipcats")
public class RelationshipCat {

    @GenericGenerator(name = "generator", strategy = "increment")
    @Id
    @GeneratedValue( generator = "generator")
    @Column(name = "relationshipCatsId")
    public Integer relationshipCatsId;

    @Column(name = "firstcatid")
    int firstCatId;

    @Column(name = "secondcatid")
    int secondCatId;

    @Column(name = "possiblechatid")
    int possibleChatId;

    public RelationshipCat(int firstCatId, int secondCatId) {
        this.firstCatId = firstCatId;
        this.secondCatId = secondCatId;
    }
}
