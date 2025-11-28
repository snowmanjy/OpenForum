CREATE TABLE categories (
    id UUID NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    is_read_only BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_category_tenant_slug UNIQUE (tenant_id, slug)
);
