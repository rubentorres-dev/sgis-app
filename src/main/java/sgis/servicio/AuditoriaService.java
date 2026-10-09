package sgis.servicio;

import sgis.db.AuditoriaDAO;
import sgis.modelo.RegistroAuditoria;
import sgis.modelo.Usuario;

import java.sql.SQLException;
import java.util.List;

/**
 * Registro de auditoría: deja constancia de quién hizo qué en el sistema.
 *
 * Registrar NUNCA debe impedir la operación que se está haciendo: si por cualquier motivo no se
 * puede guardar el registro (por ejemplo, falta ejecutar crear_auditoria.sql), la acción continúa
 * normalmente y solo se avisa en la consola.
 */
public final class AuditoriaService {

    private static final AuditoriaDAO DAO = new AuditoriaDAO();
    private static boolean avisoMostrado = false;

    private AuditoriaService() {
    }

    /**
     * @param actor quien hizo la acción (null si no hay sesión, por ejemplo un intento fallido de acceso).
     * @param accion qué hizo, en pocas palabras (ej. "Creó usuario").
     * @param detalle sobre qué (ej. el nombre del usuario o producto); puede ser null.
     */
    public static void registrar(Usuario actor, String accion, String detalle) {
        try {
            DAO.registrar(actor == null ? null : actor.getIdUsuario(), textoDe(actor), accion, detalle);
        } catch (Exception e) {
            if (!avisoMostrado) {
                avisoMostrado = true;
                System.out.println("No se pudo guardar el registro de auditoría: " + e.getMessage());
            }
        }
    }

    public static List<RegistroAuditoria> listar() {
        try {
            return DAO.listar();
        } catch (SQLException e) {
            if ("42S02".equals(e.getSQLState())) { // la tabla no existe
                throw new RuntimeException("Falta crear la tabla de auditoría: ejecuta crear_auditoria.sql en phpMyAdmin.", e);
            }
            throw new RuntimeException("Error al consultar el registro de auditoría: " + e.getMessage(), e);
        }
    }

    private static String textoDe(Usuario actor) {
        if (actor == null) {
            return "(sin sesión)";
        }
        return actor.getNombre() + " (" + actor.getRol() + ")";
    }
}
