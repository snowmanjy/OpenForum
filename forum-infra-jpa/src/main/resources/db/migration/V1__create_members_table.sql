CREATE TABLE members (
    id UUID PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_bot BOOLEAN NOT NULL DEFAULT FALSE,
    reputation INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_members_external_id ON members(external_id);
