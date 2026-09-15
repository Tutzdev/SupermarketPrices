CREATE TABLE shopping_lists (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    name VARCHAR(120) NOT NULL CHECK (btrim(name) <> ''),
    shopping_type VARCHAR(16) NOT NULL CHECK (shopping_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_shopping_lists_user_updated ON shopping_lists(user_id, updated_at DESC, id);

CREATE TABLE shopping_list_items (
    id UUID PRIMARY KEY,
    shopping_list_id UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity NUMERIC(12, 3) NOT NULL CHECK (quantity > 0 AND quantity <= 999999),
    CONSTRAINT uq_shopping_list_product UNIQUE (shopping_list_id, product_id)
);

CREATE INDEX idx_shopping_list_items_product ON shopping_list_items(product_id);
