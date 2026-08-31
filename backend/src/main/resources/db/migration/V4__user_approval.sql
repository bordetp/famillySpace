-- V4__user_approval.sql
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(16) NOT NULL DEFAULT 'pending';

-- Existing accounts keep access; new signups stay pending until moderated.
UPDATE users SET approval_status = 'approved' WHERE approval_status = 'pending';

CREATE INDEX IF NOT EXISTS idx_users_approval_status ON users(approval_status);
