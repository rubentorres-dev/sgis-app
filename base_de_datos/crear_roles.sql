USE sgis_db;

-- Roles configurables: el Administrador crea roles desde la lista de roles de un usuario o desde
-- Configuracion > Roles, y elige a que pantallas puede entrar cada uno.
-- Ejecutar UNA sola vez (se puede repetir sin problema).

CREATE TABLE IF NOT EXISTS ROL (
    idRol       INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(60)  NOT NULL UNIQUE,
    descripcion VARCHAR(200) NULL,
    pantallas   VARCHAR(200) NOT NULL,
    puedeEditar TINYINT(1)   NOT NULL DEFAULT 0,
    esSistema   TINYINT(1)   NOT NULL DEFAULT 0,
    activo      TINYINT(1)   NOT NULL DEFAULT 1
);

INSERT IGNORE INTO ROL (nombre, descripcion, pantallas, puedeEditar, esSistema, activo) VALUES
    ('Administrador',
     'Acceso total, incluida Configuracion. Es el unico que puede dar permisos y crear roles.',
     'PANEL,PRODUCTOS,VENTAS,REPORTES,USUARIOS,CONFIGURACION', 1, 1, 1),
    ('Administrador de inventario',
     'Atiende el dia a dia: productos, ventas y clientes, sin editar ni borrar.',
     'PANEL,PRODUCTOS,VENTAS,USUARIOS', 0, 0, 1);

ALTER TABLE USUARIO MODIFY rol VARCHAR(60) NOT NULL;
