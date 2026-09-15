-- Deliberately fictional fixtures: loaded only by transactional automated tests.
INSERT INTO data_sources (id, code, name, base_url, enabled, verified_at, created_at)
VALUES ('00000000-0000-0000-0000-000000000101', 'FICTIONAL_TEST', 'Fonte fictícia de teste',
        'https://source.example.test', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO supermarket_chains (id, name, source_id, source_reference, collected_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000151', 'Rede fictícia de teste',
        '00000000-0000-0000-0000-000000000101', 'test-chain', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO stores (id, supermarket_chain_id, city_id, name, active, source_id,
                    source_reference, collected_at, updated_at)
SELECT '00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000151', id,
       'Loja fictícia A', true, '00000000-0000-0000-0000-000000000101', 'test-store-a',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM cities WHERE name = 'Rio de Janeiro';

INSERT INTO stores (id, supermarket_chain_id, city_id, name, active, source_id,
                    source_reference, collected_at, updated_at)
SELECT '00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000151', id,
       'Loja fictícia B', true, '00000000-0000-0000-0000-000000000101', 'test-store-b',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM cities WHERE name = 'Rio de Janeiro';

INSERT INTO products (id, name, brand, unit, quantity, source_id, source_reference, collected_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000301', 'Produto FICTÍCIO Alpha', 'Marca fictícia', 'UN', 1,
        '00000000-0000-0000-0000-000000000101', 'test-product-a', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000302', 'Produto FICTÍCIO Beta', 'Marca fictícia', 'UN', 1,
        '00000000-0000-0000-0000-000000000101', 'test-product-b', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_source_references (id, product_id, source_id, source_reference, collected_at)
VALUES ('00000000-0000-0000-0000-000000000401', '00000000-0000-0000-0000-000000000301',
        '00000000-0000-0000-0000-000000000101', 'test-product-a', CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000402', '00000000-0000-0000-0000-000000000302',
        '00000000-0000-0000-0000-000000000101', 'test-product-b', CURRENT_TIMESTAMP);
