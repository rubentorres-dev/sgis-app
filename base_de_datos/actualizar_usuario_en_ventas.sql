USE sgis_db;

-- Guarda qué usuario del sistema registró cada venta (para saber quién vendió: el jefe o un empleado).
-- Las ventas que ya existen quedan con usuario vacío (NULL) y se muestran como "venta anterior".
-- Ejecutar UNA sola vez.
ALTER TABLE VENTA
    ADD COLUMN idUsuario INT NULL,
    ADD CONSTRAINT fk_venta_usuario FOREIGN KEY (idUsuario) REFERENCES USUARIO(idUsuario);
