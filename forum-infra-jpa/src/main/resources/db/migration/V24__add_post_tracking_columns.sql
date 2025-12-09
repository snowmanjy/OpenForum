-- Add post_count to threads (Total replies + OP)
ALTER TABLE threads ADD COLUMN IF NOT EXISTS post_count INT DEFAULT 0;

-- Add post_number to posts (1-based index within the thread)
ALTER TABLE posts ADD COLUMN IF NOT EXISTS post_number INT;

-- Ensure thread_id FK exists in posts (already exists from V4, but good to be safe or skip)
-- V4 created: CONSTRAINT fk_posts_thread FOREIGN KEY (thread_id) REFERENCES threads(id)
-- So we generally don't need to re-add it, but we can verify or skip.

-- Data Migration: Initialize post_number for existing posts
-- We assign numbers based on creation time.
WITH enumerated_posts AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY thread_id ORDER BY created_at ASC) as rn
    FROM posts
)
UPDATE posts
SET post_number = enumerated_posts.rn
FROM enumerated_posts
WHERE posts.id = enumerated_posts.id;

-- Data Migration: Initialize post_count for existing threads
-- Count includes all posts for the thread.
WITH thread_counts AS (
    SELECT thread_id, COUNT(*) as cnt
    FROM posts
    GROUP BY thread_id
)
UPDATE threads
SET post_count = thread_counts.cnt
FROM thread_counts
WHERE threads.id = thread_counts.thread_id;
