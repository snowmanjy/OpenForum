-- Composite index for efficient sorting by score with createdAt tie-breaker
-- This prevents slow sorting when many posts have the same score
CREATE INDEX idx_posts_score_created ON posts (score DESC, created_at ASC);
