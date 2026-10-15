USE sgis_db;

-- Numero de telefono de los clientes y de los usuarios del sistema.
-- Se guarda solo con numeros (7 a 10 digitos). No es unico: una familia puede compartir telefono.
-- Las filas que ya existen quedan sin telefono (NULL) hasta que se completen.
-- Ejecutar UNA sola vez (si sale "Duplicate column name 'telefono'", ya estaba hecho).
-- Requiere antes agregar_cedula.sql.
ALTER TABLE CLIENTE ADD COLUMN telefono VARCHAR(10) NULL;
ALTER TABLE USUARIO ADD COLUMN telefono VARCHAR(10) NULL;
