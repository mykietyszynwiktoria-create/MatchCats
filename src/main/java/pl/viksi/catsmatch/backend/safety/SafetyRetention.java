package pl.viksi.catsmatch.backend.safety;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @EnableScheduling
public class SafetyRetention {
    private final JdbcTemplate db;
    public SafetyRetention(JdbcTemplate db){this.db=db;}
    @Scheduled(initialDelay=60000,fixedDelay=3600000)
    @Transactional public void purge() {
        db.update("DELETE FROM mc_safety_reports WHERE created_at < CURRENT_TIMESTAMP-INTERVAL '90 days'");
        db.update("DELETE FROM mc_moderation_actions WHERE created_at < CURRENT_TIMESTAMP-INTERVAL '90 days'");
    }
}
