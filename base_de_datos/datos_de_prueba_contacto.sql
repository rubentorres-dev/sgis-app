USE sgis_db;

-- OPCIONAL (no forma parte de la instalacion): rellena con valores de PRUEBA la cedula y el telefono
-- de los clientes y de los usuarios que todavia no los tienen, para que las pantallas no muestren
-- "Sin cedula". Son numeros inventados (no pertenecen a personas reales) y distintos entre si.
-- NO toca al usuario protegido (admin): su cedula y telefono se ponen aparte, con una sola sentencia.
-- Requiere antes agregar_cedula.sql y agregar_telefono.sql.
UPDATE CLIENTE SET cedula   = CAST(1000000000 + idCliente * 7919   AS CHAR) WHERE cedula   IS NULL;
UPDATE CLIENTE SET telefono = CAST(3100000000 + idCliente * 104729 AS CHAR) WHERE telefono IS NULL;
UPDATE USUARIO SET cedula   = CAST(1010000000 + idUsuario * 7919   AS CHAR) WHERE cedula   IS NULL AND protegido = 0;
UPDATE USUARIO SET telefono = CAST(3200000000 + idUsuario * 104729 AS CHAR) WHERE telefono IS NULL AND protegido = 0;
