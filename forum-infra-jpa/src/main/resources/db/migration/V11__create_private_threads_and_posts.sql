CREATE TABLE private_threads (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE private_posts (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_private_posts_thread FOREIGN KEY (thread_id) REFERENCES private_threads(id)
);

CREATE TABLE private_thread_participants (
    private_thread_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    CONSTRAINT fk_ptp_thread FOREIGN KEY (private_thread_id) REFERENCES private_threads(id),
    PRIMARY KEY (private_thread_id, participant_id)
);

CREATE INDEX idx_private_posts_thread_created ON private_posts(thread_id, created_at);
CREATE INDEX idx_ptp_participant ON private_thread_participants(participant_id);
