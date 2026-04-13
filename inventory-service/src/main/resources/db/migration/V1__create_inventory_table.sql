CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE inventory
(
    id         UUID PRIMARY KEY   DEFAULT gen_random_uuid(),
    product_id UUID      NOT NULL,
    available  INTEGER   NOT NULL DEFAULT 0 CHECK (available >= 0),
    reserved   INTEGER   NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    version    BIGINT    NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_inventory_product UNIQUE (product_id),
    CONSTRAINT ck_inventory_available CHECK (available >= 0)
);

CREATE INDEX idx_inventory_product_id ON inventory (product_id);

COMMENT
ON TABLE  inventory IS 'Stock de inventario por producto';
COMMENT
ON COLUMN inventory.version IS 'Versión para control de concurrencia optimista';
COMMENT
ON COLUMN inventory.available IS 'Unidades disponibles para la venta';
COMMENT
ON COLUMN inventory.reserved IS 'Unidades reservadas pendientes de confirmación';
