CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    CONSTRAINT ck_email_verification_expiration CHECK (expires_at > created_at),
    CONSTRAINT ck_email_verification_usage CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE INDEX idx_email_verification_user_expiration
    ON email_verification_tokens(user_id, expires_at DESC);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    CONSTRAINT ck_password_reset_expiration CHECK (expires_at > created_at),
    CONSTRAINT ck_password_reset_usage CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE INDEX idx_password_reset_user_expiration
    ON password_reset_tokens(user_id, expires_at DESC);
