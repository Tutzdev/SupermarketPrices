-- A candle's printed digit is a variant, not the number of candles in a pack.
WITH numbered_candles AS (
    SELECT id, regexp_match(name, '\mN[.º°]?\s*([0-9]+)\s*UN\M', 'i') AS variant
    FROM products
    WHERE name ~* '\mVELA\M' AND unit = 'UN'
)
UPDATE products product
SET quantity = NULL, unit = NULL, package_description = NULL
FROM numbered_candles
WHERE product.id = numbered_candles.id
  AND numbered_candles.variant IS NOT NULL
  AND product.quantity = numbered_candles.variant[1]::numeric;
