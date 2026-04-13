CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE products
(
    id         UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    sku        VARCHAR(100)   NOT NULL,
    name       VARCHAR(255)   NOT NULL,
    price      NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    status     VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at TIMESTAMP      NOT NULL DEFAULT now(),

    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_products_sku ON products (sku);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_name ON products (name);

COMMENT
ON TABLE  products IS 'Catálogo de productos de la tienda';
COMMENT
ON COLUMN products.sku IS 'Código único de referencia del producto';
COMMENT
ON COLUMN products.status IS 'Estado del producto: ACTIVE o INACTIVE';
