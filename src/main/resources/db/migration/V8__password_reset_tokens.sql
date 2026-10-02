CREATE TABLE mc_password_resets (
    token_hash VARCHAR(64) PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES mc_accounts(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX mc_password_resets_account ON mc_password_resets(account_id);
