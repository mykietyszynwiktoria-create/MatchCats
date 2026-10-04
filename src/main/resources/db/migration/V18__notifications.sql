CREATE TABLE mc_notifications (
    id BIGSERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES mc_accounts(id) ON DELETE CASCADE,
    kind VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMPTZ
);

CREATE INDEX mc_notifications_account_created_idx
    ON mc_notifications(account_id, created_at DESC, id DESC);

