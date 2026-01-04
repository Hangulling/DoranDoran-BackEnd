-- Add signup-related columns to user_schema.app_user
-- Run on the target PostgreSQL database

ALTER TABLE user_schema.app_user
    ADD COLUMN IF NOT EXISTS birth_date DATE,
    ADD COLUMN IF NOT EXISTS signup_question VARCHAR(255),
    ADD COLUMN IF NOT EXISTS signup_answer VARCHAR(30);

-- Optional: initialize empty strings for existing rows to avoid null checks in legacy code
UPDATE user_schema.app_user
SET
    signup_question = COALESCE(signup_question, ''),
    signup_answer   = COALESCE(signup_answer, '')
WHERE signup_question IS NULL OR signup_answer IS NULL;

