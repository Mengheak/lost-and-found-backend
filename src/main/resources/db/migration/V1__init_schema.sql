-- Lost & Found initial schema.
-- Timestamps are stored as TIMESTAMPTZ (UTC). Every table carries created_at/updated_at
-- managed by the application's BaseEntity.

CREATE TABLE users (
    id                UUID PRIMARY KEY,
    name              VARCHAR(255)     NOT NULL,
    email             VARCHAR(255)     NOT NULL,
    phone             VARCHAR(50),
    password_hash     VARCHAR(255)     NOT NULL,
    profile_photo_url VARCHAR(1024),
    rating_avg        DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    icon_url   VARCHAR(1024),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE TABLE items (
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id      UUID         NOT NULL REFERENCES categories (id),
    type             VARCHAR(10)  NOT NULL, -- LOST | FOUND
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    brand            VARCHAR(100),
    color            VARCHAR(50),
    location_lat     DOUBLE PRECISION,
    location_lng     DOUBLE PRECISION,
    date_time        TIMESTAMPTZ,
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN', -- OPEN | RETURNED | CLOSED
    reward_amount    NUMERIC(12, 2),        -- only meaningful when type = LOST
    storage_location VARCHAR(255),          -- only meaningful when type = FOUND
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Element-collection table backing items.photo_urls
CREATE TABLE item_photo_urls (
    item_id   UUID          NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    photo_url VARCHAR(1024) NOT NULL
);

CREATE TABLE conversations (
    id         UUID PRIMARY KEY,
    item_id    UUID        NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    user_a_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_b_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE messages (
    id              UUID PRIMARY KEY,
    conversation_id UUID        NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text            TEXT,
    image_url       VARCHAR(1024),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE saved_items (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    item_id    UUID        NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_saved_items_user_item UNIQUE (user_id, item_id)
);

CREATE TABLE ratings (
    id           UUID PRIMARY KEY,
    from_user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    to_user_id   UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    item_id      UUID        NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    score        INT         NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment      TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ratings_from_to_item UNIQUE (from_user_id, to_user_id, item_id)
);

CREATE TABLE notifications (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(50) NOT NULL,
    message    TEXT        NOT NULL,
    is_read    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_items_user ON items (user_id);
CREATE INDEX idx_items_category ON items (category_id);
CREATE INDEX idx_items_status ON items (status);
CREATE INDEX idx_items_type ON items (type);
CREATE INDEX idx_items_date_time ON items (date_time);
CREATE INDEX idx_items_created_at ON items (created_at);
CREATE INDEX idx_item_photo_urls_item ON item_photo_urls (item_id);
CREATE INDEX idx_conversations_item ON conversations (item_id);
CREATE INDEX idx_conversations_user_a ON conversations (user_a_id);
CREATE INDEX idx_conversations_user_b ON conversations (user_b_id);
CREATE INDEX idx_messages_conversation_created ON messages (conversation_id, created_at);
CREATE INDEX idx_ratings_to_user ON ratings (to_user_id);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at);
