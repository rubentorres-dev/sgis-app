package sgis.controlador;

import sgis.db.ConexionBD;
import sgis.db.AuditoriaDAO;
import sgis.db.UsuarioDAO;
import sgis.modelo.Usuario;
import sgis.servicio.AuditoriaService;
import sgis.util.CifradoContrasena;
import sgis.util.CodigoRecuperacion;
import sgis.util.ControlIntentosLogin;
import sgis.util.Permisos;
import sgis.util.Sesion;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
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
        if (sistemaBloqueado()) {
            return null; // con el sistema bloqueado nadie entra, ni con credenciales correctas
        }
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
        if (sistemaBloqueado()) {
            return MENSAJE_SISTEMA_BLOQUEADO;
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
        codigoUltimoUsuarioCreado = null;
        problemaCodigo = null;
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
            // Todo usuario nuevo recibe su código de recuperación (se muestra al crearlo).
            try {
                Usuario creado = buscarPorLogin(u.getUsuarioLogin());
                if (creado != null) {
                    codigoUltimoUsuarioCreado = crearCodigo(creado, actor(), "Generó el código de recuperación");
                }
            } catch (SQLException e) {
                problemaCodigo = motivoDelProblema(e);
            }
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
        // El perfil único del propietario no usa este formulario: su contraseña se recupera con el código único.
        if (esPropietarioProtegido(login)) {
            return "Este es el perfil único del propietario: su contraseña solo se recupera con el código único. "
                    + "Escribe el usuario en el inicio de sesión y pulsa «¿Olvidaste tu contraseña?».";
        }
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

    // ------------------------------------------------------------------------------------------
    // Perfil único del propietario (usuario protegido): código único de recuperación
    // ------------------------------------------------------------------------------------------

    /** Busca al propietario (usuario protegido) por su login; null si no existe o no es el protegido. */
    private Usuario buscarPropietario(String login) throws SQLException {
        if (login == null || login.isBlank()) {
            return null;
        }
        for (Usuario u : usuarioDAO.listarUsuarios()) {
            if (u.isProtegido() && u.getUsuarioLogin().equalsIgnoreCase(login.trim())) {
                return u;
            }
        }
        return null;
    }

    /** true si el login escrito es el del perfil único del propietario (el usuario protegido). */
    public boolean esPropietarioProtegido(String login) {
        try {
            return buscarPropietario(login) != null;
        } catch (SQLException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Código de recuperación de 6 dígitos de TODOS los usuarios
    // ------------------------------------------------------------------------------------------

    /** Motivo por el que el último intento de preparar un código no se pudo hacer (null si no hubo problema). */
    private String problemaCodigo;

    /** Código del último usuario creado con registrarUsuario (para mostrarlo una vez), o null. */
    private String codigoUltimoUsuarioCreado;

    private static final String ACCION_FALLO_RECUPERACION = "Intento fallido de recuperación";
    private static final String ACCION_EXITO_RECUPERACION = "Recuperó su contraseña con el código";

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public String problemaCodigoRecuperacion() {
        return problemaCodigo;
    }

    public String codigoDelUltimoUsuarioCreado() {
        return codigoUltimoUsuarioCreado;
    }

    private Usuario buscarPorLogin(String login) throws SQLException {
        if (login == null || login.isBlank()) {
            return null;
        }
        for (Usuario u : usuarioDAO.listarUsuarios()) {
            if (u.getUsuarioLogin().equalsIgnoreCase(login.trim())) {
                return u;
            }
        }
        return null;
    }

    private String motivoDelProblema(SQLException e) {
        if ("42S22".equals(e.getSQLState())) {
            return "Falta ejecutar el script agregar_codigo_recuperacion.sql en la base de datos.";
        }
        return "Error de la base de datos: " + e.getMessage();
    }

    /**
     * Crea un código nuevo para el usuario: guarda su hash y, salvo el del administrador principal, una copia
     * cifrada que el administrador principal puede ver. Reemplaza el código anterior, si había.
     */
    private String crearCodigo(Usuario usuario, Usuario quienActua, String accion) throws SQLException {
        String codigo = CodigoRecuperacion.generar();
        String copiaCifrada = usuario.isProtegido() ? null : CifradoContrasena.cifrar(codigo);
        usuarioDAO.guardarCodigoRecuperacion(usuario.getIdUsuario(), CodigoRecuperacion.hash(codigo), copiaCifrada);
        AuditoriaService.registrar(quienActua, accion, usuario.getNombre() + " · usuario " + usuario.getUsuarioLogin());
        return codigo;
    }

    /**
     * Se llama cuando alguien acaba de iniciar sesión. Si todavía no tiene código de recuperación (usuarios
     * anteriores a esta función, o tras haber usado su código), genera uno, lo guarda en ese momento y lo
     * devuelve para mostrarlo UNA sola vez.
     *
     * @return el código en claro, o null si no corresponde (ya tiene uno) o no se pudo (en ese caso,
     *         problemaCodigoRecuperacion() dice por qué). Nunca interrumpe el inicio de sesión.
     */
    public String generarCodigoRecuperacionUnaVez(Usuario sesion) {
        problemaCodigo = null;
        if (sesion == null) {
            return null;
        }
        Usuario usuario;
        try {
            usuario = buscarPorLogin(sesion.getUsuarioLogin());
        } catch (SQLException e) {
            return null;
        }
        if (usuario == null) {
            return null;
        }
        try {
            if (usuarioDAO.tieneCodigoRecuperacion(usuario.getIdUsuario())) {
                return null;
            }
            return crearCodigo(usuario, sesion, "Activó su código de recuperación");
        } catch (SQLException e) {
            problemaCodigo = motivoDelProblema(e);
            return null;
        }
    }

    private boolean esAdministradorPrincipal(Usuario quien) {
        return quien != null && esPropietarioProtegido(quien.getUsuarioLogin());
    }

    /**
     * Ver el código de recuperación de un usuario: cualquier Administrador (la misma regla que para ver
     * contraseñas). El código del administrador principal nunca se puede ver.
     */
    public String verCodigoRecuperacion(Usuario quienPide, Usuario objetivo) {
        if (!Permisos.esAdministrativo(quienPide)) {
            throw new SecurityException("Solo un Administrador puede ver los códigos de recuperación.");
        }
        if (objetivo.isProtegido()) {
            throw new SecurityException("El código del administrador principal solo se muestra una vez, al activarlo.");
        }
        try {
            AuditoriaService.registrar(quienPide, "Vio un código de recuperación",
                    objetivo.getNombre() + " · usuario " + objetivo.getUsuarioLogin());
            return CifradoContrasena.descifrar(usuarioDAO.obtenerCodigoCifrado(objetivo.getIdUsuario()));
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el código: " + e.getMessage(), e);
        }
    }

    /** Genera un código NUEVO para un usuario (el anterior deja de servir). Solo el administrador principal. */
    public String renovarCodigoRecuperacion(Usuario quienPide, Usuario objetivo) {
        if (!esAdministradorPrincipal(quienPide)) {
            throw new SecurityException("Solo el administrador principal puede generar códigos de recuperación.");
        }
        if (objetivo.isProtegido()) {
            throw new SecurityException("El código del administrador principal se activa solo, al iniciar sesión.");
        }
        try {
            return crearCodigo(objetivo, quienPide, "Generó el código de recuperación");
        } catch (SQLException e) {
            throw new RuntimeException(motivoDelProblema(e), e);
        }
    }

    /** Ids de los usuarios que ya tienen código (para mostrar la columna «Código»). */
    public Set<Integer> usuariosConCodigo() {
        try {
            return usuarioDAO.idsConCodigo();
        } catch (SQLException e) {
            return new HashSet<>();
        }
    }

    // ---------- Recuperación de la contraseña con el código (los intentos se cuentan POR USUARIO) ----------

    private static String detalleDe(String login) {
        return "Usuario: " + login.trim().toLowerCase();
    }

    /**
     * Intentos fallidos de recuperación de ese usuario en los últimos minutos, del más reciente al más antiguo.
     * Se leen de la auditoría (la base de datos), así que NO se reinician al cerrar la aplicación. Los fallos
     * anteriores a una recuperación exitosa de ese mismo usuario no cuentan.
     */
    private List<LocalDateTime> fallosRecientesDeRecuperacion(String login) throws SQLException {
        LocalDateTime ahora = LocalDateTime.now();
        long idUltimoExito = auditoriaDAO.ultimoIdQueEmpiezaCon(ACCION_EXITO_RECUPERACION, detalleDe(login));
        List<LocalDateTime> validos = new ArrayList<>();
        for (LocalDateTime fecha : auditoriaDAO.ultimasFechas(ACCION_FALLO_RECUPERACION, detalleDe(login),
                idUltimoExito, ControlIntentosLogin.MAX_INTENTOS)) {
            if (fecha.isAfter(ahora.minusMinutes(ControlIntentosLogin.MINUTOS_BLOQUEO))) {
                validos.add(fecha);
            }
        }
        return validos;
    }

    private long segundosBloqueoPermanente(String login) {
        try {
            List<LocalDateTime> fallos = fallosRecientesDeRecuperacion(login);
            if (fallos.size() < ControlIntentosLogin.MAX_INTENTOS) {
                return 0;
            }
            long resto = Duration.between(LocalDateTime.now(),
                    fallos.get(0).plusMinutes(ControlIntentosLogin.MINUTOS_BLOQUEO)).getSeconds();
            return Math.max(resto, 0);
        } catch (SQLException e) {
            return 0; // sin auditoría disponible, queda el control en memoria
        }
    }

    private int intentosRestantesPermanentes(String login) {
        try {
            return Math.max(ControlIntentosLogin.MAX_INTENTOS - fallosRecientesDeRecuperacion(login).size(), 0);
        } catch (SQLException e) {
            return -1;
        }
    }

    /**
     * Pone una contraseña nueva a un usuario si escribe su código de recuperación (6 dígitos). Sirve para TODOS
     * los usuarios, incluido el administrador principal. El código vale una sola vez, y después de 5 intentos
     * fallidos con ese usuario la recuperación se bloquea 5 minutos (el conteo se guarda en la base de datos).
     *
     * @return null si todo salió bien, o el mensaje de por qué no se pudo.
     */
    public String recuperarContrasena(String login, String codigo, String contrasenaNueva) {
        if (login == null || login.isBlank()) {
            return "Escribe tu usuario en el inicio de sesión.";
        }
        if (sistemaBloqueado()) {
            return MENSAJE_SISTEMA_BLOQUEADO;
        }
        if (!CodigoRecuperacion.tieneFormatoValido(codigo)) {
            return "Escribe tu código de 6 dígitos.";
        }
        String clave = "recuperacion:" + login.trim().toLowerCase();
        long segundos = Math.max(ControlIntentosLogin.segundosDeBloqueoRestantes(clave), segundosBloqueoPermanente(login));
        if (segundos > 0) {
            return "Demasiados intentos. Intenta de nuevo en " + (segundos / 60) + " min " + (segundos % 60) + " s.";
        }
        String errorNueva = validarContrasena(contrasenaNueva);
        if (errorNueva != null) {
            return errorNueva;
        }

        try {
            Usuario usuario = buscarPorLogin(login);
            boolean existeYActivo = usuario != null && "activo".equals(usuario.getEstado());
            if (existeYActivo && !usuarioDAO.tieneCodigoRecuperacion(usuario.getIdUsuario())) {
                return "Este usuario todavía no tiene un código. Entra una vez con tu contraseña para recibirlo, "
                        + "o pídeselo al administrador principal.";
            }
            // Si el usuario no existe o está inactivo se responde igual que con un código incorrecto.
            if (!existeYActivo || !usuarioDAO.codigoRecuperacionCoincide(usuario.getIdUsuario(), codigo)) {
                int restantesMemoria = ControlIntentosLogin.registrarFallo(clave);
                AuditoriaService.registrar(null, ACCION_FALLO_RECUPERACION, detalleDe(login));
                int restantesGuardados = intentosRestantesPermanentes(login);
                int restantes = restantesGuardados < 0 ? restantesMemoria : Math.min(restantesMemoria, restantesGuardados);
                if (restantes == 0) {
                    return "Código incorrecto. Se bloqueó la recuperación por "
                            + ControlIntentosLogin.MINUTOS_BLOQUEO + " minutos.";
                }
                return "Código incorrecto. Intentos restantes: " + restantes + ".";
            }

            usuarioDAO.cambiarContrasena(usuario.getIdUsuario(), contrasenaNueva);
            // Un código vale una sola vez: se descarta, y en el próximo inicio de sesión se muestra otro nuevo.
            usuarioDAO.guardarCodigoRecuperacion(usuario.getIdUsuario(), null, null);
            ControlIntentosLogin.registrarExito(clave);
            ControlIntentosLogin.registrarExito(login); // si estaba bloqueado por intentos fallidos, queda libre
            AuditoriaService.registrar(usuario, ACCION_EXITO_RECUPERACION, detalleDe(login));
            return null;
        } catch (SQLException e) {
            if ("42S22".equals(e.getSQLState())) {
                return "La recuperación por código no está disponible: falta ejecutar el script "
                        + "agregar_codigo_recuperacion.sql en la base de datos.";
            }
            return "Error al recuperar la contraseña: " + e.getMessage();
        }
    }

    /** Se conserva por compatibilidad: la recuperación ahora es igual para todos los usuarios. */
    public String recuperarContrasenaPropietario(String login, String codigo, String contrasenaNueva) {
        return recuperarContrasena(login, codigo, contrasenaNueva);
    }

    // ------------------------------------------------------------------------------------------
    // Bloqueo del sistema por ataque de fuerza bruta
    //
    // Cada vez que alguien llega al bloqueo temporal de un usuario (5 intentos fallidos = 5 minutos) queda un
    // registro en la auditoría. Si un MISMO usuario acumula 3 bloqueos dentro de una hora (es decir, alguien
    // esperó varias veces y siguió probando), el sistema se bloquea completo. El estado se guarda en la base
    // de datos (no se pierde al cerrar la aplicación) y solo lo levanta un Administrador con sus credenciales.
    // ------------------------------------------------------------------------------------------

    public static final int BLOQUEOS_PARA_ATAQUE = 3;
    public static final int VENTANA_ATAQUE_MINUTOS = 60;
    private static final int MAX_INTENTOS_DESBLOQUEO = 5;
    private static final int MINUTOS_BLOQUEO_DESBLOQUEO = 15;

    public static final String MENSAJE_SISTEMA_BLOQUEADO =
            "El sistema está bloqueado. Para acceder, comunícate con los administradores.";

    private static final String ACCION_BLOQUEO_TEMPORAL = "Acceso bloqueado por intentos fallidos";
    private static final String ACCION_SISTEMA_BLOQUEADO = "Sistema bloqueado por fuerza bruta";
    private static final String ACCION_SISTEMA_DESBLOQUEADO = "Sistema desbloqueado por un administrador";
    private static final String ACCION_FALLO_DESBLOQUEO = "Intento fallido de desbloqueo";

    /** true si el sistema está bloqueado por un ataque de fuerza bruta (hasta que un administrador lo desbloquee). */
    public boolean sistemaBloqueado() {
        try {
            return auditoriaDAO.ultimoId(ACCION_SISTEMA_BLOQUEADO) > auditoriaDAO.ultimoId(ACCION_SISTEMA_DESBLOQUEADO);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Se llama cuando un usuario acaba de llegar al bloqueo temporal por intentos fallidos. Deja el registro y
     * comprueba si ya son 3 bloqueos del mismo usuario en la última hora (sin contar los anteriores al último
     * desbloqueo del sistema).
     *
     * @return true si el sistema quedó bloqueado por un ataque de fuerza bruta
     */
    public boolean registrarBloqueoTemporal(String login) {
        String detalle = detalleDe(login);
        AuditoriaService.registrar(null, ACCION_BLOQUEO_TEMPORAL, detalle);
        try {
            long desdeElDesbloqueo = auditoriaDAO.ultimoId(ACCION_SISTEMA_DESBLOQUEADO);
            LocalDateTime limite = LocalDateTime.now().minusMinutes(VENTANA_ATAQUE_MINUTOS);
            int bloqueos = 0;
            for (LocalDateTime fecha : auditoriaDAO.ultimasFechas(ACCION_BLOQUEO_TEMPORAL, detalle,
                    desdeElDesbloqueo, BLOQUEOS_PARA_ATAQUE)) {
                if (fecha.isAfter(limite)) {
                    bloqueos++;
                }
            }
            if (bloqueos >= BLOQUEOS_PARA_ATAQUE && !sistemaBloqueado()) {
                AuditoriaService.registrar(null, ACCION_SISTEMA_BLOQUEADO,
                        "Ataque de fuerza bruta contra el usuario " + login.trim() + " (" + bloqueos
                                + " bloqueos en " + VENTANA_ATAQUE_MINUTOS + " minutos)");
            }
            return sistemaBloqueado();
        } catch (SQLException e) {
            return false;
        }
    }

    /** Segundos que faltan para poder volver a intentar el desbloqueo, o 0 si no está limitado. */
    private long segundosBloqueoDelDesbloqueo() {
        try {
            long desde = auditoriaDAO.ultimoId(ACCION_SISTEMA_DESBLOQUEADO);
            LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_BLOQUEO_DESBLOQUEO);
            List<LocalDateTime> recientes = new ArrayList<>();
            for (LocalDateTime fecha : auditoriaDAO.ultimasFechasDeAccion(ACCION_FALLO_DESBLOQUEO, desde, MAX_INTENTOS_DESBLOQUEO)) {
                if (fecha.isAfter(limite)) {
                    recientes.add(fecha);
                }
            }
            if (recientes.size() < MAX_INTENTOS_DESBLOQUEO) {
                return 0;
            }
            return Math.max(Duration.between(LocalDateTime.now(), recientes.get(0).plusMinutes(MINUTOS_BLOQUEO_DESBLOQUEO)).getSeconds(), 0);
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Desbloquea el sistema si las credenciales son de un Administrador. Después de 5 intentos fallidos el propio
     * desbloqueo se limita 15 minutos (también guardado en la base de datos), para que esta pantalla no sirva
     * para adivinar contraseñas.
     *
     * @return null si quedó desbloqueado, o el mensaje de por qué no.
     */
    public String desbloquearSistema(String login, String contrasena) {
        if (!sistemaBloqueado()) {
            return null;
        }
        if (login == null || login.isBlank() || contrasena == null || contrasena.isEmpty()) {
            return "Escribe el usuario y la contraseña de un administrador.";
        }
        long espera = segundosBloqueoDelDesbloqueo();
        if (espera > 0) {
            return "Demasiados intentos. Intenta de nuevo en " + (espera / 60) + " min " + (espera % 60) + " s.";
        }
        try {
            Usuario usuario = usuarioDAO.autenticar(login, contrasena);
            if (usuario != null && Permisos.esAdministrativo(usuario)) {
                AuditoriaService.registrar(usuario, ACCION_SISTEMA_DESBLOQUEADO, "Desbloqueado por " + usuario.getUsuarioLogin());
                ControlIntentosLogin.reiniciarTodo();
                return sistemaBloqueado() ? "No se pudo registrar el desbloqueo. Intenta de nuevo." : null;
            }
            AuditoriaService.registrar(null, ACCION_FALLO_DESBLOQUEO, detalleDe(login));
            return "Esas credenciales no pertenecen a un administrador.";
        } catch (SQLException e) {
            return "Error al verificar las credenciales: " + e.getMessage();
        }
    }
}
