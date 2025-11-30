ALTER TABLE members ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_members_tenant_id ON members(tenant_id);
