package pl.viksi.catsmatch.backend.notifications;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pl.viksi.catsmatch.backend.account.AccountService;
import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {
    public record NotificationView(long id, String kind, String title, String body, Instant createdAt, Instant readAt) {}
    public record UnreadCount(long count) {}
    private final JdbcTemplate jdbc;
    private final AccountService accounts;
    public NotificationService(JdbcTemplate jdbc, AccountService accounts) { this.jdbc=jdbc; this.accounts=accounts; }
    public void message(Integer recipientId) { jdbc.update("INSERT INTO mc_notifications(account_id, kind, title, body) VALUES (?, ?, ?, ?)", recipientId, "MESSAGE", "New message", "You received a new MatchCats message."); }
    public List<NotificationView> list(Authentication auth, int limit) {
        if (limit < 1 || limit > 100) throw new pl.viksi.catsmatch.backend.common.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Limit must be between 1 and 100");
        Integer accountId=accounts.current(auth).id;
        return jdbc.query("SELECT id, kind, title, body, created_at, read_at FROM mc_notifications WHERE account_id=? ORDER BY created_at DESC, id DESC LIMIT ?", (rs,row)->new NotificationView(rs.getLong("id"),rs.getString("kind"),rs.getString("title"),rs.getString("body"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("read_at")==null?null:rs.getTimestamp("read_at").toInstant()), accountId, limit);
    }
    public UnreadCount unreadCount(Authentication auth) {
        Integer accountId=accounts.current(auth).id;
        Long count=jdbc.queryForObject("SELECT COUNT(*) FROM mc_notifications WHERE account_id=? AND read_at IS NULL", Long.class, accountId);
        return new UnreadCount(count==null?0:count);
    }
    public void read(long id, Authentication auth) {
        Integer accountId=accounts.current(auth).id;
        int updated=jdbc.update("UPDATE mc_notifications SET read_at=COALESCE(read_at, CURRENT_TIMESTAMP) WHERE id=? AND account_id=?",id,accountId);
        if(updated==0)throw pl.viksi.catsmatch.backend.common.ApiException.missing("Notification");
    }
}

