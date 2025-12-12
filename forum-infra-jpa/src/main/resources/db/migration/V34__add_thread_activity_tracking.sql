-- Add last_activity_at column
ALTER TABLE threads ADD COLUMN last_activity_at TIMESTAMP;

-- Backfill with created_at for existing threads
UPDATE threads SET last_activity_at = created_at WHERE last_activity_at IS NULL;

-- Make column distinct not null after backfill
ALTER TABLE threads ALTER COLUMN last_activity_at SET NOT NULL;

-- Index for efficient archival queries
CREATE INDEX idx_threads_status_activity ON threads (status, last_activity_at);
