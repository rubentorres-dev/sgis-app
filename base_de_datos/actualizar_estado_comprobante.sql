USE sgis_db;

-- El estado del comprobante de cada venta pasa a tener un valor mas:
--   completado = se entrego el comprobante en PDF
-- (los que ya existian siguen igual: pendiente, enviado por correo, fallido).
-- Ejecutar UNA sola vez.
ALTER TABLE VENTA
    MODIFY estadoEnvio ENUM('pendiente', 'enviado', 'fallido', 'completado') NOT NULL DEFAULT 'pendiente';
