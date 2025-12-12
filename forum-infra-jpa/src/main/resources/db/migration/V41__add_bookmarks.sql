-- Bookmarks table for saving posts to private collection
CREATE TABLE bookmarks (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    member_id UUID NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID,
    last_modified_at TIMESTAMP WITH TIME ZONE,
    last_modified_by UUID
);

-- Unique constraint: user can only bookmark a post once
CREATE UNIQUE INDEX idx_bookmarks_member_post ON bookmarks(member_id, post_id);

-- Index for fetching user's bookmarks efficiently
CREATE INDEX idx_bookmarks_member ON bookmarks(member_id);

-- Index for tenant filtering
CREATE INDEX idx_bookmarks_tenant ON bookmarks(tenant_id);

-- Add bookmark_count to posts for high-performance sorting
ALTER TABLE posts ADD COLUMN bookmark_count INTEGER NOT NULL DEFAULT 0;
