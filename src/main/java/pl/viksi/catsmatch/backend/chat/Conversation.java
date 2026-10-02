package pl.viksi.catsmatch.backend.chat;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mc_conversations")
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "first_owner_id", nullable = false)
    public Integer firstOwnerId;
    @Column(name = "second_owner_id", nullable = false)
    public Integer secondOwnerId;
    @Column(name = "context_cat_id")
    public Integer contextCatId;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    protected Conversation() {}
    public Conversation(int first, int second, int catId) {
        firstOwnerId = first;
        secondOwnerId = second;
        contextCatId = catId;
        createdAt = Instant.now();
    }
}
