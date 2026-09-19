-- A quoted pack price belongs to the whole pack, not one bottle's volume.
WITH multipacks AS (
    SELECT id, regexp_match(name, '\m([0-9]+)\s*[x×]\s*([0-9]+(?:[.,][0-9]+)?)\s*(kg|g|ml|l)\M', 'i') AS parts
    FROM products
)
UPDATE products AS product
SET quantity = multipacks.parts[1]::numeric,
    unit = 'UN',
    package_description = multipacks.parts[1] || ' X ' || replace(multipacks.parts[2], ',', '.') || ' ' || upper(multipacks.parts[3])
FROM multipacks
WHERE product.id = multipacks.id AND multipacks.parts IS NOT NULL
  AND multipacks.parts[1]::numeric > 0;
