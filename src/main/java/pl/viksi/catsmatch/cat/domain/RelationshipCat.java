package pl.viksi.catsmatch.cat.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "relationshipcats")
public class RelationshipCat {

    @GenericGenerator(name = "generator", strategy = "increment")
    @Id
    @GeneratedValue( generator = "generator")
    @Column(name = "relationshipcatsid")
    public Integer relationshipcatsid;

    @Column(name = "firstcatid")
    int firstCatId;

    @Column(name = "secondcatid")
    int secondCatId;



    public RelationshipCat(int firstCatId, int secondCatId) {
        this.firstCatId = firstCatId;
        this.secondCatId = secondCatId;
    }
}
