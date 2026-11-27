-- V7: Agregar soporte para TOTP (Google Authenticator) y flexibilizar empresa a ocupacion
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(64);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'clientes' AND column_name = 'empresa'
    ) THEN
        ALTER TABLE clientes ALTER COLUMN empresa DROP NOT NULL;
    END IF;
END $$;
