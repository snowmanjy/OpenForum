-- Add audit columns to tenants table
ALTER TABLE tenants ADD COLUMN created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE tenants ADD COLUMN last_modified_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE tenants ADD COLUMN created_by UUID;
ALTER TABLE tenants ADD COLUMN last_modified_by UUID;
