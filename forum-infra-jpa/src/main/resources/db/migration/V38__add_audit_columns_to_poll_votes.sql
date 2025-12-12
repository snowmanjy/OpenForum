ALTER TABLE poll_votes ADD COLUMN last_modified_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE poll_votes ADD COLUMN created_by UUID;
ALTER TABLE poll_votes ADD COLUMN last_modified_by UUID;

UPDATE poll_votes SET created_by = voter_id;
UPDATE poll_votes SET last_modified_at = created_at WHERE last_modified_at IS NULL;
