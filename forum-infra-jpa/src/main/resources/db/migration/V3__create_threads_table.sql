CREATE TABLE threads (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    author_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    metadata JSONB,
    version BIGINT,
    CONSTRAINT fk_threads_author FOREIGN KEY (author_id) REFERENCES members(id)
);

CREATE INDEX idx_threads_tenant_id ON threads(tenant_id);
CREATE INDEX idx_threads_author_id ON threads(author_id);
CREATE INDEX idx_threads_tenant_status ON threads(tenant_id, status);
