-- Add voting tables for post upvote/downvote system

-- Table: post_votes
CREATE TABLE post_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    value SMALLINT NOT NULL CHECK (value IN (-1, 1)),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID,
    last_modified_at TIMESTAMP WITH TIME ZONE,
    last_modified_by UUID,
    CONSTRAINT fk_post_votes_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT uq_post_votes_post_user UNIQUE (post_id, user_id)
);

-- Index for efficient lookups by post
CREATE INDEX idx_post_votes_post_id ON post_votes(post_id);

-- Add score column to posts table
ALTER TABLE posts ADD COLUMN score INTEGER NOT NULL DEFAULT 0;

-- Index for sorting by score (e.g., 'Sort by Top')
CREATE INDEX idx_posts_score ON posts(score);
