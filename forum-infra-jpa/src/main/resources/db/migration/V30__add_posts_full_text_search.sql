-- Add search_vector column for full-text search on posts
ALTER TABLE posts ADD COLUMN search_vector tsvector;

-- Create GIN index for fast full-text search
CREATE INDEX idx_posts_search_vector ON posts USING GIN(search_vector);

-- Create trigger to automatically update search_vector when content changes
-- Using the built-in tsvector_update_trigger function
CREATE TRIGGER posts_tsvectorupdate BEFORE INSERT OR UPDATE
ON posts FOR EACH ROW EXECUTE FUNCTION
tsvector_update_trigger(search_vector, 'pg_catalog.english', content);

-- Populate existing rows with search vectors
UPDATE posts SET search_vector = to_tsvector('english', COALESCE(content, ''));
