CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL CHECK (length(trim(name)) > 0),
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_app_users_email UNIQUE (email),
    CONSTRAINT ck_app_users_normalized_email CHECK (email = lower(trim(email)))
);

CREATE TABLE auth_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_auth_tokens_expiration CHECK (expires_at > created_at)
);

CREATE INDEX idx_auth_tokens_user_expiration ON auth_tokens(user_id, expires_at);
