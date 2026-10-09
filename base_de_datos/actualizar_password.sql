USE sgis_db;

UPDATE USUARIO
SET contrasena = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'
WHERE usuarioLogin = 'admin';

-- Con esto, en la pantalla de login de la app puedes entrar con:
-- Usuario: admin
-- Contraseña: admin123
