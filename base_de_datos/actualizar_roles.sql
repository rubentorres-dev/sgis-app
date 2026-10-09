USE sgis_db;

-- Deja solo dos roles en el sistema:
--   "Administrador"             (antes: Administrador y Responsable de la empresa)
--   "Administrador de inventario" (antes: Encargado de inventario y Empleado)
-- Los usuarios que ya existen se convierten automaticamente. Ejecutar UNA sola vez.

-- 1) Permitir temporalmente el valor nuevo junto a los antiguos
ALTER TABLE USUARIO
    MODIFY rol ENUM('Administrador', 'Encargado de inventario', 'Empleado',
                    'Responsable de la empresa', 'Administrador de inventario') NOT NULL;

-- 2) Convertir los usuarios existentes
UPDATE USUARIO SET rol = 'Administrador'
    WHERE rol = 'Responsable de la empresa';
UPDATE USUARIO SET rol = 'Administrador de inventario'
    WHERE rol IN ('Encargado de inventario', 'Empleado');

-- 3) Dejar unicamente los dos roles
ALTER TABLE USUARIO
    MODIFY rol ENUM('Administrador', 'Administrador de inventario') NOT NULL;
