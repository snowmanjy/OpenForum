ALTER TABLE members DROP CONSTRAINT IF EXISTS members_external_id_key;
ALTER TABLE members ADD CONSTRAINT members_external_id_tenant_id_key UNIQUE (external_id, tenant_id);
