package pl.viksi.catsmatch.backend.chat;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mc_messages")
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "conversation_id", nullable = false)
    public Long conversationId;
    @Column(name = "author_id", nullable = false)
    public Integer authorId;
    @Column(nullable = false, length = 4000)
    public String text;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    protected Message() {}
    public Message(long conversationId, int authorId, String text) {
        this.conversationId = conversationId;
        this.authorId = authorId;
        this.text = text;
        createdAt = Instant.now();
    }
}
