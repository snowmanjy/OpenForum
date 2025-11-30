CREATE TABLE IF NOT EXISTS post_mentions (
    post_id UUID NOT NULL,
    user_id UUID,
    CONSTRAINT fk_post_mentions_post FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE INDEX IF NOT EXISTS idx_post_mentions_post_id ON post_mentions(post_id);
