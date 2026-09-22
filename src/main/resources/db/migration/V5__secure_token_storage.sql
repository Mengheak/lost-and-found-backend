-- Existing rows contain reusable bearer credentials and have no trustworthy database expiry.
-- Invalidate those sessions once, then store only SHA-256 fingerprints for newly issued tokens.
DELETE FROM tokens;

ALTER TABLE tokens RENAME COLUMN token TO token_hash;
ALTER TABLE tokens RENAME CONSTRAINT uq_tokens_token TO uq_tokens_token_hash;
ALTER TABLE tokens ALTER COLUMN token_hash TYPE VARCHAR(64);
ALTER TABLE tokens ADD COLUMN expires_at TIMESTAMPTZ NOT NULL;

CREATE INDEX idx_tokens_expires_at ON tokens (expires_at);
