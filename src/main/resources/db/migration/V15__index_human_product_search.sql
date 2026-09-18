-- Search aliases are separate from product identity / GTIN matching.
CREATE FUNCTION gomo_search_text(value text) RETURNS text
LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE AS $$
DECLARE result text;
BEGIN
    result := lower(translate(coalesce(value, ''),
        'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇáàâãäéèêëíìîïóòôõöúùûüç',
        'AAAAAEEEEIIIIOOOOOUUUUCaaaaaeeeeiiiiooooouuuuc'));
    result := regexp_replace(result, '([0-9])([a-z])', '\1 \2', 'g');
    result := regexp_replace(result, '[^a-z0-9]+', ' ', 'g');
    result := regexp_replace(result, '\m(cocacola)\M', 'coca cola', 'g');
    result := regexp_replace(result, '\m(ipe|ype)\M', 'ype', 'g');
    result := regexp_replace(result, '\m(refri|refrigerantes)\M', 'refrigerante', 'g');
    result := regexp_replace(result, '\m(carne de boi|carne bovina|carnes bovinas|bovina|bovinos|bovinas)\M', 'bovino', 'g');
    result := regexp_replace(result, '\m(litros?)\M', 'l', 'g');
    result := regexp_replace(result, '\m(mililitros?)\M', 'ml', 'g');
    result := regexp_replace(result, '\m(quilos?|quilogramas?)\M', 'kg', 'g');
    result := regexp_replace(result, '\m(gramas?)\M', 'g', 'g');
    result := regexp_replace(result, '\m(cafes)\M', 'cafe', 'g');
    result := regexp_replace(result, '\m(detergentes)\M', 'detergente', 'g');
    result := regexp_replace(result, '\m(saboes)\M', 'sabao', 'g');
    result := regexp_replace(result, '\m(feijoes)\M', 'feijao', 'g');
    result := regexp_replace(result, '\m(carnes)\M', 'carne', 'g');
    RETURN trim(regexp_replace(result, '\s+', ' ', 'g'));
END;
$$;

ALTER TABLE products
    ADD COLUMN search_name text GENERATED ALWAYS AS (gomo_search_text(name)) STORED,
    ADD COLUMN search_brand text GENERATED ALWAYS AS (gomo_search_text(brand)) STORED,
    ADD COLUMN search_text text GENERATED ALWAYS AS
        (gomo_search_text(name || ' ' || coalesce(brand, '') || ' ' || coalesce(category, ''))) STORED;

CREATE INDEX idx_products_search_text ON products USING gin (search_text gin_trgm_ops);
CREATE INDEX idx_products_search_name ON products USING gin (search_name gin_trgm_ops);
CREATE INDEX idx_products_search_measurement ON products (unit, quantity);
CREATE INDEX idx_prices_product_store_latest ON price_records
    (product_id, store_id, collected_at DESC, recorded_at DESC, id DESC);
