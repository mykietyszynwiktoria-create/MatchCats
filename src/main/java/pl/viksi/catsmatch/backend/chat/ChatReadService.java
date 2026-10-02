package pl.viksi.catsmatch.backend.chat;

import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.account.AccountService;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.util.*;

@Service
public class ChatReadService {
    public record ReadInput(@NotNull @Size(min=1,max=100) List<@NotNull @Positive Long> messageIds) {}
    public record UnreadView(long unreadMessages, long unreadConversations) {}
    private final JdbcTemplate db;
    private final AccountService accounts;
    public ChatReadService(JdbcTemplate db, AccountService accounts) { this.db=db; this.accounts=accounts; }

    public long unread(long chat, int reader) {
        return db.queryForObject("SELECT count(*) FROM mc_messages m LEFT JOIN mc_message_reads r ON r.message_id=m.id AND r.reader_id=? WHERE m.conversation_id=? AND m.author_id<>? AND r.message_id IS NULL", Long.class, reader,chat,reader);
    }

    @Transactional(readOnly=true)
    public UnreadView summary(Authentication auth) {
        int reader=accounts.current(auth).id;
        return db.queryForObject("SELECT count(*) AS messages,count(DISTINCT m.conversation_id) AS chats FROM mc_messages m JOIN mc_conversations c ON c.id=m.conversation_id LEFT JOIN mc_message_reads r ON r.message_id=m.id AND r.reader_id=? WHERE (c.first_owner_id=? OR c.second_owner_id=?) AND m.author_id<>? AND r.message_id IS NULL",
            (rs,n)->new UnreadView(rs.getLong("messages"),rs.getLong("chats")),reader,reader,reader,reader);
    }

    @Transactional
    public void mark(long chat, Authentication auth, ReadInput input) {
        int reader=accounts.current(auth).id;
        var participants=db.query("SELECT first_owner_id,second_owner_id FROM mc_conversations WHERE id=?",(rs,n)->List.of(rs.getInt(1),rs.getInt(2)),chat);
        if(participants.isEmpty())throw ApiException.missing("Conversation");
        if(!participants.getFirst().contains(reader))throw ApiException.forbidden();
        var ids=new LinkedHashSet<>(input.messageIds());
        String slots=String.join(",",Collections.nCopies(ids.size(),"?"));
        List<Object> parameters=new ArrayList<>(List.of(chat,reader));parameters.addAll(ids);
        long valid=db.queryForObject("SELECT count(*) FROM mc_messages WHERE conversation_id=? AND author_id<>? AND id IN ("+slots+")",Long.class,parameters.toArray());
        if(valid!=ids.size())throw ApiException.invalid("Only received messages from this conversation can be marked read");
        List<Object> inserts=new ArrayList<>(List.of(reader,chat,reader));inserts.addAll(ids);
        // Explicit message IDs protect unseen pages and messages arriving during a read request.
        db.update("INSERT INTO mc_message_reads(message_id,reader_id) SELECT id,? FROM mc_messages WHERE conversation_id=? AND author_id<>? AND id IN ("+slots+") ON CONFLICT DO NOTHING",inserts.toArray());
    }
}
