ALTER TABLE collection_runs
    ADD COLUMN available_count INTEGER NOT NULL DEFAULT 0 CHECK (available_count >= 0),
    ADD COLUMN unavailable_count INTEGER NOT NULL DEFAULT 0 CHECK (unavailable_count >= 0),
    ADD COLUMN unknown_availability_count INTEGER NOT NULL DEFAULT 0 CHECK (unknown_availability_count >= 0),
    ADD COLUMN products_updated_count INTEGER NOT NULL DEFAULT 0 CHECK (products_updated_count >= 0);
