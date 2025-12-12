DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='posts' AND column_name='reply_to_id') THEN
        -- Migrate reply_to_id to reply_to_post_id if data exists and reply_to_post_id is null
        UPDATE posts
        SET reply_to_post_id = reply_to_id
        WHERE reply_to_id IS NOT NULL 
          AND reply_to_post_id IS NULL;

        -- Drop the duplicate column
        ALTER TABLE posts DROP COLUMN reply_to_id;
    END IF;
END $$;

-- Ensure reply_to_post_id has foreign key constraint
-- Check if constraint exists first or just add it if missing (ignoring error if exists is hard in pure SQL without PL/pgSQL)
-- Assuming standard naming convention or re-adding it safe if we drop first.
-- For safety, let's just ensure index and constraint.
-- ALTER TABLE posts ADD CONSTRAINT fk_posts_reply_to_post_id FOREIGN KEY (reply_to_post_id) REFERENCES posts(id);

-- Add new auditing columns
ALTER TABLE posts ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS last_modified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS last_modified_by UUID;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS created_by UUID;

-- Add embedding column (using pgvector extension)
-- Assuming extension is already created in previous migrations (V30__add_posts_full_text_search.sql likely did it or similar)
CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE posts ADD COLUMN IF NOT EXISTS embedding vector(1536);
