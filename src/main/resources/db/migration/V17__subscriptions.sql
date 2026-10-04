CREATE TABLE mc_subscriptions (
    account_id INTEGER PRIMARY KEY REFERENCES mc_accounts(id) ON DELETE CASCADE,
    plan_id VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_period_end TIMESTAMPTZ,
    provider_customer_id VARCHAR(200),
    provider_subscription_id VARCHAR(200),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX mc_subscriptions_provider_subscription_idx
    ON mc_subscriptions(provider_subscription_id);

