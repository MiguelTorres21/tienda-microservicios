INSERT INTO products (id, sku, name, price, status)
VALUES ('a1b2c3d4-0001-0001-0001-000000000001', 'SKU-001', 'Laptop Pro 15', 1299.99, 'ACTIVE'),
       ('a1b2c3d4-0002-0002-0002-000000000002', 'SKU-002', 'Mouse Inalámbrico', 29.99, 'ACTIVE'),
       ('a1b2c3d4-0003-0003-0003-000000000003', 'SKU-003', 'Teclado Mecánico', 89.99, 'ACTIVE'),
       ('a1b2c3d4-0004-0004-0004-000000000004', 'SKU-004', 'Monitor 27 4K', 499.99, 'ACTIVE'),
       ('a1b2c3d4-0005-0005-0005-000000000005', 'SKU-005', 'Auriculares BT', 79.99,
        'INACTIVE') ON CONFLICT (sku) DO NOTHING;
