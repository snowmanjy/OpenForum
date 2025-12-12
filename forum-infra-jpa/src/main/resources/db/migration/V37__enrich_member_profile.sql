-- Add avatar_url column to members table if it doesn't exist
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'members' AND column_name = 'avatar_url') THEN 
        ALTER TABLE members ADD COLUMN avatar_url VARCHAR(2048); 
    END IF; 
END $$;
