CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    preferred_city_id UUID REFERENCES cities(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE favorite_stores (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_favorite_store UNIQUE (user_id, store_id)
);

CREATE INDEX idx_favorite_stores_user ON favorite_stores(user_id, created_at, id);

CREATE TABLE price_alerts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    city_id UUID NOT NULL REFERENCES cities(id) ON DELETE RESTRICT,
    target_price NUMERIC(12, 2) NOT NULL CHECK (target_price > 0),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_notified_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_price_alerts_user_time ON price_alerts(user_id, created_at DESC, id);
CREATE INDEX idx_price_alerts_evaluation ON price_alerts(product_id, city_id) WHERE active;

CREATE TABLE alert_notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    alert_id UUID NOT NULL REFERENCES price_alerts(id) ON DELETE RESTRICT,
    price_record_id UUID NOT NULL REFERENCES price_records(id) ON DELETE RESTRICT,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price > 0),
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    CONSTRAINT uq_alert_notification_observation UNIQUE (alert_id, price_record_id)
);

CREATE INDEX idx_alert_notifications_user_time
    ON alert_notifications(user_id, created_at DESC, id);
