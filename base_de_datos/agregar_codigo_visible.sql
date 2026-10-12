USE sgis_db;

-- Copia CIFRADA (AES-256-GCM) del codigo de recuperacion de cada usuario, para que el administrador
-- principal pueda verlo en Configuracion > Usuarios del sistema.
-- Ejecutar UNA sola vez (si sale "Duplicate column name 'codigoVisible'", ya estaba hecho).
-- Requiere antes agregar_codigo_recuperacion.sql.
ALTER TABLE USUARIO ADD COLUMN codigoVisible VARCHAR(255) NULL;
