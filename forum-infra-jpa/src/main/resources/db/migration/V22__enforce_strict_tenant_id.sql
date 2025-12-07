-- Enforce Strict Multi-Tenancy: Every record MUST explicitly declare its tenant_id

-- Posts table: Drop default and ensure NOT NULL
ALTER TABLE posts ALTER COLUMN tenant_id DROP DEFAULT;

-- Threads table: Already has NOT NULL, but verify no default exists
-- (threads.tenant_id was defined as NOT NULL from V3, no action needed)

-- Safety: If any posts have 'default-tenant', this will be visible but we proceed.
-- In production, you would migrate these records first.
