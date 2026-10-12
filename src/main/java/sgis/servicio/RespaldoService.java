package sgis.servicio;

import sgis.db.ConexionBD;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Respaldo automático de la base de datos (RNF-05).
 *
 * - Genera un respaldo CADA HORA mientras la aplicación está abierta. Al abrirla, si el último
 *   respaldo ya tiene una hora o más, crea uno enseguida; si es más reciente, espera a que se cumpla la hora.
 * - Cada respaldo es un archivo .sql completo (estructura + datos) que se puede restaurar
 *   pegándolo en la pestaña SQL de phpMyAdmin.
 * - Se guardan en la carpeta SGIS_Respaldos dentro de la carpeta personal del usuario,
 *   y se conservan los 168 más recientes (una semana de respaldos por hora).
 */
public class RespaldoService {

    // En orden de dependencia (primero las tablas que otras necesitan).
    private static final List<String> TABLAS = List.of(
            "CATEGORIA", "ROL", "USUARIO", "PRODUCTO", "MOVIMIENTO", "CLIENTE", "VENTA", "DETALLE_VENTA", "AUDITORIA");

    private static final Path CARPETA = Paths.get(System.getProperty("user.home"), "SGIS_Respaldos");
    private static final String PREFIJO = "respaldo_sgis_";
    private static final int MAX_RESPALDOS = 168;            // una semana de respaldos por hora
    private static final long MINUTOS_ENTRE_RESPALDOS = 60;  // un respaldo cada hora

    private static ScheduledExecutorService programador;

    private RespaldoService() {
    }

    /** Inicia los respaldos automáticos (se llama una sola vez al abrir la aplicación). */
    public static synchronized void iniciarProgramado() {
        if (programador != null) return;

        programador = Executors.newSingleThreadScheduledExecutor(tarea -> {
            Thread hilo = new Thread(tarea, "respaldo-sgis");
            hilo.setDaemon(true); // no impide que la aplicación se cierre
            return hilo;
        });

        // Si el último respaldo es reciente, el siguiente se hace cuando se cumpla la hora; si no, de inmediato.
        long espera = 0;
        Optional<LocalDateTime> ultimo = fechaUltimoRespaldo();
        if (ultimo.isPresent()) {
            long transcurridos = Duration.between(ultimo.get(), LocalDateTime.now()).toMinutes();
            espera = Math.max(0, MINUTOS_ENTRE_RESPALDOS - transcurridos);
        }
        programador.scheduleAtFixedRate(RespaldoService::respaldoPeriodico,
                espera, MINUTOS_ENTRE_RESPALDOS, TimeUnit.MINUTES);
    }

    private static void respaldoPeriodico() {
        try {
            Path archivo = generarRespaldo();
            System.out.println("Respaldo automático creado: " + archivo);
        } catch (Exception e) {
            // Un fallo en el respaldo nunca debe afectar el uso normal de la aplicación
            // (y capturarlo aquí evita que se cancelen los respaldos siguientes).
            System.out.println("No se pudo crear el respaldo automático: " + e.getMessage());
        }
    }

    /** Genera un respaldo completo ahora mismo y devuelve la ruta del archivo creado. */
    public static synchronized Path generarRespaldo() throws SQLException, IOException {
        Files.createDirectories(CARPETA);
        String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path destino = CARPETA.resolve(PREFIJO + marca + ".sql");

        try (Connection conexion = ConexionBD.obtenerConexion();
             BufferedWriter out = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {

            out.write("-- Respaldo de la base de datos SGIS generado el " + LocalDateTime.now() + "\n");
            out.write("-- Para restaurar: pegar este contenido en la pestaña SQL de phpMyAdmin.\n\n");
            out.write("CREATE DATABASE IF NOT EXISTS sgis_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n");
            out.write("USE sgis_db;\n");
            out.write("SET FOREIGN_KEY_CHECKS=0;\n\n");

            for (String tabla : TABLAS) {
                try {
                    volcarTabla(conexion, out, tabla);
                } catch (SQLException e) {
                    // Si una tabla aún no existe (ej. falta ejecutar crear_auditoria.sql), se omite y el resto se respalda.
                    if (!"42S02".equals(e.getSQLState())) {
                        throw e;
                    }
                    out.write("-- (la tabla " + tabla + " no existe todavía; se omitió)\n\n");
                }
            }

            out.write("SET FOREIGN_KEY_CHECKS=1;\n");
        }

        eliminarRespaldosAntiguos();
        return destino;
    }

    private static void volcarTabla(Connection conexion, BufferedWriter out, String tabla)
            throws SQLException, IOException {

        out.write("-- Tabla " + tabla + "\n");

        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SHOW CREATE TABLE " + tabla)) {
            if (rs.next()) {
                out.write("DROP TABLE IF EXISTS `" + tabla + "`;\n");
                out.write(rs.getString(2) + ";\n\n");
            }
        }

        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnas = meta.getColumnCount();

            StringBuilder nombres = new StringBuilder();
            for (int i = 1; i <= columnas; i++) {
                if (i > 1) nombres.append(", ");
                nombres.append('`').append(meta.getColumnName(i)).append('`');
            }

            while (rs.next()) {
                StringBuilder valores = new StringBuilder();
                for (int i = 1; i <= columnas; i++) {
                    if (i > 1) valores.append(", ");
                    valores.append(literalSql(rs.getObject(i)));
                }
                out.write("INSERT INTO `" + tabla + "` (" + nombres + ") VALUES (" + valores + ");\n");
            }
        }
        out.write("\n");
    }

    /** Convierte un valor de la base de datos al texto que se escribe en el archivo .sql. */
    private static String literalSql(Object valor) {
        if (valor == null) return "NULL";
        if (valor instanceof BigDecimal) return ((BigDecimal) valor).toPlainString();
        if (valor instanceof Number) return valor.toString();
        if (valor instanceof Boolean) return ((Boolean) valor) ? "1" : "0";

        DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (valor instanceof Timestamp) {
            return "'" + ((Timestamp) valor).toLocalDateTime().format(fechaHora) + "'";
        }
        if (valor instanceof LocalDateTime) {
            return "'" + ((LocalDateTime) valor).format(fechaHora) + "'";
        }

        String texto = valor.toString()
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\0", "\\0");
        return "'" + texto + "'";
    }

    private static List<Path> listarRespaldos() throws IOException {
        if (!Files.isDirectory(CARPETA)) return List.of();
        try (Stream<Path> archivos = Files.list(CARPETA)) {
            return archivos
                    .filter(p -> p.getFileName().toString().startsWith(PREFIJO)
                              && p.getFileName().toString().endsWith(".sql"))
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .collect(Collectors.toList()); // del más reciente al más antiguo
        }
    }

    private static void eliminarRespaldosAntiguos() throws IOException {
        List<Path> respaldos = listarRespaldos();
        for (int i = MAX_RESPALDOS; i < respaldos.size(); i++) {
            Files.deleteIfExists(respaldos.get(i));
        }
    }

    /** Fecha y hora del respaldo más reciente, o vacío si todavía no hay ninguno. */
    public static Optional<LocalDateTime> fechaUltimoRespaldo() {
        try {
            List<Path> respaldos = listarRespaldos();
            if (respaldos.isEmpty()) return Optional.empty();
            long milis = Files.getLastModifiedTime(respaldos.get(0)).toMillis();
            return Optional.of(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(milis), ZoneId.systemDefault()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static Path getCarpeta() {
        return CARPETA;
    }
}
