package sgis.util;

import java.sql.SQLException;

/** Convierte los errores de «falta una columna» de la base de datos en un aviso claro de qué script ejecutar. */
public final class MensajesBD {

    private MensajesBD() {
    }

    /** @return el aviso si el error es por falta de la columna cédula o teléfono; null en cualquier otro caso. */
    public static String scriptFaltante(SQLException e) {
        if (!"42S22".equals(e.getSQLState()) || e.getMessage() == null) {
            return null;
        }
        String mensaje = e.getMessage().toLowerCase();
        if (mensaje.contains("telefono")) {
            return "Falta ejecutar el script agregar_telefono.sql en la base de datos.";
        }
        if (mensaje.contains("cedula")) {
            return "Falta ejecutar el script agregar_cedula.sql en la base de datos.";
        }
        return null;
    }
}
