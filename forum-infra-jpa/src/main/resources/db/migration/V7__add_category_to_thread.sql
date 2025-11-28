ALTER TABLE threads ADD COLUMN category_id UUID;
CREATE INDEX idx_threads_category ON threads (category_id);
