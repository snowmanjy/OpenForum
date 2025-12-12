-- Enable pgvector extension for AI vector embeddings
-- Note: Requires pgvector/pgvector:pg16 Docker image
CREATE EXTENSION IF NOT EXISTS vector;

-- Add embedding column for AI-generated semantic vectors
-- Using 1536 dimensions (standard for OpenAI text-embedding-3-small)
-- Can also support 768 dimensions for Gemini/Vertex AI
ALTER TABLE posts ADD COLUMN embedding vector(1536);

-- Create HNSW index for fast approximate nearest neighbor search
-- Using cosine similarity which is standard for text embeddings
CREATE INDEX idx_posts_embedding ON posts USING hnsw (embedding vector_cosine_ops);

-- Note: Embeddings will be populated asynchronously by EmbeddingService
-- when posts are created. NULL embeddings indicate pending processing.
