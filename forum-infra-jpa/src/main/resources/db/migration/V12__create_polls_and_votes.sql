CREATE TABLE polls (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    post_id UUID NOT NULL,
    question TEXT NOT NULL,
    options JSONB NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    allow_multiple_votes BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE poll_votes (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES polls(id),
    voter_id UUID NOT NULL,
    option_index INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_polls_tenant_id ON polls(tenant_id);
CREATE INDEX idx_poll_votes_poll_id ON poll_votes(poll_id);
CREATE INDEX idx_poll_votes_voter_id ON poll_votes(voter_id);
