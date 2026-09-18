-- Trigrams narrow candidates; edit distance rejects unrelated similar words.
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;

-- Beef-cut aliases apply only to meat classified by the source, never to identity matching.
CREATE FUNCTION gomo_product_search_text(product_name text, brand text, category text) RETURNS text
LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE AS $$
DECLARE result text;
BEGIN
    result := gomo_search_text(product_name || ' ' || coalesce(brand, '') || ' ' || coalesce(category, ''));
    IF result ~ '\m(carne|acougue|bovino)\M'
        AND result ~ '\m(patinho|acem|contrafile|alcatra|coxao mole|coxao duro)\M'
        AND result !~ '\m(suino|suina|frango|ave|aves|cordeiro|vegetal)\M' THEN
        result := result || ' bovino';
    END IF;
    RETURN result;
END;
$$;

-- Only the derived search column is rebuilt; original products, references and prices are preserved.
ALTER TABLE products DROP COLUMN search_text;
ALTER TABLE products ADD COLUMN search_text text GENERATED ALWAYS AS
    (gomo_product_search_text(name, brand, category)) STORED;
CREATE INDEX idx_products_search_text ON products USING gin (search_text gin_trgm_ops);
