-- Add search_vector column
ALTER TABLE threads ADD COLUMN search_vector tsvector;

-- Create GIN index for fast full-text search
CREATE INDEX idx_threads_search_vector ON threads USING GIN(search_vector);

-- Create function to update search_vector
CREATE OR REPLACE FUNCTION threads_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Create trigger to update search_vector on insert or update
CREATE TRIGGER threads_search_vector_update
BEFORE INSERT OR UPDATE ON threads
FOR EACH ROW EXECUTE FUNCTION threads_search_vector_update();

-- Update existing rows
UPDATE threads SET search_vector = setweight(to_tsvector('english', COALESCE(title, '')), 'A');
