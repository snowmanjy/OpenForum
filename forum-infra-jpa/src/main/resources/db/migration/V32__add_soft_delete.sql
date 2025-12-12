-- Add soft delete columns for posts
ALTER TABLE posts ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE posts ADD COLUMN deleted_at TIMESTAMP;

-- Add soft delete columns for threads
ALTER TABLE threads ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE threads ADD COLUMN deleted_at TIMESTAMP;

-- Index for efficient filtering of non-deleted content
CREATE INDEX idx_posts_deleted ON posts (deleted) WHERE deleted = FALSE;
CREATE INDEX idx_threads_deleted ON threads (deleted) WHERE deleted = FALSE;
