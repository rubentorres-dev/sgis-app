USE sgis_db;

-- Agrega el método de pago a cada venta (efectivo, tarjeta o transferencia).
-- Las ventas que ya existen quedan registradas como "efectivo" por defecto.
ALTER TABLE VENTA
ADD COLUMN metodoPago ENUM('efectivo', 'tarjeta', 'transferencia') NOT NULL DEFAULT 'efectivo';
