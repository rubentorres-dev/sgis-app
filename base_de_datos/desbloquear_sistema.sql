USE sgis_db;

-- DESBLOQUEO DE EMERGENCIA del sistema (solo si ningun administrador puede desbloquearlo desde la pantalla del candado).
-- Cuando el sistema detecta un ataque de fuerza bruta se bloquea y solo lo desbloquea un Administrador con sus
-- credenciales. Este script hace lo mismo desde la base de datos: deja el registro del desbloqueo manual.
-- Ejecutalo UNA vez cuando haga falta. No cambia contrasenas ni usuarios.
INSERT INTO AUDITORIA (fecha, idUsuario, usuario, accion, detalle)
VALUES (NOW(), NULL, 'Sistema', 'Sistema desbloqueado por un administrador', 'Desbloqueo manual desde la base de datos');
