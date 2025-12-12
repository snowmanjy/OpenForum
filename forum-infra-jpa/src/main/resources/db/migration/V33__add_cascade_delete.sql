-- Add ON DELETE CASCADE to posts foreign key for thread_id
ALTER TABLE posts DROP CONSTRAINT fk_posts_thread;

ALTER TABLE posts 
    ADD CONSTRAINT fk_posts_thread 
    FOREIGN KEY (thread_id) 
    REFERENCES threads(id) 
    ON DELETE CASCADE;
