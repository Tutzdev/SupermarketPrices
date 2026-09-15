-- Reject concurrent refreshes based on stale entity state instead of losing newer observations.
ALTER TABLE products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE product_source_references ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE stores ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE supermarket_chains ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
