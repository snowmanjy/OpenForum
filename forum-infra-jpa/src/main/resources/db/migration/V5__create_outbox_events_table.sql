CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID,
    type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_events_created_at ON outbox_events(created_at);
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id);
