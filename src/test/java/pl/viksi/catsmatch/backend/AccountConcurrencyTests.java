package pl.viksi.catsmatch.backend;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pl.viksi.catsmatch.backend.account.*;
import pl.viksi.catsmatch.backend.common.ApiException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
class AccountConcurrencyTests {
    @Autowired AccountService service;
    @Autowired AccountRepository accounts;
    @Autowired JdbcTemplate db;
    @Autowired PlatformTransactionManager transactions;
    @Autowired PasswordEncoder encoder;

    @ParameterizedTest @ValueSource(strings = {"password", "email", "delete"})
    void waitingSensitiveMutationMustVerifyTheLatestPassword(String operation) throws Exception {
        String username = "passwordrace" + UUID.randomUUID().toString().substring(0, 8);
        String oldPassword = "OldStrongPassword!", newPassword = "NewStrongPassword!";
        int id = service.register(new AccountService.Registration(username, oldPassword,
            username + "@example.test", "Test", "Breeder")).id();
        var auth = UsernamePasswordAuthenticationToken.authenticated(username, null, List.of());
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> pending = new TransactionTemplate(transactions).execute(status -> {
                Account locked = accounts.lockByUsername(username).orElseThrow();
                Future<Integer> change = worker.submit(() -> {
                    try {
                        switch (operation) {
                            case "password" -> service.changePassword(auth, new AccountService.PasswordChange(oldPassword, "CompetingPassword!"));
                            case "email" -> service.update(auth, new AccountService.ProfileInput("changed-" + username + "@example.test", "Test", "Breeder", oldPassword));
                            case "delete" -> service.delete(auth, new AccountService.DeleteAccount(oldPassword));
                            default -> throw new IllegalArgumentException(operation);
                        }
                        return 200;
                    } catch (ApiException error) { return error.status.value(); }
                });
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                boolean waiting = false;
                while (System.nanoTime() < deadline) {
                    db.execute("select pg_stat_clear_snapshot()");
                    waiting = Boolean.TRUE.equals(db.queryForObject("""
                        select exists(select 1 from pg_stat_activity
                        where datname = current_database() and pid <> pg_backend_pid()
                        and wait_event_type = 'Lock' and query like '%mc_accounts%')
                        """, Boolean.class));
                    if (waiting) break;
                    try { Thread.sleep(20); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
                }
                assertTrue(waiting, "The competing change must be waiting on the account lock");
                service.replacePassword(locked, newPassword);
                return change;
            });
            assertEquals(400, pending.get(20, TimeUnit.SECONDS), "An old password must not authorize a change after waiting");
            Account current = accounts.findById(id).orElseThrow();
            assertTrue(encoder.matches(newPassword, current.passwordHash));
            assertEquals(1, current.securityVersion);
            assertEquals(username + "@example.test", current.email);
        } finally {
            worker.shutdownNow();
            worker.awaitTermination(20, TimeUnit.SECONDS);
            db.update("delete from mc_accounts where id = ?", id);
        }
    }
}
