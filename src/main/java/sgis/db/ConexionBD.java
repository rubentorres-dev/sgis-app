package sgis.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase encargada de abrir la conexión JDBC hacia la base de datos MySQL (sgis_db).
 * Todas las clases DAO (ProductoDAO, MovimientoDAO, etc.) usarán este método
 * para obtener una conexión antes de ejecutar sus consultas SQL.
 */
public class ConexionBD {

    // --- Datos de conexión a la base de datos ---
    // Por defecto, con XAMPP, MySQL corre en localhost, puerto 3306,
    // usuario "root" y SIN contraseña. Ajusta esto si en tu XAMPP
    // le pusiste una contraseña al usuario root.
    private static final String URL = "jdbc:mysql://localhost:3306/sgis_db?useSSL=false&serverTimezone=UTC";
    private static final String USUARIO = "root";
    private static final String CONTRASENA = "";

    /**
     * Abre y devuelve una nueva conexión a la base de datos.
     * Quien llame a este método es responsable de cerrarla (usar try-with-resources).
     */
    public static Connection obtenerConexion() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USUARIO, CONTRASENA);
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    /** Mensaje para cuando MySQL no está encendido (el caso más común al abrir la aplicación). */
    public static final String MENSAJE_SIN_CONEXION =
            "No se pudo conectar con la base de datos. Abre XAMPP, pulsa Start en MySQL y vuelve a intentarlo.";

    /** Cambia los errores técnicos de conexión por mensajes que se entienden. */
    private static SQLException traducir(SQLException e) {
        String estado = e.getSQLState();
        if (estado != null && estado.startsWith("08")) {
            return new SQLException(MENSAJE_SIN_CONEXION, estado, e.getErrorCode(), e);
        }
        if (e.getErrorCode() == 1049) { // base de datos desconocida
            return new SQLException("MySQL está encendido, pero no existe la base de datos sgis_db. "
                    + "Impórtala en phpMyAdmin con el archivo sgis_schema.sql.", estado, e.getErrorCode(), e);
        }
        if (e.getErrorCode() == 1045) { // acceso denegado
            return new SQLException("MySQL rechazó el usuario o la contraseña de conexión. "
                    + "Revisa los datos de ConexionBD.java.", estado, e.getErrorCode(), e);
        }
        return e;
    }

    /** @return null si la base de datos responde, o un mensaje que explica el problema. */
    public static String comprobarConexion() {
        try (Connection conexion = obtenerConexion()) {
            return null;
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    /**
     * Método de prueba: solo verifica que la conexión se pueda abrir y cerrar
     * correctamente. Útil para confirmar que la configuración es correcta
     * antes de seguir construyendo el resto de la aplicación.
     */
    public static void probarConexion() {
        try (Connection conexion = obtenerConexion()) {
            if (conexion != null && !conexion.isClosed()) {
                System.out.println("Conexión exitosa a la base de datos sgis_db");
            }
        } catch (SQLException e) {
            System.out.println("ERROR al conectar con la base de datos: " + e.getMessage());
        }
    }
}
