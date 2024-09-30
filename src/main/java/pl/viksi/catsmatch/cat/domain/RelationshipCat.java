package pl.viksi.catsmatch.cat.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "relationshipcats")
public class RelationshipCat {

    @Id
    @SequenceGenerator(name = "stu_seq", sequenceName = "relationshipcats_seq", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stu_seq")
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
