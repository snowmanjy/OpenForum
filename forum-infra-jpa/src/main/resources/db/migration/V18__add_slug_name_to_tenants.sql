ALTER TABLE tenants ADD COLUMN slug VARCHAR(255);
ALTER TABLE tenants ADD COLUMN name VARCHAR(255);

-- Add unique constraint on slug per tenant? Wait, tenants table has ID. Slug should probably be unique globally or per some other scope?
-- The user said: "Note: Add a unique constraint on slug if possible, or handle duplicates in app logic."
-- Since Tenant ID is the primary key, and slug is likely used for URLs like /t/{slug}, it should be unique.
-- However, existing data might have nulls. But this is a new column.
-- I'll add the constraint.

ALTER TABLE tenants ADD CONSTRAINT uq_tenants_slug UNIQUE (slug);
