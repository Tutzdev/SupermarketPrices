CREATE TABLE price_records (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    store_id UUID NOT NULL REFERENCES stores (id) ON DELETE RESTRICT,
    source_id UUID NOT NULL REFERENCES data_sources (id) ON DELETE RESTRICT,
    source_reference VARCHAR(500) NOT NULL CHECK (length(trim(source_reference)) > 0),
    regular_price NUMERIC(12, 2) NOT NULL CHECK (regular_price > 0),
    promotional_price NUMERIC(12, 2),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL' CHECK (currency = 'BRL'),
    collected_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    promotion_valid_until TIMESTAMPTZ,
    availability VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    CONSTRAINT uq_price_source_observation UNIQUE (source_id, source_reference),
    CONSTRAINT ck_price_promotion CHECK (
        promotional_price IS NULL OR (promotional_price > 0 AND promotional_price < regular_price)
    ),
    CONSTRAINT ck_price_availability CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'UNKNOWN')),
    CONSTRAINT ck_price_collection_time CHECK (collected_at <= recorded_at),
    CONSTRAINT ck_price_validity CHECK (valid_until IS NULL OR valid_until > collected_at),
    CONSTRAINT ck_price_promotion_validity CHECK (
        promotion_valid_until IS NULL
        OR (promotional_price IS NOT NULL AND promotion_valid_until > collected_at)
    )
);

CREATE INDEX idx_price_latest ON price_records (
    store_id, product_id, collected_at DESC, recorded_at DESC, id DESC
);
CREATE INDEX idx_price_product ON price_records (product_id);
