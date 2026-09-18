CREATE INDEX idx_products_gtin_search ON products USING gin (gtin gin_trgm_ops);
