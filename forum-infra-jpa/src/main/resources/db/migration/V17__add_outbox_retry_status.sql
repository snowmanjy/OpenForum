ALTER TABLE outbox_events
ADD COLUMN retry_count INTEGER DEFAULT 0 NOT NULL,
ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
ADD COLUMN error_message TEXT;

CREATE INDEX idx_outbox_status ON outbox_events(status);
