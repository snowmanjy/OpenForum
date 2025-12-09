-- 1. POSTS: Rename updated_at -> last_modified_at, Add created_by, last_modified_by
ALTER TABLE posts RENAME COLUMN updated_at TO last_modified_at;
ALTER TABLE posts ADD COLUMN created_by UUID;
ALTER TABLE posts ADD COLUMN last_modified_by UUID;

-- Backfill posts.created_by from author_id
UPDATE posts SET created_by = author_id;


-- 2. THREADS: Add last_modified_at, created_by, last_modified_by (created_at exists)
ALTER TABLE threads ADD COLUMN last_modified_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE threads ADD COLUMN created_by UUID;
ALTER TABLE threads ADD COLUMN last_modified_by UUID;

-- Backfill threads.created_by from author_id
UPDATE threads SET created_by = author_id;
-- Backfill threads.last_modified_at from created_at initially
UPDATE threads SET last_modified_at = created_at WHERE last_modified_at IS NULL;


-- 3. SUBSCRIPTIONS: Add last_modified_at, created_by, last_modified_by (created_at exists)
ALTER TABLE subscriptions ADD COLUMN last_modified_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE subscriptions ADD COLUMN created_by UUID;
ALTER TABLE subscriptions ADD COLUMN last_modified_by UUID;

-- Backfill subscriptions.created_by from user_id
UPDATE subscriptions SET created_by = user_id;
UPDATE subscriptions SET last_modified_at = created_at WHERE last_modified_at IS NULL;


-- 4. POLLS: Add last_modified_at, created_by, last_modified_by (created_at exists)
ALTER TABLE polls ADD COLUMN last_modified_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE polls ADD COLUMN created_by UUID;
ALTER TABLE polls ADD COLUMN last_modified_by UUID;
-- Polls has no user/author link directly on the entity (in the previous file view it had postId), can't easily backfill created_by without joining.
-- We can join posts to get author?
UPDATE polls SET created_by = (SELECT author_id FROM posts WHERE posts.id = polls.post_id);
UPDATE polls SET last_modified_at = created_at WHERE last_modified_at IS NULL;


-- 5. CATEGORIES: Add created_at, last_modified_at, created_by, last_modified_by
ALTER TABLE categories ADD COLUMN created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE categories ADD COLUMN last_modified_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE categories ADD COLUMN created_by UUID;
ALTER TABLE categories ADD COLUMN last_modified_by UUID;


-- 6. TAGS: Add created_at, last_modified_at, created_by, last_modified_by
ALTER TABLE tags ADD COLUMN created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE tags ADD COLUMN last_modified_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE tags ADD COLUMN created_by UUID;
ALTER TABLE tags ADD COLUMN last_modified_by UUID;


-- 7. MEMBERS: Add created_at, last_modified_at, created_by, last_modified_by
-- Members has joined_at, but we'll add standard created_at too, or we can reuse joined_at?
-- The user request was "Move createdAt field to TenantAwareEntity".
-- MemberEntity extends TenantAwareEntity. MemberEntity already has joined_at.
-- Let's add created_at distinct from joined_at to align with the base class requirements, or we keep it redundant.
-- Usually system created_at vs business joined_at.
ALTER TABLE members ADD COLUMN created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE members ADD COLUMN last_modified_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE members ADD COLUMN created_by UUID;
ALTER TABLE members ADD COLUMN last_modified_by UUID;
-- Backfill created_at from joined_at if reasonable, but default current_timestamp is fine for new column.
UPDATE members SET created_at = joined_at WHERE joined_at IS NOT NULL;


-- 8. PRIVATE THREADS: Add created_by, last_modified_by (created_at, last_activity_at exist)
-- PrivateThreadEntity extends TenantAwareEntity.
-- It has created_at, last_activity_at.
ALTER TABLE private_threads ADD COLUMN last_modified_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE private_threads ADD COLUMN created_by UUID;
ALTER TABLE private_threads ADD COLUMN last_modified_by UUID;
UPDATE private_threads SET last_modified_at = last_activity_at;

