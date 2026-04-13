CREATE TABLE idempotency_records
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    expires_at      TIMESTAMP    NOT NULL,

    CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_idempotency_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_idempotency_key ON idempotency_records (idempotency_key);
CREATE INDEX idx_idempotency_expires_at ON idempotency_records (expires_at);

COMMENT
ON TABLE  idempotency_records IS 'Registro de operaciones idempotentes para POST /purchases';
COMMENT
ON COLUMN idempotency_records.status IS 'PROCESSING: en curso, COMPLETED: exitosa, FAILED: fallida con error de negocio';
COMMENT
ON COLUMN idempotency_records.response_body IS 'JSON serializado de la respuesta para devolverla idéntica en requests repetidos';
COMMENT
ON COLUMN idempotency_records.expires_at IS 'Marca de tiempo de expiración. Registros expirados se limpian por scheduled job';
