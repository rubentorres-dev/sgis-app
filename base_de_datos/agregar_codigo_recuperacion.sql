USE sgis_db;

-- Codigo unico de recuperacion del propietario (usuario protegido).
-- Solo se guarda su hash (SHA-256, 64 caracteres), nunca el codigo.
-- Ejecutar UNA sola vez (si sale "Duplicate column name 'codigoRecuperacion'", ya estaba hecho).
ALTER TABLE USUARIO ADD COLUMN codigoRecuperacion VARCHAR(64) NULL;
