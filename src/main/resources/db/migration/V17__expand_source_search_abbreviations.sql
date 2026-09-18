-- Search aliases are separate from product identity / GTIN matching.
CREATE OR REPLACE FUNCTION gomo_search_text(value text) RETURNS text
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
    result := regexp_replace(result, '\m(refri|refr|refrigerantes)\M', 'refrigerante', 'g');
    result := regexp_replace(result, '\m(carne de boi|carne bovina|carnes bovinas|bovina|bovinos|bovinas)\M', 'bovino', 'g');
    result := regexp_replace(result, '\m(litros?)\M', 'l', 'g');
    result := regexp_replace(result, '\m(mililitros?)\M', 'ml', 'g');
    result := regexp_replace(result, '\m(quilos?|quilogramas?)\M', 'kg', 'g');
    result := regexp_replace(result, '\m(gramas?)\M', 'g', 'g');
    result := regexp_replace(result, '\m(cafes)\M', 'cafe', 'g');
    result := regexp_replace(result, '\m(deterg|detergentes)\M', 'detergente', 'g');
    result := regexp_replace(result, '\m(saboes)\M', 'sabao', 'g');
    result := regexp_replace(result, '\m(feijoes)\M', 'feijao', 'g');
    result := regexp_replace(result, '\m(carnes)\M', 'carne', 'g');
    RETURN trim(regexp_replace(result, '\s+', ' ', 'g'));
END;
$$;

-- Recompute generated search fields after adding source abbreviations.
-- Source names, identities, collection dates and price history are unchanged.
UPDATE products SET name = name;
