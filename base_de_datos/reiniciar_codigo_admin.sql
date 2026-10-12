USE sgis_db;

-- Borra el codigo de recuperacion guardado del usuario admin.
-- La proxima vez que admin inicie sesion, el programa mostrara un codigo NUEVO de 6 digitos (una sola vez).
-- Sirve si el codigo se perdio o si todavia no se ha visto. No cambia la contrasena.
-- (Requiere que la columna codigoRecuperacion exista: agregar_codigo_recuperacion.sql)
UPDATE USUARIO SET codigoRecuperacion = NULL WHERE usuarioLogin = 'admin';
