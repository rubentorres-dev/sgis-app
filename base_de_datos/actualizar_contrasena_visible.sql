USE sgis_db;

-- Guarda, ademas del hash que se usa para iniciar sesion, una COPIA CIFRADA de cada contrasena
-- para que un Administrador pueda verla con el ojo en Configuracion > Usuarios del sistema.
-- La llave para descifrarla NO esta en la base de datos (queda en un archivo del computador).
-- Ejecutar UNA sola vez.
ALTER TABLE USUARIO ADD COLUMN contrasenaVisible VARCHAR(255) NULL;
