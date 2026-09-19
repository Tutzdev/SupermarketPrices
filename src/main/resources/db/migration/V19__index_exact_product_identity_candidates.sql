CREATE INDEX idx_products_exact_identity_candidates ON products (normalized_brand, unit, quantity)
    WHERE normalized_brand IS NOT NULL AND quantity IS NOT NULL;
