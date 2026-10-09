USE sgis_db;

-- Usuario protegido ("anclado"): el de Ruben Torres (usuario admin).
-- Nadie puede editarlo, desactivarlo, eliminarlo ni ver o cambiar su contrasena desde el sistema.
-- Ejecutar UNA sola vez (si sale "Duplicate column name 'protegido'", ya estaba hecho).
ALTER TABLE USUARIO ADD COLUMN protegido TINYINT(1) NOT NULL DEFAULT 0;

UPDATE USUARIO SET protegido = 1 WHERE usuarioLogin = 'admin';
