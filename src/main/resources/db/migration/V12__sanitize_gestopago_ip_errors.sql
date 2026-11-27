-- Normaliza codigos 55 o volcados tecnicos de validacion de IP de GestoPago a codigo 05 limpio
UPDATE gestopago_transactions
SET provider_code = '05',
    provider_message = 'Acceso denegado por GestoPago (Código 05): la dirección IP de origen no está autorizada para operar.'
WHERE provider_code = '55'
   OR LOWER(COALESCE(provider_message, '')) LIKE '%validationcompleted%'
   OR LOWER(COALESCE(provider_message, '')) LIKE '%validips%'
   OR LOWER(COALESCE(provider_message, '')) LIKE '%isvalid=false%';
