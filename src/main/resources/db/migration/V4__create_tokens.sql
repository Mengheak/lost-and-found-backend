CREATE TABLE tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(1000) NOT NULL,
    token_type VARCHAR(20)   NOT NULL DEFAULT 'BEARER',
    revoked    BOOLEAN       NOT NULL DEFAULT FALSE,
    expired    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_tokens_token UNIQUE (token),
    CONSTRAINT ck_tokens_token_type CHECK (token_type IN ('BEARER'))
);


CREATE INDEX idx_tokens_user_active ON tokens (user_id) WHERE revoked = FALSE AND expired = FALSE;
