-- Tabla de Clientes
CREATE TABLE IF NOT EXISTS clientes (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    segundo_nombre VARCHAR(50),
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    curp CHAR(18) UNIQUE NOT NULL,
    rfc CHAR(13) UNIQUE NOT NULL,
    sexo CHAR(1) NOT NULL,
    nacionalidad VARCHAR(100) NOT NULL,
    estado_civil VARCHAR(20) NOT NULL,
    correo_electronico VARCHAR(100) UNIQUE NOT NULL,
    telefono_movil CHAR(10) NOT NULL,
    telefono_alternativo CHAR(10),
    ocupacion VARCHAR(100) NOT NULL,
    empresa VARCHAR(100) NOT NULL,
    ingreso_mensual NUMERIC(24, 2) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de Domicilios (Relacion 1 a 1 con Cliente)
CREATE TABLE IF NOT EXISTS domicilios (
    id SERIAL PRIMARY KEY,
    cliente_id INT UNIQUE NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    calle VARCHAR(255) NOT NULL,
    numero_exterior VARCHAR(50) NOT NULL,
    numero_interior VARCHAR(50),
    colonia VARCHAR(255) NOT NULL,
    municipio VARCHAR(255) NOT NULL,
    estado VARCHAR(255) NOT NULL,
    codigo_postal CHAR(5) NOT NULL,
    pais VARCHAR(100) NOT NULL
);

-- Tabla de Cuentas (Relacion 1 a N con Cliente)
CREATE TABLE IF NOT EXISTS cuentas (
    id SERIAL PRIMARY KEY,
    cliente_id INT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    numero_cuenta VARCHAR(20) UNIQUE NOT NULL,
    saldo NUMERIC(24, 2) NOT NULL DEFAULT 0.00,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de Usuarios (Relacion 1 a 1 con Cliente)
-- Se elimino la tabla anterior (si existia de la V1) para adaptarla a la nueva arquitectura
DROP TABLE IF EXISTS usuarios CASCADE;

CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    cliente_id INT UNIQUE NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    correo VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) DEFAULT 'CLIENTE' NOT NULL,
    face_id_enabled BOOLEAN DEFAULT FALSE NOT NULL,
    face_id_hash VARCHAR(255),
    refresh_token VARCHAR(255),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
