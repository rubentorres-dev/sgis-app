package sgis.controlador;

import sgis.db.ConexionBD;
import sgis.db.UsuarioDAO;
import sgis.modelo.Usuario;
import sgis.servicio.AuditoriaService;
import sgis.util.CifradoContrasena;
import sgis.util.ControlIntentosLogin;
import sgis.util.Permisos;
import sgis.util.Sesion;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Controlador de autenticación (login) y administración de los usuarios del sistema
 * (las cuentas con las que se entra a la aplicación, cada una con su rol).
 *
 * Todos los métodos de administración devuelven null si salió bien, o un mensaje
 * de error listo para mostrar en pantalla.
 */
public class UsuarioController {

    private static final Pattern LOGIN_VALIDO = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final int LARGO_MINIMO_CONTRASENA = 6;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    /** Administrador que autorizó la última creación de usuario desde el inicio de sesión (no hay sesión iniciada). */
    private Usuario autorizador;

    // ---------------------------------------------------------------
    // Inicio de sesión
    // ---------------------------------------------------------------

    /**
     * Intenta iniciar sesión.
     * @return el Usuario autenticado, o null si el login/contraseña son incorrectos.
     */
    public Usuario iniciarSesion(String usuarioLogin, String contrasena) {
        try {
            Usuario usuario = usuarioDAO.autenticar(usuarioLogin, contrasena);
            if (usuario != null && !Permisos.rolActivo(usuario.getRol())) {
                // Las credenciales son correctas, pero su rol fue desactivado: no cuenta como intento fallido.
                AuditoriaService.registrar(usuario, "Acceso denegado: rol desactivado", usuario.getRol());
                throw new RuntimeException("Tu rol \"" + usuario.getRol()
                        + "\" está desactivado. Comunícate con el administrador.");
            }
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("Error al validar el inicio de sesión: " + e.getMessage(), e);
        }
    }

    /** @return null si la base de datos responde, o un mensaje que explica qué hacer (ej. encender MySQL). */
    public String comprobarConexion() {
        return ConexionBD.comprobarConexion();
    }

