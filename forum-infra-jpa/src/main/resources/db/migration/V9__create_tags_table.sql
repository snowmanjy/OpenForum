CREATE TABLE tags (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    usage_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_tags_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_tags_name_prefix ON tags (tenant_id, name text_pattern_ops);
