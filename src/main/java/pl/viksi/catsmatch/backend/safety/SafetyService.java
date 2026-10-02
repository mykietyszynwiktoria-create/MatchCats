package pl.viksi.catsmatch.backend.safety;

import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.viksi.catsmatch.backend.account.*;
import pl.viksi.catsmatch.backend.cats.*;
import pl.viksi.catsmatch.backend.chat.*;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.sql.*;
import java.time.Instant;
import java.util.*;

@Service
public class SafetyService {
    public enum Target { CAT, MESSAGE }
    public enum Reason { SPAM, HARASSMENT, FRAUD, OTHER }
    public enum Status { OPEN, RESOLVED, DISMISSED }
    public enum Action { NONE, SUSPEND_ACCOUNT }
    public record ReportInput(@NotNull Target targetType, @NotNull @Positive Long targetId,
                              @NotNull Reason reason, @NotBlank @Size(min=10,max=2000) String details) {}
    public record Decision(@NotNull Status status, @NotNull Action action,
                           @NotBlank @Size(max=2000) String note) {}
    public record Reinstatement(@NotBlank @Size(max=2000) String note) {}
    public record ReportView(long id, Integer reporterId, Integer reportedAccountId, Target targetType,
                             long targetId, Reason reason, String details, String snapshot,
                             Instant createdAt, Status status, String decisionNote) {}
    public record BlockView(int accountId, String label) {}
    public record SuspendedView(int accountId, String username) {}
    public record Capabilities(boolean moderator, boolean moderationConfigured) {}
    public record ContactState(boolean contactBlocked, boolean blockedByYou) {}
    private final JdbcTemplate db;
    private final AccountService service;
    private final AccountRepository accounts;
    private final CatRepository cats;
    private final MessageRepository messages;
    private final ConversationRepository conversations;
    private final Set<Integer> moderators;
    public SafetyService(JdbcTemplate db, AccountService service, AccountRepository accounts,
                         CatRepository cats, MessageRepository messages, ConversationRepository conversations,
                         @Value("${app.moderation.account-ids:}") String moderatorIds) {
        this.db=db; this.service=service; this.accounts=accounts; this.cats=cats;
        this.messages=messages; this.conversations=conversations;
        moderators=new HashSet<>();
        for(String id:moderatorIds.split(",")) if(!id.isBlank()) {
            int parsed=Integer.parseInt(id.strip());
            if(parsed<=0)throw new IllegalArgumentException("Moderator account IDs must be positive");
            moderators.add(parsed);
        }
    }
    public Capabilities capabilities(Authentication auth) {
        return new Capabilities(moderators.contains(service.current(auth).id), !moderators.isEmpty());
    }
    private int moderator(Authentication auth) {
        int id=service.current(auth).id;
        if(!moderators.contains(id))throw ApiException.forbidden();
        return id;
    }
    public boolean blocked(int first,int second) {
        return Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM mc_user_blocks WHERE (blocker_id=? AND blocked_id=?) OR (blocker_id=? AND blocked_id=?))",Boolean.class,first,second,second,first));
    }
    public void requireContact(int first,int second) {
        if(blocked(first,second))throw new ApiException(HttpStatus.FORBIDDEN,"CONTACT_BLOCKED","Contact is blocked");
        if(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM mc_accounts WHERE id IN (?,?) AND suspended)",Boolean.class,first,second)))
            throw new ApiException(HttpStatus.FORBIDDEN,"CONTACT_BLOCKED","Contact is unavailable");
    }
    public List<Integer> excludedContacts(int owner) {
        return db.queryForList("SELECT blocked_id AS id FROM mc_user_blocks WHERE blocker_id=? UNION SELECT blocker_id AS id FROM mc_user_blocks WHERE blocked_id=?",Integer.class,owner,owner);
    }
    public ContactState contactState(Authentication auth,int target) {
        int owner=service.current(auth).id;
        Account other=accounts.findById(target).orElseThrow(()->ApiException.missing("Account"));
        boolean own=db.queryForObject("SELECT EXISTS(SELECT 1 FROM mc_user_blocks WHERE blocker_id=? AND blocked_id=?)",Boolean.class,owner,target);
        return new ContactState(other.suspended || blocked(owner,target),own);
    }
    @Transactional public void block(Authentication auth,int target) {
        int owner=service.current(auth).id;
        if(owner==target)throw ApiException.invalid("You cannot block yourself");
        var locked=accounts.lockAccounts(List.of(owner,target));
        if(locked.size()!=2)throw ApiException.missing("Account");
        int count=db.queryForObject("SELECT count(*) FROM mc_user_blocks WHERE blocker_id=?",Integer.class,owner);
        if(count>=500 && !db.queryForObject("SELECT EXISTS(SELECT 1 FROM mc_user_blocks WHERE blocker_id=? AND blocked_id=?)",Boolean.class,owner,target))
            throw ApiException.invalid("The block list limit is 500");
        db.update("INSERT INTO mc_user_blocks(blocker_id,blocked_id) VALUES (?,?) ON CONFLICT DO NOTHING",owner,target);
    }
    @Transactional public void unblock(Authentication auth,int target) {
        int owner=service.current(auth).id;
        accounts.lockAccounts(List.of(owner,target));
        db.update("DELETE FROM mc_user_blocks WHERE blocker_id=? AND blocked_id=?",owner,target);
    }
    public List<BlockView> blocks(Authentication auth) {
        return db.query("SELECT b.blocked_id,COALESCE(p.kennel,a.username) AS label FROM mc_user_blocks b JOIN mc_accounts a ON a.id=b.blocked_id LEFT JOIN mc_breeders p ON p.id=a.id WHERE b.blocker_id=? ORDER BY b.created_at DESC",(rs,n)->new BlockView(rs.getInt(1),rs.getString(2)),service.current(auth).id);
    }
    @Transactional public ReportView report(Authentication auth,ReportInput input) {
        int reporter=service.current(auth).id;
        String details=input.details().strip();
        if(details.length()<10)throw ApiException.invalid("Describe the problem in at least 10 characters");
        int target;String snapshot;
        if(input.targetType()==Target.CAT) {
            if(input.targetId()>Integer.MAX_VALUE)throw ApiException.missing("Cat");
            Cat cat=cats.findById(input.targetId().intValue()).orElseThrow(()->ApiException.missing("Cat"));
            target=cat.ownerId;snapshot=cat.name+"\n"+cat.breed+"\n"+cat.description;
        } else {
            Message message=messages.findById(input.targetId()).orElseThrow(()->ApiException.missing("Message"));
            Conversation chat=conversations.findById(message.conversationId).orElseThrow(()->ApiException.missing("Conversation"));
            if(!chat.firstOwnerId.equals(reporter) && !chat.secondOwnerId.equals(reporter))throw ApiException.forbidden();
            target=message.authorId;snapshot=message.text;
        }
        if(target==reporter)throw ApiException.invalid("You cannot report your own content");
        accounts.lockAccounts(List.of(reporter,target));
        int count=db.queryForObject("SELECT count(*) FROM mc_safety_reports WHERE reporter_id=? AND created_at>CURRENT_TIMESTAMP-INTERVAL '1 day'",Integer.class,reporter);
        if(count>=20)throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"REPORT_LIMIT","At most 20 reports per day");
        long id=db.queryForObject("INSERT INTO mc_safety_reports(reporter_id,reported_account_id,target_type,target_id,reason,details,snapshot) VALUES (?,?,?,?,?,?,?) RETURNING id",Long.class,reporter,target,input.targetType().name(),input.targetId(),input.reason().name(),details,snapshot);
        return ownReport(id);
    }
    private ReportView map(ResultSet rs,boolean privateFields) throws SQLException {
        return new ReportView(rs.getLong("id"),privateFields?(Integer)rs.getObject("reporter_id"):null,
            privateFields?(Integer)rs.getObject("reported_account_id"):null,Target.valueOf(rs.getString("target_type")),rs.getLong("target_id"),
            Reason.valueOf(rs.getString("reason")),rs.getString("details"),privateFields?rs.getString("snapshot"):null,
            rs.getTimestamp("created_at").toInstant(),Status.valueOf(rs.getString("status")),privateFields?rs.getString("decision_note"):null);
    }
    private ReportView ownReport(long id) {return db.queryForObject("SELECT * FROM mc_safety_reports WHERE id=?",(rs,n)->map(rs,false),id);}
    private CatService.PageView<ReportView> reports(Integer owner,Status status,int page,int size,boolean privateFields) {
        if(page<0 || size<1 || size>50)throw ApiException.invalid("Invalid report pagination");
        String filter=owner!=null?"reporter_id=?":"status=?";
        Object value=owner!=null?owner:status.name();
        var items=db.query("SELECT * FROM mc_safety_reports WHERE "+filter+" ORDER BY id DESC LIMIT ? OFFSET ?",(rs,n)->map(rs,privateFields),value,size,(long)page*size);
        long total=db.queryForObject("SELECT count(*) FROM mc_safety_reports WHERE "+filter,Long.class,value);
        return new CatService.PageView<>(items,total,page,size);
    }
    public CatService.PageView<ReportView> ownReports(Authentication auth,int page,int size) {return reports(service.current(auth).id,null,page,size,false);}
    public CatService.PageView<ReportView> queue(Authentication auth,Status status,int page,int size) {moderator(auth);return reports(null,status,page,size,true);}
    @Transactional public void decide(Authentication auth,long id,Decision input) {
        int reviewer=moderator(auth);
        if(input.status()==Status.OPEN || (input.status()==Status.DISMISSED && input.action()!=Action.NONE))throw ApiException.invalid("Invalid decision");
        var found=db.query("SELECT * FROM mc_safety_reports WHERE id=?",(rs,n)->map(rs,true),id);
        if(found.isEmpty())throw ApiException.missing("Report");
        ReportView report=found.getFirst();
        // Account locks precede the report lock, matching other account operations.
        var ids=new ArrayList<Integer>();ids.add(reviewer);if(report.reportedAccountId()!=null)ids.add(report.reportedAccountId());
        var locked=accounts.lockAccounts(ids.stream().distinct().toList());
        ReportView current=db.queryForObject("SELECT * FROM mc_safety_reports WHERE id=? FOR UPDATE",(rs,n)->map(rs,true),id);
        if(current.status()!=Status.OPEN)throw new ApiException(HttpStatus.CONFLICT,"REPORT_REVIEWED","The report has already been reviewed");
        if(input.action()==Action.SUSPEND_ACCOUNT) {
            Integer target=current.reportedAccountId();
            if(target==null)throw ApiException.missing("Reported account");
            if(moderators.contains(target))throw ApiException.invalid("Moderator accounts require separate administrator review");
            Account account=locked.stream().filter(a->a.id.equals(target)).findFirst().orElseThrow(()->ApiException.missing("Account"));
            account.suspended=true;account.securityVersion++;accounts.saveAndFlush(account);
            db.update("INSERT INTO mc_moderation_actions(moderator_id,account_id,action,note) VALUES (?,?,'SUSPEND',?)",reviewer,target,input.note().strip());
        }
        db.update("UPDATE mc_safety_reports SET status=?,moderator_id=?,decision_note=?,moderated_at=CURRENT_TIMESTAMP WHERE id=?",input.status().name(),reviewer,input.note().strip(),id);
    }
    public List<SuspendedView> suspended(Authentication auth) {moderator(auth);return db.query("SELECT id,username FROM mc_accounts WHERE suspended ORDER BY id LIMIT 100",(rs,n)->new SuspendedView(rs.getInt(1),rs.getString(2)));}
    @Transactional public void reinstate(Authentication auth,int target,Reinstatement input) {
        int reviewer=moderator(auth);var locked=accounts.lockAccounts(List.of(reviewer,target));
        Account account=locked.stream().filter(a->a.id.equals(target)).findFirst().orElseThrow(()->ApiException.missing("Account"));
        account.suspended=false;account.securityVersion++;accounts.saveAndFlush(account);
        db.update("INSERT INTO mc_moderation_actions(moderator_id,account_id,action,note) VALUES (?,?,'REINSTATE',?)",reviewer,target,input.note().strip());
    }
}
