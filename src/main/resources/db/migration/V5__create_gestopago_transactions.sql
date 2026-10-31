ALTER TABLE cuentas
    ADD COLUMN IF NOT EXISTS saldo_retenido NUMERIC(24, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE gestopago_tokens
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

ALTER TABLE gestopago_products
    ALTER COLUMN precio TYPE NUMERIC(24, 2) USING precio::NUMERIC(24, 2);

CREATE TABLE IF NOT EXISTS gestopago_transactions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    upc VARCHAR(15) NOT NULL UNIQUE,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
    cliente_id INTEGER NOT NULL REFERENCES clientes(id),
    cuenta_id INTEGER NOT NULL REFERENCES cuentas(id),
    id_producto INTEGER NOT NULL,
    id_servicio INTEGER NOT NULL,
    tipo_front INTEGER NOT NULL,
    id_cat_tipo_servicio INTEGER,
    telefono VARCHAR(20) NOT NULL,
    referencia VARCHAR(100),
    monto NUMERIC(24, 2) NOT NULL,
    unidad VARCHAR(25) NOT NULL,
    hora_local VARCHAR(14),
    payload_json TEXT,
    estado VARCHAR(30) NOT NULL,
    provider_code VARCHAR(10),
    provider_message TEXT,
    provider_transaction_id VARCHAR(60),
    provider_authorization VARCHAR(60),
    pin VARCHAR(500),
    legend TEXT,
    confirmation_attempts INTEGER NOT NULL DEFAULT 0,
    next_confirmation_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_gestopago_tx_status_confirmation
    ON gestopago_transactions (estado, next_confirmation_at);

CREATE INDEX IF NOT EXISTS idx_gestopago_tx_user
    ON gestopago_transactions (usuario_id, created_at DESC);
