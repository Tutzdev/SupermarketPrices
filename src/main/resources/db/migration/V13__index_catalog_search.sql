CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_products_name_order ON products (name, id);
CREATE INDEX idx_products_name_search ON products USING gin (lower(name) gin_trgm_ops);
CREATE INDEX idx_products_brand_search ON products USING gin (lower(brand) gin_trgm_ops);
CREATE INDEX idx_products_category_search ON products USING gin (lower(category) gin_trgm_ops);
CREATE INDEX idx_products_description_search ON products USING gin (lower(description) gin_trgm_ops);
CREATE INDEX idx_products_normalized_name_search ON products USING gin (normalized_name gin_trgm_ops);
CREATE INDEX idx_products_normalized_brand_search ON products USING gin (normalized_brand gin_trgm_ops);
