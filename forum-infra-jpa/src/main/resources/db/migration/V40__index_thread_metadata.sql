-- GIN index for efficient JSONB metadata queries
-- Enables filtering threads by metadata key-value pairs (e.g., questionId=102)
CREATE INDEX idx_threads_metadata ON threads USING GIN (metadata);