    /**
     * Verifica que las credenciales sean de un Administrador
     * (se usa para autorizar la creación de usuarios desde la pantalla de login).
     */
    public String autorizarAdministrativo(String login, String contrasena) {
        if (login == null || login.isBlank() || contrasena == null || contrasena.isEmpty()) {
            return "Escribe el usuario y la contraseña de un administrador para autorizar.";
        }
        long segundos = ControlIntentosLogin.segundosDeBloqueoRestantes(login);
        if (segundos > 0) {
            return "Autorización bloqueada por demasiados intentos fallidos. Intenta de nuevo en "
                    + (segundos / 60) + " min " + (segundos % 60) + " s.";
        }
        try {
            Usuario usuario = usuarioDAO.autenticar(login, contrasena);
            if (usuario != null && Permisos.esAdministrativo(usuario)) {
                ControlIntentosLogin.registrarExito(login);
                autorizador = usuario;
                return null;
            }
            ControlIntentosLogin.registrarFallo(login);
            return "Esas credenciales no pertenecen a un Administrador.";
        } catch (SQLException e) {
            return "Error al verificar la autorización: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------
    // Administración de usuarios
    // ---------------------------------------------------------------

    public List<Usuario> obtenerUsuarios() {
        try {
            return usuarioDAO.listarUsuarios();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los usuarios: " + e.getMessage(), e);
        }
    }

    public String registrarUsuario(String nombre, String rol, String login, String contrasena) {
        if (nombre == null || nombre.isBlank()) {
            return "El nombre es obligatorio.";
        }
        if (!rolValido(rol)) {
            return "Selecciona un rol válido.";
        }
        if (!Permisos.rolActivo(rol)) {
            return "Ese rol está desactivado. Actívalo en Configuración > Roles o elige otro.";
        }
        if (login == null || !LOGIN_VALIDO.matcher(login.trim()).matches()) {
            return "El usuario debe tener entre 3 y 30 caracteres (letras, números, punto, guion o guion bajo, sin espacios).";
        }
        String errorContrasena = validarContrasena(contrasena);
        if (errorContrasena != null) {
            return errorContrasena;
        }

        try {
            if (usuarioDAO.existeLogin(login.trim())) {
                return "Ya existe un usuario con ese nombre de usuario.";
            }
            Usuario u = new Usuario();
            u.setNombre(nombre.trim());
            u.setRol(rol);
            u.setUsuarioLogin(login.trim());
            u.setEstado("activo");
            usuarioDAO.registrarUsuario(u, contrasena);
            boolean desdeLogin = Sesion.getUsuarioActual() == null;
            AuditoriaService.registrar(actor(), "Creó usuario",
                    u.getNombre() + " · usuario " + u.getUsuarioLogin() + " · rol " + u.getRol()
                    + (desdeLogin ? " (autorizado desde el inicio de sesión)" : ""));
            return null;
        } catch (SQLException e) {
            return "Error al registrar el usuario: " + e.getMessage();
        }
    }

    /** Cambia el nombre y el rol de un usuario. */
    public String actualizarUsuario(Usuario editado, Usuario quienEdita) {
        if (editado.getNombre() == null || editado.getNombre().isBlank()) {
            return "El nombre es obligatorio.";
        }
        if (!rolValido(editado.getRol())) {
            return "Selecciona un rol válido.";
        }
        try {
            List<Usuario> todos = usuarioDAO.listarUsuarios();
            Usuario actual = buscar(todos, editado.getIdUsuario());
            if (actual == null) {
                return "El usuario ya no existe.";
            }
            if (actual.isProtegido()) {
                return rechazarProtegido(actual, "Editar");
            }

            boolean pierdePrivilegio = Permisos.esAdministrativo(actual.getRol())
                    && !Permisos.esAdministrativo(editado.getRol());
            if (pierdePrivilegio) {
                if (quienEdita != null && quienEdita.getIdUsuario() == actual.getIdUsuario()) {
                    return "No puedes quitarte a ti mismo el rol de Administrador.";
                }
                if ("activo".equals(actual.getEstado()) && otrosAdministrativosActivos(todos, actual.getIdUsuario()) == 0) {
                    return "Debe quedar al menos un Administrador activo.";
                }
            }

            usuarioDAO.actualizarUsuario(editado);
            AuditoriaService.registrar(actor(), "Editó usuario",
                    editado.getNombre() + " · usuario " + actual.getUsuarioLogin()
                    + " · rol " + actual.getRol() + " → " + editado.getRol());
            return null;
        } catch (SQLException e) {
            return "Error al actualizar el usuario: " + e.getMessage();
        }
    }

    /** El Administrador escribe una contraseña nueva para un usuario (formulario de edición). */
    public String cambiarContrasena(int idUsuario, String nuevaContrasena) {
        try {
            Usuario objetivo = buscar(usuarioDAO.listarUsuarios(), idUsuario);
            if (objetivo != null && objetivo.isProtegido()) {
                return rechazarProtegido(objetivo, "Cambiar la contraseña de");
            }
        } catch (SQLException e) {
            return "Error al consultar el usuario: " + e.getMessage();
        }
        String error = guardarContrasena(idUsuario, nuevaContrasena);
        if (error == null) {
            AuditoriaService.registrar(actor(), "Cambió la contraseña de un usuario", descripcionDe(idUsuario));
        }
        return error;
    }

    /**
     * Cambia la contraseña de un usuario exigiendo la clave temporal (la que generó el botón Clave).
     * La clave temporal debe ser la contraseña actual del usuario; si no coincide, no se cambia nada.
     */
    public String cambiarContrasenaConClaveTemporal(Usuario objetivo, String claveTemporal, String contrasenaNueva) {
        if (objetivo.isProtegido()) {
            return rechazarProtegido(objetivo, "Cambiar la contraseña de");
        }
        if (claveTemporal == null || claveTemporal.isEmpty()) {
            return "Pega la clave temporal. Si no tienes una, pulsa 🔑 Clave en la tabla de usuarios.";
        }
        String errorNueva = validarContrasena(contrasenaNueva);
        if (errorNueva != null) {
            return errorNueva;
        }
        if (contrasenaNueva.equals(claveTemporal)) {
            return "La contraseña nueva debe ser distinta a la clave temporal.";
        }
        try {
            if (!usuarioDAO.contrasenaCoincide(objetivo.getIdUsuario(), claveTemporal)) {
                return "La clave temporal no es correcta. Pulsa 🔑 Clave, copia la nueva y pégala otra vez.";
            }
            usuarioDAO.cambiarContrasena(objetivo.getIdUsuario(), contrasenaNueva);
            AuditoriaService.registrar(actor(), "Cambió la contraseña de un usuario",
                    objetivo.getNombre() + " · usuario " + objetivo.getUsuarioLogin() + " (con clave temporal)");
            return null;
        } catch (SQLException e) {
            return "Error al cambiar la contraseña: " + e.getMessage();
        }
    }

    /** El Administrador genera una contraseña temporal para un usuario (botón Clave). */
    public String restablecerContrasenaTemporal(Usuario objetivo, String contrasenaTemporal) {
        if (objetivo.isProtegido()) {
            return rechazarProtegido(objetivo, "Generar una clave temporal para");
        }
        String error = guardarContrasena(objetivo.getIdUsuario(), contrasenaTemporal);
        if (error == null) {
            AuditoriaService.registrar(actor(), "Generó una contraseña temporal",
                    objetivo.getNombre() + " · usuario " + objetivo.getUsuarioLogin());
        }
        return error;
    }

    private String guardarContrasena(int idUsuario, String nuevaContrasena) {
        String error = validarContrasena(nuevaContrasena);
        if (error != null) {
            return error;
        }
        try {
            usuarioDAO.cambiarContrasena(idUsuario, nuevaContrasena);
            return null;
        } catch (SQLException e) {
            return "Error al cambiar la contraseña: " + e.getMessage();
        }
    }

    /**
     * Permite que un usuario cambie su propia contraseña, si conoce la actual.
     * Cada intento fallido cuenta para el bloqueo (5 intentos). Quien no recuerda su contraseña
     * debe pedirle al administrador que la restablezca desde Configuración.
     */
    public String cambiarContrasenaPropia(String login, String contrasenaActual, String contrasenaNueva) {
        if (login == null || login.isBlank() || contrasenaActual == null || contrasenaActual.isEmpty()) {
            return "Escribe tu usuario y tu contraseña actual.";
        }
        long segundos = ControlIntentosLogin.segundosDeBloqueoRestantes(login);
        if (segundos > 0) {
            return "Demasiados intentos fallidos. Comunícate con el administrador o intenta de nuevo en "
                    + (segundos / 60) + " min " + (segundos % 60) + " s.";
        }
        String errorNueva = validarContrasena(contrasenaNueva);
        if (errorNueva != null) {
            return errorNueva;
        }
        if (contrasenaNueva.equals(contrasenaActual)) {
            return "La contraseña nueva debe ser distinta a la actual.";
        }

        try {
            Usuario usuario = usuarioDAO.autenticar(login, contrasenaActual);
            if (usuario == null) {
                int restantes = ControlIntentosLogin.registrarFallo(login);
                if (restantes == 0) {
                    return "Usuario o contraseña actual incorrectos. Se bloqueó el acceso por "
                            + ControlIntentosLogin.MINUTOS_BLOQUEO + " minutos. Comunícate con el administrador.";
                }
                return "Usuario o contraseña actual incorrectos. Intentos restantes: " + restantes + ".";
            }
            usuarioDAO.cambiarContrasena(usuario.getIdUsuario(), contrasenaNueva);
            ControlIntentosLogin.registrarExito(login);
            AuditoriaService.registrar(usuario, "Cambió su propia contraseña", null);
            return null;
        } catch (SQLException e) {
            return "Error al cambiar la contraseña: " + e.getMessage();
        }
    }

    /**
     * Devuelve la contraseña de un usuario para que un Administrador la vea.
     * @return la contraseña, o null si no está disponible (se creó antes de esta función, o la llave cambió).
     * @throws SecurityException si quien pide no es Administrador.
     */
    public String verContrasena(Usuario quienPide, Usuario objetivo) {
        if (!Permisos.esAdministrativo(quienPide)) {
            throw new SecurityException("Solo un Administrador puede ver contraseñas.");
        }
        if (objetivo.isProtegido()) {
            throw new SecurityException(rechazarProtegido(objetivo, "Ver la contraseña de"));
        }
        try {
            AuditoriaService.registrar(quienPide, "Vio la contraseña de un usuario",
                    objetivo.getNombre() + " · usuario " + objetivo.getUsuarioLogin());
            return CifradoContrasena.descifrar(usuarioDAO.obtenerContrasenaCifrada(objetivo.getIdUsuario()));
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar la contraseña: " + e.getMessage(), e);
        }
    }

    public String cambiarEstado(Usuario objetivo, String nuevoEstado, Usuario quienActua) {
        if (objetivo.isProtegido()) {
            return rechazarProtegido(objetivo, "Activar o desactivar a");
        }
        try {
            if ("inactivo".equals(nuevoEstado)) {
                if (quienActua != null && quienActua.getIdUsuario() == objetivo.getIdUsuario()) {
                    return "No puedes desactivar tu propio usuario.";
                }
                List<Usuario> todos = usuarioDAO.listarUsuarios();
                if (Permisos.esAdministrativo(objetivo.getRol())
                        && otrosAdministrativosActivos(todos, objetivo.getIdUsuario()) == 0) {
                    return "Debe quedar al menos un Administrador activo.";
                }
            }
            usuarioDAO.cambiarEstado(objetivo.getIdUsuario(), nuevoEstado);
            AuditoriaService.registrar(quienActua, "activo".equals(nuevoEstado) ? "Activó usuario" : "Desactivó usuario",
                    objetivo.getNombre() + " · usuario " + objetivo.getUsuarioLogin());
            return null;
        } catch (SQLException e) {
            return "Error al cambiar el estado del usuario: " + e.getMessage();
        }
    }

    public String eliminarUsuario(Usuario objetivo, Usuario quienActua) {
        if (objetivo.isProtegido()) {
            return rechazarProtegido(objetivo, "Eliminar a");
        }
        if (quienActua != null && quienActua.getIdUsuario() == objetivo.getIdUsuario()) {
            return "No puedes eliminar tu propio usuario.";
        }
        try {
            List<Usuario> todos = usuarioDAO.listarUsuarios();
            if (Permisos.esAdministrativo(objetivo.getRol()) && "activo".equals(objetivo.getEstado())
                    && otrosAdministrativosActivos(todos, objetivo.getIdUsuario()) == 0) {
                return "Debe quedar al menos un Administrador activo.";
            }

            int asociados = usuarioDAO.contarRegistrosAsociados(objetivo.getIdUsuario());
            if (asociados > 0) {
                return "No se puede eliminar: este usuario tiene " + asociados
                        + " movimiento(s) o venta(s) registrados. Desactívalo en su lugar y así se conserva el historial.";
            }
            usuarioDAO.eliminarUsuario(objetivo.getIdUsuario());
            AuditoriaService.registrar(quienActua, "Eliminó usuario",
                    objetivo.getNombre() + " · usuario " + objetivo.getUsuarioLogin() + " · rol " + objetivo.getRol());
            return null;
        } catch (SQLException e) {
            return "Error al eliminar el usuario: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------

    /**
     * Rechaza cualquier acción sobre el usuario protegido (el de Rubén Torres) y deja constancia en la auditoría.
     * La única excepción es que él mismo cambie su contraseña desde "¿Olvidaste tu contraseña?" en el login.
     */
    private String rechazarProtegido(Usuario objetivo, String accion) {
        AuditoriaService.registrar(actor(), "Intento bloqueado sobre usuario protegido",
                accion + " " + objetivo.getNombre() + " · usuario " + objetivo.getUsuarioLogin());
        return "Este usuario está protegido: nadie puede editarlo, desactivarlo, eliminarlo ni ver o cambiar "
                + "su contraseña desde el sistema.";
    }

    /** Quien hace la acción: el usuario con sesión iniciada o, si no hay sesión, el administrador que autorizó. */
    private Usuario actor() {
        return Sesion.getUsuarioActual() != null ? Sesion.getUsuarioActual() : autorizador;
    }

    /** "Nombre · usuario login" de un usuario, o su número si ya no se encuentra. */
    private String descripcionDe(int idUsuario) {
        try {
            Usuario u = buscar(usuarioDAO.listarUsuarios(), idUsuario);
            return u == null ? "usuario #" + idUsuario : u.getNombre() + " · usuario " + u.getUsuarioLogin();
        } catch (SQLException e) {
            return "usuario #" + idUsuario;
        }
    }

    private boolean rolValido(String rol) {
        // Basta con que el rol exista: un usuario que ya tenía un rol desactivado puede seguir editándose.
        return rol != null && Permisos.rolDe(rol) != null;
    }

    private String validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            return "La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.";
        }
        return null;
    }

    private Usuario buscar(List<Usuario> usuarios, int id) {
        for (Usuario u : usuarios) {
            if (u.getIdUsuario() == id) {
                return u;
            }
        }
        return null;
    }

    private int otrosAdministrativosActivos(List<Usuario> usuarios, int idExcluido) {
        int total = 0;
        for (Usuario u : usuarios) {
            if (u.getIdUsuario() != idExcluido && "activo".equals(u.getEstado())
                    && Permisos.esAdministrativo(u.getRol())) {
                total++;
            }
        }
        return total;
    }
}
