package pl.viksi.catsmatch.backend.matching;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "mc_cat_pairs")
public class CatPair {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "first_cat_id", nullable = false) public Integer firstCatId;
    @Column(name = "second_cat_id", nullable = false) public Integer secondCatId;
    @Column(name = "conversation_id", nullable = false) public Long conversationId;
    @Column(name = "created_at", nullable = false) public Instant createdAt;

    protected CatPair() {}
    public CatPair(int firstCatId, int secondCatId, long conversationId) {
        this.firstCatId = firstCatId;
        this.secondCatId = secondCatId;
        this.conversationId = conversationId;
        createdAt = Instant.now();
    }
}
