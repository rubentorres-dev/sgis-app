USE sgis_db;

-- Registro de auditoria: quien hizo algo en el sistema (inicios de sesion, cambios de usuarios,
-- productos, categorias, clientes, contrasenas vistas, etc.).
-- Guarda el nombre de quien lo hizo tal como era en ese momento, asi el historial no se pierde
-- aunque despues se cambie o se elimine ese usuario. Ejecutar UNA sola vez.
CREATE TABLE IF NOT EXISTS AUDITORIA (
    idAuditoria INT AUTO_INCREMENT PRIMARY KEY,
    fecha       DATETIME     NOT NULL,
    idUsuario   INT          NULL,
    usuario     VARCHAR(150) NOT NULL,
    accion      VARCHAR(60)  NOT NULL,
    detalle     VARCHAR(255) NULL,
    INDEX idx_auditoria_fecha (fecha)
);
