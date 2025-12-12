-- Rename user_id to member_id in subscriptions table
ALTER TABLE subscriptions RENAME COLUMN user_id TO member_id;

-- Rename user_id to member_id in post_votes table
ALTER TABLE post_votes RENAME COLUMN user_id TO member_id;

-- Rename mentioned_user_ids to mentioned_member_ids in posts table
ALTER TABLE posts RENAME COLUMN mentioned_user_ids TO mentioned_member_ids;
