WITH promotional_packages AS (
    SELECT id, regexp_match(name, '\mLEVE\s*(\d+)\s*PAGUE\s*\d+\M', 'i') AS quantity_match
    FROM products
    WHERE unit = 'UN'
)
UPDATE products product
SET quantity = promotional_packages.quantity_match[1]::numeric,
    package_description = COALESCE(
        regexp_replace(product.package_description, '^[0-9]+', promotional_packages.quantity_match[1]),
        promotional_packages.quantity_match[1] || ' UN')
FROM promotional_packages
WHERE product.id = promotional_packages.id
  AND promotional_packages.quantity_match IS NOT NULL
  AND promotional_packages.quantity_match[1]::numeric > 0
  AND product.quantity <> promotional_packages.quantity_match[1]::numeric;
