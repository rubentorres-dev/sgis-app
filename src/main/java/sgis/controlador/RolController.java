package sgis.controlador;

import sgis.db.RolDAO;
import sgis.modelo.Rol;
import sgis.servicio.AuditoriaService;
import sgis.util.Permisos;
import sgis.util.Sesion;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlador de la administración de roles (Configuración > Roles).
 * Los métodos que modifican datos devuelven null si salió bien, o un mensaje de error.
 */
public class RolController {

    /** Pantallas que se pueden dar a un rol. Configuración es solo para el Administrador. */
    private static final List<String> PANTALLAS_ASIGNABLES = Arrays.asList("PRODUCTOS", "VENTAS", "REPORTES", "USUARIOS");

    /** Mensaje para cuando todavía no se ejecutó el script que crea la tabla de roles. */
    public static final String MENSAJE_SIN_TABLA =
            "Falta crear la tabla de roles en la base de datos. En phpMyAdmin, pestaña SQL, ejecuta el archivo "
            + "crear_roles.sql (está en la carpeta base_de_datos).";

    private final RolDAO rolDAO = new RolDAO();

    private String errorDe(SQLException e, String prefijo) {
        if ("42S02".equals(e.getSQLState())) {
            return MENSAJE_SIN_TABLA;
        }
        if ("42S22".equals(e.getSQLState())) { // falta la columna activo (tabla creada con el script anterior)
            return "Falta agregar la columna activo a la tabla de roles. En phpMyAdmin, pestaña SQL, ejecuta: "
                    + "ALTER TABLE ROL ADD COLUMN activo TINYINT(1) NOT NULL DEFAULT 1;";
        }
        return prefijo + e.getMessage();
    }

    public List<Rol> obtenerRoles() {
        try {
            return rolDAO.listar();
        } catch (SQLException e) {
            if ("42S02".equals(e.getSQLState())) { // la tabla no existe
                throw new RuntimeException(MENSAJE_SIN_TABLA, e);
            }
            throw new RuntimeException("Error al consultar los roles: " + e.getMessage(), e);
        }
    }

    public String crearRol(String nombre, String descripcion, Set<String> pantallas, boolean puedeEditar) {
        if (nombre == null || nombre.trim().length() < 3 || nombre.trim().length() > 60) {
            return "El nombre del rol debe tener entre 3 y 60 caracteres.";
        }
        try {
            if (rolDAO.existeNombre(nombre.trim())) {
                return "Ya existe un rol con ese nombre.";
            }
            Rol rol = new Rol();
            rol.setNombre(nombre.trim());
            rol.setDescripcion(limpiar(descripcion));
            rol.setPantallas(normalizar(pantallas));
            rol.setPuedeEditar(puedeEditar);
            rolDAO.crear(rol);

            Permisos.recargar();
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Creó rol", detalle(rol));
            return null;
        } catch (SQLException e) {
            return errorDe(e, "Error al crear el rol: ");
        }
    }

    public String actualizarRol(Rol rol, String descripcion, Set<String> pantallas, boolean puedeEditar) {
        if (rol.isEsSistema()) {
            return "El rol " + rol.getNombre() + " es del sistema y no se puede modificar.";
        }
        try {
            rol.setDescripcion(limpiar(descripcion));
            rol.setPantallas(normalizar(pantallas));
            rol.setPuedeEditar(puedeEditar);
            rolDAO.actualizar(rol);

            Permisos.recargar();
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Editó rol", detalle(rol));
            return null;
        } catch (SQLException e) {
            return errorDe(e, "Error al actualizar el rol: ");
        }
    }

    /** Activa o desactiva un rol. Un rol desactivado no se puede asignar y sus usuarios no pueden iniciar sesión. */
    public String cambiarEstado(Rol rol, boolean activar) {
        if (rol.isEsSistema()) {
            return "El rol " + rol.getNombre() + " es del sistema y no se puede desactivar: "
                    + "sin él nadie podría administrar el sistema.";
        }
        try {
            rolDAO.cambiarActivo(rol.getIdRol(), activar);

            Permisos.recargar();
            AuditoriaService.registrar(Sesion.getUsuarioActual(),
                    activar ? "Activó rol" : "Desactivó rol", rol.getNombre());
            return null;
        } catch (SQLException e) {
            return errorDe(e, "Error al cambiar el estado del rol: ");
        }
    }

    public String eliminarRol(Rol rol) {
        if (rol.isEsSistema()) {
            return "El rol " + rol.getNombre() + " es del sistema y no se puede eliminar: "
                    + "sin él nadie podría administrar el sistema.";
        }
        try {
            int usuarios = rolDAO.contarUsuarios(rol.getNombre());
            if (usuarios > 0) {
                return "No se puede eliminar: " + usuarios + " usuario(s) tienen este rol. "
                        + "Cámbiales el rol primero.";
            }
            rolDAO.eliminar(rol.getIdRol());

            Permisos.recargar();
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Eliminó rol", rol.getNombre());
            return null;
        } catch (SQLException e) {
            return errorDe(e, "Error al eliminar el rol: ");
        }
    }

    /** El Panel Principal siempre está; solo se aceptan las pantallas que se pueden asignar, en orden fijo. */
    private Set<String> normalizar(Set<String> elegidas) {
        Set<String> resultado = new LinkedHashSet<>();
        resultado.add("PANEL");
        for (String pantalla : PANTALLAS_ASIGNABLES) {
            if (elegidas != null && elegidas.contains(pantalla)) {
                resultado.add(pantalla);
            }
        }
        return resultado;
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private String detalle(Rol rol) {
        return rol.getNombre() + " · pantallas: " + Permisos.pantallasAmigables(rol)
                + " · editar y borrar: " + (rol.isPuedeEditar() ? "sí" : "no");
    }
}
