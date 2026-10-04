package pl.viksi.catsmatch.backend.billing;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pl.viksi.catsmatch.backend.account.AccountService;

@Service
public class SubscriptionService {
    private final JdbcTemplate jdbc;
    private final AccountService accounts;

    public SubscriptionService(JdbcTemplate jdbc, AccountService accounts) {
        this.jdbc = jdbc;
        this.accounts = accounts;
    }

    public BillingController.AccountPlan current(Authentication authentication) {
        Integer accountId = accounts.current(authentication).id;
        try {
            return jdbc.queryForObject("SELECT plan_id, status FROM mc_subscriptions WHERE account_id = ?",
                (rs, row) -> new BillingController.AccountPlan(rs.getString("plan_id"),
                    "ACTIVE".equals(rs.getString("status")), rs.getString("status")), accountId);
        } catch (EmptyResultDataAccessException absent) {
            return new BillingController.AccountPlan("FREE", true, "ACTIVE");
        }
    }
}

