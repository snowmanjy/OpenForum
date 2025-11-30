CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    target_id UUID NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_subscription_user_target UNIQUE (user_id, target_id)
);

CREATE INDEX IF NOT EXISTS idx_subscription_target ON subscriptions(target_id);
CREATE INDEX IF NOT EXISTS idx_subscription_user ON subscriptions(user_id);
