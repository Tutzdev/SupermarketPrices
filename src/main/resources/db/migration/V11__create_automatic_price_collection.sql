ALTER TABLE products
    ADD COLUMN normalized_name VARCHAR(200),
    ADD COLUMN normalized_brand VARCHAR(120),
    ADD COLUMN package_description VARCHAR(120);

ALTER TABLE price_records
    ADD COLUMN promotion_condition VARCHAR(500);

CREATE TABLE collection_runs (
    id UUID PRIMARY KEY,
    collector_code VARCHAR(80) NOT NULL,
    supermarket_name VARCHAR(160) NOT NULL,
    store_name VARCHAR(160) NOT NULL,
    source_id UUID REFERENCES data_sources(id) ON DELETE RESTRICT,
    store_id UUID REFERENCES stores(id) ON DELETE RESTRICT,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status VARCHAR(16) NOT NULL,
    found_count INTEGER NOT NULL DEFAULT 0 CHECK (found_count >= 0),
    created_count INTEGER NOT NULL DEFAULT 0 CHECK (created_count >= 0),
    updated_count INTEGER NOT NULL DEFAULT 0 CHECK (updated_count >= 0),
    skipped_count INTEGER NOT NULL DEFAULT 0 CHECK (skipped_count >= 0),
    error_count INTEGER NOT NULL DEFAULT 0 CHECK (error_count >= 0),
    error_message VARCHAR(2000),
    CONSTRAINT ck_collection_run_status CHECK (status IN ('RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED')),
    CONSTRAINT ck_collection_run_finished CHECK (
        (status = 'RUNNING' AND finished_at IS NULL)
        OR (status <> 'RUNNING' AND finished_at IS NOT NULL AND finished_at >= started_at)
    )
);

CREATE INDEX idx_collection_runs_collector_started
    ON collection_runs(collector_code, started_at DESC, id DESC);

CREATE INDEX idx_collection_runs_status_started
    ON collection_runs(status, started_at DESC);
