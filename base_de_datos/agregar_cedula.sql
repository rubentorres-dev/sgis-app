USE sgis_db;

-- Cedula de ciudadania de los clientes y de los usuarios del sistema.
-- Se guarda solo con numeros (6 a 10 digitos) y no se puede repetir. Las filas que ya existen quedan sin cedula (NULL)
-- hasta que se completen: un cliente la recibe en su proxima venta, y un usuario cuando un Administrador lo edita.
-- Ejecutar UNA sola vez (si sale "Duplicate column name 'cedula'", ya estaba hecho).
ALTER TABLE CLIENTE ADD COLUMN cedula VARCHAR(10) NULL;
ALTER TABLE CLIENTE ADD UNIQUE KEY uk_cliente_cedula (cedula);
ALTER TABLE USUARIO ADD COLUMN cedula VARCHAR(10) NULL;
ALTER TABLE USUARIO ADD UNIQUE KEY uk_usuario_cedula (cedula);
