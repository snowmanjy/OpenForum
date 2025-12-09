-- Fix missing reply_to_post_id
ALTER TABLE posts ADD COLUMN IF NOT EXISTS reply_to_post_id UUID;
CREATE INDEX IF NOT EXISTS idx_posts_reply_to ON posts(reply_to_post_id);
ALTER TABLE posts DROP CONSTRAINT IF EXISTS fk_posts_reply_to;
ALTER TABLE posts ADD CONSTRAINT fk_posts_reply_to FOREIGN KEY (reply_to_post_id) REFERENCES posts(id);

-- Fix missing mentioned_user_ids (JSONB)
ALTER TABLE posts ADD COLUMN IF NOT EXISTS mentioned_user_ids JSONB;

-- Fix missing updated_at
ALTER TABLE posts ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;
