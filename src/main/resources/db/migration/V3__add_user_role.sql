-- Adds role-based access control. Every existing account becomes a regular USER;
-- promote the first admin with:
--   UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';

ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));

CREATE INDEX idx_users_role ON users (role);
