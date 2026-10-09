-- Renombrar columna en la tabla clientes
ALTER TABLE clientes RENAME COLUMN correo_electronico TO correo;

-- Eliminar columna redundante en la tabla usuarios
ALTER TABLE usuarios DROP COLUMN correo;

-- Estandarizar campos de fecha en la tabla usuarios
ALTER TABLE usuarios RENAME COLUMN fecha_creacion TO created_at;
ALTER TABLE usuarios RENAME COLUMN fecha_actualizacion TO updated_at;
