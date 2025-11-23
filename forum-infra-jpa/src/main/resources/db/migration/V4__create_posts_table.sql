CREATE TABLE posts (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    version BIGINT NOT NULL,
    reply_to_post_id UUID,
    metadata JSONB,
    CONSTRAINT fk_posts_thread FOREIGN KEY (thread_id) REFERENCES threads(id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES members(id),
    CONSTRAINT fk_posts_reply_to FOREIGN KEY (reply_to_post_id) REFERENCES posts(id)
);

CREATE INDEX idx_posts_thread_id ON posts(thread_id);
CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_posts_reply_to ON posts(reply_to_post_id);
