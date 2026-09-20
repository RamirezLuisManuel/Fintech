CREATE TABLE IF NOT EXISTS gestopago_products (
    id_producto INTEGER PRIMARY KEY,
    servicio VARCHAR(255),
    producto VARCHAR(255),
    id_servicio INTEGER,
    id_cat_tipo_servicio INTEGER,
    tipo_front INTEGER,
    has_digito_verificador BOOLEAN,
    precio DOUBLE PRECISION,
    show_ayuda BOOLEAN,
    tipo_referencia VARCHAR(50),
    legend TEXT
);
