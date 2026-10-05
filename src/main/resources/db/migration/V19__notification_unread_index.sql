CREATE INDEX IF NOT EXISTS idx_mc_notifications_account_read
    ON mc_notifications(account_id, read_at);
