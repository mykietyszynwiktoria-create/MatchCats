package pl.viksi.catsmatch.backend;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pl.viksi.catsmatch.backend.account.*;
import pl.viksi.catsmatch.backend.common.ApiException;
import org.mockito.ArgumentCaptor;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"app.mail.enabled=true","app.mail.from=test@example.test","app.public-base-url=http://localhost:8084"})
@ActiveProfiles("test")
class EmailVerificationConcurrencyTests {
    @Autowired AccountService service;
    @Autowired AccountRepository accounts;
    @Autowired EmailVerification verification;
    @Autowired EmailVerificationRepository tokens;
    @Autowired JdbcTemplate db;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean JavaMailSender mail;

    @ParameterizedTest @ValueSource(strings={"email", "verified", "suspended"})
    void waitingRequestMustUseCurrentAccountState(String change) throws Exception {
        String username="mailrace"+UUID.randomUUID().toString().substring(0,8);
        String oldEmail=username+"@example.test", newEmail="new-"+oldEmail;
        int id=service.register(new AccountService.Registration(username,"StrongPassword!",oldEmail,"Test","Breeder")).id();
        var auth=UsernamePasswordAuthenticationToken.authenticated(username,null,List.of());
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            Future<Integer> pending=new TransactionTemplate(transactions).execute(status->{
                Account account=accounts.lockByUsername(username).orElseThrow();
                Future<Integer> request=worker.submit(()->{
                    try { verification.request(auth,new EmailVerification.Request("en"));return 202; }
                    catch(ApiException error){return error.status.value();}
                });
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
                boolean waiting=false;
                while(System.nanoTime()<deadline){
                    db.execute("select pg_stat_clear_snapshot()");
                    waiting=Boolean.TRUE.equals(db.queryForObject("""
                        select exists(select 1 from pg_stat_activity
                        where datname=current_database() and pid<>pg_backend_pid()
                        and wait_event_type='Lock' and query like '%mc_accounts%')
                        """,Boolean.class));
                    if(waiting)break;
                    try{Thread.sleep(20);}catch(InterruptedException error){Thread.currentThread().interrupt();throw new IllegalStateException(error);}
                }
                assertTrue(waiting,"The verification request must wait for the account update");
                switch(change){
                    case "email" -> service.update(auth,new AccountService.ProfileInput(newEmail,"Test","Breeder","StrongPassword!"));
                    case "verified" -> {account.emailVerified=true;accounts.saveAndFlush(account);}
                    case "suspended" -> {account.suspended=true;accounts.saveAndFlush(account);}
                    default -> throw new IllegalArgumentException(change);
                }
                return request;
            });
            assertEquals(change.equals("suspended")?403:202,pending.get(20,TimeUnit.SECONDS));
            if(change.equals("email")){
                var capture=ArgumentCaptor.forClass(SimpleMailMessage.class);
                verify(mail).send(capture.capture());
                assertArrayEquals(new String[]{newEmail},capture.getValue().getTo());
                assertEquals(newEmail,tokens.findByAccountId(id).orElseThrow().email);
                assertEquals(newEmail,accounts.findById(id).orElseThrow().email);
            }else{
                verifyNoInteractions(mail);
                assertTrue(tokens.findByAccountId(id).isEmpty());
            }
        }finally{
            worker.shutdownNow();worker.awaitTermination(20,TimeUnit.SECONDS);
            db.update("delete from mc_accounts where id=?",id);
        }
    }
}
