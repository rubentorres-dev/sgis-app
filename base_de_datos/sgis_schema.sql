-- =========================================================
-- SGIS - Sistema de Gestión de Inventario para Supermercado
-- Script de creación de base de datos
-- Motor: MySQL / MariaDB
-- Basado en el Modelo Entidad-Relación consolidado (7 entidades)
-- =========================================================

CREATE DATABASE IF NOT EXISTS sgis_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE sgis_db;

-- ---------------------------------------------------------
-- Tabla: CATEGORIA
-- ---------------------------------------------------------
CREATE TABLE CATEGORIA (
    idCategoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE
);

-- ---------------------------------------------------------
-- Tabla: USUARIO
-- ---------------------------------------------------------
CREATE TABLE USUARIO (
    idUsuario     INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(150) NOT NULL,
    rol           ENUM('Administrador', 'Encargado de inventario', 'Empleado', 'Responsable de la empresa') NOT NULL,
    usuarioLogin  VARCHAR(50)  NOT NULL UNIQUE,
    contrasena    VARCHAR(255) NOT NULL,
    estado        ENUM('activo', 'inactivo') NOT NULL DEFAULT 'activo'
);

-- ---------------------------------------------------------
-- Tabla: PRODUCTO
-- ---------------------------------------------------------
CREATE TABLE PRODUCTO (
    idProducto          INT AUTO_INCREMENT PRIMARY KEY,
    nombre              VARCHAR(150) NOT NULL,
    descripcion         VARCHAR(255),
    precio              DECIMAL(10,2) NOT NULL,
    cantidadDisponible  INT NOT NULL DEFAULT 0,
    estado              ENUM('activo', 'inactivo') NOT NULL DEFAULT 'activo',
    idCategoria         INT NOT NULL,
    CONSTRAINT fk_producto_categoria
        FOREIGN KEY (idCategoria) REFERENCES CATEGORIA(idCategoria)
);

-- ---------------------------------------------------------
-- Tabla: MOVIMIENTO
-- ---------------------------------------------------------
CREATE TABLE MOVIMIENTO (
    idMovimiento INT AUTO_INCREMENT PRIMARY KEY,
    tipo         ENUM('entrada', 'salida') NOT NULL,
    cantidad     INT NOT NULL,
    fecha        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idProducto   INT NOT NULL,
    idUsuario    INT NOT NULL,
    CONSTRAINT fk_movimiento_producto
        FOREIGN KEY (idProducto) REFERENCES PRODUCTO(idProducto),
    CONSTRAINT fk_movimiento_usuario
        FOREIGN KEY (idUsuario) REFERENCES USUARIO(idUsuario)
);

-- ---------------------------------------------------------
-- Tabla: CLIENTE
-- ---------------------------------------------------------
CREATE TABLE CLIENTE (
    idCliente INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(150) NOT NULL,
    correo    VARCHAR(150) NOT NULL
);

-- ---------------------------------------------------------
-- Tabla: VENTA
-- ---------------------------------------------------------
CREATE TABLE VENTA (
    idVenta     INT AUTO_INCREMENT PRIMARY KEY,
    fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estadoEnvio ENUM('pendiente', 'enviado', 'fallido') NOT NULL DEFAULT 'pendiente',
    idCliente   INT NOT NULL,
    CONSTRAINT fk_venta_cliente
        FOREIGN KEY (idCliente) REFERENCES CLIENTE(idCliente)
);

-- ---------------------------------------------------------
-- Tabla: DETALLE_VENTA
-- ---------------------------------------------------------
CREATE TABLE DETALLE_VENTA (
    idDetalleVenta INT AUTO_INCREMENT PRIMARY KEY,
    cantidad       INT NOT NULL,
    precioUnitario DECIMAL(10,2) NOT NULL,
    idVenta        INT NOT NULL,
    idProducto     INT NOT NULL,
    CONSTRAINT fk_detalleventa_venta
        FOREIGN KEY (idVenta) REFERENCES VENTA(idVenta),
    CONSTRAINT fk_detalleventa_producto
        FOREIGN KEY (idProducto) REFERENCES PRODUCTO(idProducto)
);

-- ---------------------------------------------------------
-- Datos de prueba (opcional, para probar el backend)
-- ---------------------------------------------------------
INSERT INTO CATEGORIA (nombre) VALUES
('Granos'), ('Lácteos'), ('Aseo'), ('Bebidas');

INSERT INTO USUARIO (nombre, rol, usuarioLogin, contrasena) VALUES
('Rubén Torres', 'Administrador', 'admin', 'cambia_esta_clave'),
('Daniel Pardo', 'Encargado de inventario', 'daniel', 'cambia_esta_clave');

INSERT INTO PRODUCTO (nombre, descripcion, precio, cantidadDisponible, idCategoria) VALUES
('Arroz Diana 1kg', 'Arroz blanco', 5500, 120, 1),
('Leche Alpina 1L', 'Leche entera', 4400, 32, 2),
('Detergente Ariel 850g', 'Detergente en polvo', 12500, 68, 3);
