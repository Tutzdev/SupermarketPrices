ALTER TABLE app_users
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER',
    ADD COLUMN email_verified_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_app_users_role CHECK (role IN ('USER', 'ADMIN'));

CREATE INDEX idx_app_users_role ON app_users(role);

CREATE TABLE admin_audit_entries (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    action VARCHAR(80) NOT NULL CHECK (btrim(action) <> ''),
    resource_type VARCHAR(80) NOT NULL CHECK (btrim(resource_type) <> ''),
    resource_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_admin_audit_actor_time ON admin_audit_entries(actor_user_id, occurred_at DESC);
CREATE INDEX idx_admin_audit_resource ON admin_audit_entries(resource_type, resource_id, occurred_at DESC);

CREATE TABLE price_contributions (
    id UUID PRIMARY KEY,
    contributor_id UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    regular_price NUMERIC(12, 2) NOT NULL CHECK (regular_price > 0),
    promotional_price NUMERIC(12, 2),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL' CHECK (currency = 'BRL'),
    observed_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    promotion_valid_until TIMESTAMPTZ,
    availability VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    moderator_id UUID REFERENCES app_users(id) ON DELETE RESTRICT,
    decided_at TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    price_record_id UUID UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_contribution_promotion CHECK (
        promotional_price IS NULL OR (promotional_price > 0 AND promotional_price < regular_price)
    ),
    CONSTRAINT ck_contribution_availability CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'UNKNOWN')),
    CONSTRAINT ck_contribution_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_contribution_observed_time CHECK (observed_at <= submitted_at),
    CONSTRAINT ck_contribution_validity CHECK (valid_until IS NULL OR valid_until > observed_at),
    CONSTRAINT ck_contribution_promotion_validity CHECK (
        promotion_valid_until IS NULL
        OR (promotional_price IS NOT NULL AND promotion_valid_until > observed_at)
    ),
    CONSTRAINT ck_contribution_decision CHECK (
        (status = 'PENDING' AND moderator_id IS NULL AND decided_at IS NULL
            AND rejection_reason IS NULL AND price_record_id IS NULL)
        OR (status = 'APPROVED' AND moderator_id IS NOT NULL AND decided_at IS NOT NULL
            AND rejection_reason IS NULL AND price_record_id IS NOT NULL)
        OR (status = 'REJECTED' AND moderator_id IS NOT NULL AND decided_at IS NOT NULL
            AND rejection_reason IS NOT NULL AND price_record_id IS NULL)
    )
);

CREATE INDEX idx_contributions_contributor_time
    ON price_contributions(contributor_id, submitted_at DESC, id);
CREATE INDEX idx_contributions_moderation_queue
    ON price_contributions(status, submitted_at, id);

ALTER TABLE price_records
    ADD COLUMN origin_type VARCHAR(24) NOT NULL DEFAULT 'SOURCE',
    ADD COLUMN contribution_id UUID,
    ADD CONSTRAINT ck_price_origin_type CHECK (origin_type IN ('SOURCE', 'USER_CONTRIBUTION')),
    ADD CONSTRAINT fk_price_contribution FOREIGN KEY (contribution_id)
        REFERENCES price_contributions(id) ON DELETE RESTRICT,
    ADD CONSTRAINT uq_price_contribution UNIQUE (contribution_id),
    ADD CONSTRAINT ck_price_origin_reference CHECK (
        (origin_type = 'SOURCE' AND contribution_id IS NULL)
        OR (origin_type = 'USER_CONTRIBUTION' AND contribution_id IS NOT NULL)
    );

ALTER TABLE price_contributions
    ADD CONSTRAINT fk_contribution_price_record FOREIGN KEY (price_record_id)
        REFERENCES price_records(id) ON DELETE RESTRICT;
