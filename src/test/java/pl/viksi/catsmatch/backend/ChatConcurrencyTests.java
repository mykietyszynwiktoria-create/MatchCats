package pl.viksi.catsmatch.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import pl.viksi.catsmatch.backend.account.AccountService;
import pl.viksi.catsmatch.backend.cats.*;
import pl.viksi.catsmatch.backend.chat.ChatService;
import pl.viksi.catsmatch.backend.matching.MatchingService;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
class ChatConcurrencyTests {
    @Autowired AccountService accounts;
    @Autowired CatService cats;
    @Autowired ChatService chats;
    @Autowired MatchingService matches;
    @Autowired JdbcTemplate database;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired pl.viksi.catsmatch.backend.account.AccountRepository accountRepository;

    @Test void deletingCandidateAccountWhileSelectingPairDoesNotDeadlock() throws Exception {
        List<Integer> createdAccounts = new ArrayList<>();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Authentication[] owners = new Authentication[2];
            int[] catIds = new int[2];
            String prefix = "deleterace" + UUID.randomUUID().toString().substring(0, 8);
            for (int i = 0; i < 2; i++) {
                String username = prefix + i;
                createdAccounts.add(accounts.register(new AccountService.Registration(username,
                    "StrongTestPassword!", username + "@example.test", "Test", "Breeder")).id());
                owners[i] = UsernamePasswordAuthenticationToken.authenticated(username, null, List.of());
                cats.saveBreeder(owners[i], new CatService.BreederInput("Blue Cats", "Warsaw", "Poland", ""));
                catIds[i] = cats.create(owners[i], new CatService.CatInput("Luna", "Maine Coon",
                    i == 0 ? Cat.Sex.FEMALE : Cat.Sex.MALE, Cat.Health.HEALTHY,
                    LocalDate.of(2022, 1, 1), "Warsaw", "Poland", "", true)).id();
            }
            var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            Future<Integer> selection = transaction.execute(status -> {
                accountRepository.lockAccounts(List.of(createdAccounts.get(1)));
                Future<Integer> pending = worker.submit(() -> {
                    try { matches.select(catIds[0], owners[0], new MatchingService.Selection(catIds[1])); return 200; }
                    catch (pl.viksi.catsmatch.backend.common.ApiException error) { return error.status.value(); }
                });
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                boolean waiting = false;
                while (System.nanoTime() < deadline) {
                    database.execute("select pg_stat_clear_snapshot()");
                    waiting = Boolean.TRUE.equals(database.queryForObject("""
                        select exists(select 1 from pg_stat_activity
                        where datname = current_database() and pid <> pg_backend_pid()
                        and wait_event_type = 'Lock' and query like '%mc_accounts%')
                        """, Boolean.class));
                    if (waiting) break;
                    try { Thread.sleep(20); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
                }
                assertTrue(waiting, "Pair selection must be waiting on the candidate account lock");
                accounts.delete(owners[1], new AccountService.DeleteAccount("StrongTestPassword!"));
                return pending;
            });
            assertEquals(404, selection.get(20, TimeUnit.SECONDS));
            assertEquals(0, matches.list(catIds[0], owners[0], 0, 20).total());
        } finally {
            worker.shutdownNow();
            worker.awaitTermination(20, TimeUnit.SECONDS);
            for (int id : createdAccounts) database.update("delete from mc_accounts where id = ?", id);
        }
    }

    @Test void simultaneousContactInOppositeDirectionsCreatesOnlyOneConversation() throws Exception {
        simultaneousSelection(false);
    }

    @Test void simultaneousPairSelectionAndRecipientDecisionsAreSerialized() throws Exception {
        simultaneousSelection(true);
    }

    private void simultaneousSelection(boolean selectPair) throws Exception {
        List<Integer> createdAccounts = new ArrayList<>();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Authentication[] owners = new Authentication[2];
            int[] catIds = new int[2];
            String prefix = "race" + UUID.randomUUID().toString().substring(0, 8);
            for (int i = 0; i < 2; i++) {
                String username = prefix + i;
                createdAccounts.add(accounts.register(new AccountService.Registration(username,
                    "StrongTestPassword!", username + "@example.test", "Test", "Breeder")).id());
                owners[i] = UsernamePasswordAuthenticationToken.authenticated(username, null, List.of());
                cats.saveBreeder(owners[i], new CatService.BreederInput("Blue Cats", "Warsaw", "Poland", ""));
                catIds[i] = cats.create(owners[i], new CatService.CatInput("Luna", "Maine Coon",
                    i == 0 ? Cat.Sex.FEMALE : Cat.Sex.MALE, Cat.Health.HEALTHY,
                    LocalDate.of(2022, 1, 1), "Warsaw", "Poland", "", true)).id();
            }
            CountDownLatch start = new CountDownLatch(1);
            Future<Long> first = workers.submit(() -> {
                start.await();
                return selectPair ? matches.select(catIds[0], owners[0], new MatchingService.Selection(catIds[1])).id()
                    : chats.contact(catIds[1], owners[0]).id();
            });
            Future<Long> second = workers.submit(() -> {
                start.await();
                return selectPair ? matches.select(catIds[1], owners[1], new MatchingService.Selection(catIds[0])).id()
                    : chats.contact(catIds[0], owners[1]).id();
            });
            start.countDown();
            assertEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1, chats.list(owners[0], 0, 20).total());
            assertEquals(1, chats.list(owners[1], 0, 20).total());
            if (selectPair) {
                assertEquals(1, matches.list(catIds[0], owners[0], 0, 20).total());
                assertEquals(1, matches.list(catIds[1], owners[1], 0, 20).total());
                var pair = matches.list(catIds[0], owners[0], 0, 20).items().getFirst();
                assertEquals(pl.viksi.catsmatch.backend.matching.CatPair.Status.PENDING, pair.status());
                Authentication receiver = owners[pair.proposedBy().equals(createdAccounts.get(0)) ? 1 : 0];
                CountDownLatch decide = new CountDownLatch(1);
                List<Future<Integer>> decisions = new ArrayList<>();
                for (var action : List.of(MatchingService.Action.ACCEPT, MatchingService.Action.DECLINE)) {
                    decisions.add(workers.submit(() -> {
                        decide.await();
                        try { matches.decide(pair.id(), receiver, new MatchingService.Decision(action)); return 200; }
                        catch (pl.viksi.catsmatch.backend.common.ApiException error) { return error.status.value(); }
                    }));
                }
                decide.countDown();
                List<Integer> results = new ArrayList<>();
                for (var decision : decisions) results.add(decision.get(20, TimeUnit.SECONDS));
                Collections.sort(results);
                assertEquals(List.of(200, 409), results, "Only one competing response may commit");
            }
        } finally {
            workers.shutdownNow();
            workers.awaitTermination(20, TimeUnit.SECONDS);
            // Only this test's explicitly recorded accounts are removed.
            for (int id : createdAccounts) database.update("delete from mc_accounts where id = ?", id);
        }
    }
}
