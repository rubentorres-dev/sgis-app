package sgis.db;

import sgis.modelo.Usuario;
import sgis.util.CifradoContrasena;
import sgis.util.CodigoRecuperacion;
import sgis.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DAO para la tabla USUARIO: autenticación (login) y gestión de usuarios.
 */
public class UsuarioDAO {

    /**
     * Verifica las credenciales de un usuario.
     * @return el Usuario si el login/contraseña son correctos y el usuario está activo,
     *         o null si no coinciden o el usuario está inactivo.
     */
    public Usuario autenticar(String usuarioLogin, String contrasenaPlana) throws SQLException {
        String sql = "SELECT idUsuario, nombre, rol, usuarioLogin, contrasena, estado "
                   + "FROM USUARIO WHERE usuarioLogin = ? AND estado = 'activo'";

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {

            stmt.setString(1, usuarioLogin);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashGuardado = rs.getString("contrasena");

                    if (PasswordUtil.coincide(contrasenaPlana, hashGuardado)) {
                        return new Usuario(
                                rs.getInt("idUsuario"),
                                rs.getString("nombre"),
                                rs.getString("rol"),
                                rs.getString("usuarioLogin"),
                                hashGuardado,
                                rs.getString("estado")
                        );
                    }
                }
                return null; // usuario no existe, contraseña incorrecta, o inactivo
            }
        }
    }

    /**
     * Actualiza el nombre y el rol de un usuario existente (no cambia la contraseña aquí).
     */
    public void actualizarUsuario(Usuario u) throws SQLException {
        String sql = "UPDATE USUARIO SET nombre = ?, rol = ? WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, u.getNombre());
            stmt.setString(2, u.getRol());
            stmt.setInt(3, u.getIdUsuario());
            stmt.executeUpdate();
        }
    }

    /**
     * Activa o desactiva un usuario (RF-06: activar/desactivar usuarios).
     */
    public void cambiarEstado(int idUsuario, String nuevoEstado) throws SQLException {
        String sql = "UPDATE USUARIO SET estado = ? WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        }
    }

    /**
     * Devuelve la lista completa de usuarios (para la pantalla de administración).
     */
    public List<Usuario> listarUsuarios() throws SQLException {
        try {
            return listarUsuarios(true);
        } catch (SQLException e) {
            // Si todavía no se ejecutó el script que agrega la columna protegido, el usuario "admin"
            // (el de Rubén Torres) queda protegido igual.
            if ("42S22".equals(e.getSQLState())) {
                return listarUsuarios(false);
            }
            throw e;
        }
    }

    private List<Usuario> listarUsuarios(boolean conColumnaProtegido) throws SQLException {
        String sql = "SELECT idUsuario, nombre, rol, usuarioLogin, contrasena, estado, "
                   + (conColumnaProtegido ? "protegido" : "(usuarioLogin = 'admin') AS protegido")
                   + " FROM USUARIO ORDER BY nombre";
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Usuario usuario = new Usuario(
                        rs.getInt("idUsuario"),
                        rs.getString("nombre"),
                        rs.getString("rol"),
                        rs.getString("usuarioLogin"),
                        rs.getString("contrasena"),
                        rs.getString("estado"));
                usuario.setProtegido(rs.getBoolean("protegido"));
                usuarios.add(usuario);
            }
        }
        return usuarios;
    }

    /**
     * Registra un nuevo usuario, cifrando la contraseña antes de guardarla.
     */
    public void registrarUsuario(Usuario u, String contrasenaPlana) throws SQLException {
        try {
            insertarUsuario(u, contrasenaPlana, true);
        } catch (SQLException e) {
            // Si la columna de la copia cifrada aún no existe (falta ejecutar el script), se guarda sin ella.
            // Si lo que falta es la columna de la cédula, el error se informa (no se crea un usuario sin cédula).
            if (!columnaNoExiste(e) || mencionaCedula(e)) throw e;
            insertarUsuario(u, contrasenaPlana, false);
        }
    }

    private void insertarUsuario(Usuario u, String contrasenaPlana, boolean conCopia) throws SQLException {
        String sql = conCopia
                ? "INSERT INTO USUARIO (nombre, rol, usuarioLogin, contrasena, estado, cedula, telefono, contrasenaVisible) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO USUARIO (nombre, rol, usuarioLogin, contrasena, estado, cedula, telefono) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {

            stmt.setString(1, u.getNombre());
            stmt.setString(2, u.getRol());
            stmt.setString(3, u.getUsuarioLogin());
            stmt.setString(4, PasswordUtil.hash(contrasenaPlana));
            stmt.setString(5, u.getEstado() != null ? u.getEstado() : "activo");
            stmt.setString(6, u.getCedula());
            stmt.setString(7, u.getTelefono());
            if (conCopia) {
                stmt.setString(8, CifradoContrasena.cifrar(contrasenaPlana));
            }
            stmt.executeUpdate();
        }
    }

    /** SQLState 42S22 = "la columna no existe". */
    private boolean columnaNoExiste(SQLException e) {
        return "42S22".equals(e.getSQLState());
    }

    /** Indica si ya existe un usuario con ese nombre de usuario (login). */
    public boolean existeLogin(String usuarioLogin) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM USUARIO WHERE LOWER(usuarioLogin) = LOWER(?)";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, usuarioLogin);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        }
    }

    /** Cambia la contraseña de un usuario (se guarda cifrada). */
    public void cambiarContrasena(int idUsuario, String contrasenaPlana) throws SQLException {
        try {
            actualizarContrasena(idUsuario, contrasenaPlana, true);
        } catch (SQLException e) {
            if (!columnaNoExiste(e)) throw e;
            actualizarContrasena(idUsuario, contrasenaPlana, false);
        }
    }

    private void actualizarContrasena(int idUsuario, String contrasenaPlana, boolean conCopia) throws SQLException {
        String sql = conCopia
                ? "UPDATE USUARIO SET contrasena = ?, contrasenaVisible = ? WHERE idUsuario = ?"
                : "UPDATE USUARIO SET contrasena = ? WHERE idUsuario = ?";

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, PasswordUtil.hash(contrasenaPlana));
            if (conCopia) {
                stmt.setString(2, CifradoContrasena.cifrar(contrasenaPlana));
                stmt.setInt(3, idUsuario);
            } else {
                stmt.setInt(2, idUsuario);
            }
            stmt.executeUpdate();
        }
    }

    /**
     * Copia cifrada de la contraseña de un usuario (o null si no tiene: se creó antes de esta función).
     * Solo se consulta cuando un Administrador pulsa el ojo; no forma parte de la lista de usuarios.
     */
    public String obtenerContrasenaCifrada(int idUsuario) throws SQLException {
        String sql = "SELECT contrasenaVisible FROM USUARIO WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            if (columnaNoExiste(e)) return null;
            throw e;
        }
    }

    /** Cuántos movimientos de inventario y ventas ha registrado este usuario. */
    public int contarRegistrosAsociados(int idUsuario) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM MOVIMIENTO WHERE idUsuario = ?) "
                   + "     + (SELECT COUNT(*) FROM VENTA WHERE idUsuario = ?) AS total";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    /** Elimina definitivamente un usuario (solo si no tiene historial asociado). */
    public void eliminarUsuario(int idUsuario) throws SQLException {
        String sql = "DELETE FROM USUARIO WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.executeUpdate();
        }
    }

    /** Indica si la contraseña dada es la actual de ese usuario (se compara contra el hash, sin importar su estado). */
    public boolean contrasenaCoincide(int idUsuario, String contrasenaPlana) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM USUARIO WHERE idUsuario = ? AND contrasena = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setString(2, PasswordUtil.hash(contrasenaPlana));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Código único de recuperación del propietario (usuario protegido). Solo se guarda su hash.
    // Si la columna codigoRecuperacion todavía no existe, estos métodos lanzan SQLException
    // (SQLState 42S22) y quien los llama debe tratarlo como "función no disponible".
    // ------------------------------------------------------------------------------------------

    /** @return true si el usuario ya tiene un código de recuperación activo. */
    public boolean tieneCodigoRecuperacion(int idUsuario) throws SQLException {
        String sql = "SELECT codigoRecuperacion FROM USUARIO WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String hash = rs.getString(1);
                return hash != null && !hash.isEmpty();
            }
        }
    }

    /** Guarda el hash del código de recuperación; con null lo descarta (el código ya se usó). */
    public void guardarCodigoRecuperacion(int idUsuario, String hash) throws SQLException {
        String sql = "UPDATE USUARIO SET codigoRecuperacion = ? WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            if (hash == null) {
                stmt.setNull(1, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(1, hash);
            }
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        }
    }

    /** @return true si el código escrito coincide con el guardado para ese usuario. */
    public boolean codigoRecuperacionCoincide(int idUsuario, String codigoEscrito) throws SQLException {
        String sql = "SELECT codigoRecuperacion FROM USUARIO WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && CodigoRecuperacion.coincide(codigoEscrito, rs.getString(1));
            }
        }
    }

    /**
     * Guarda el hash del código de recuperación y, si se indica, una copia cifrada (columna codigoVisible) que el
     * administrador principal puede ver. Con hash y copia en null, descarta el código. Si la columna
     * codigoVisible todavía no existe, se guarda solo el hash (el código funciona, pero no se puede ver).
     */
    public void guardarCodigoRecuperacion(int idUsuario, String hash, String cifrado) throws SQLException {
        try {
            guardarCodigo(idUsuario, hash, cifrado, true);
        } catch (SQLException e) {
            if (!columnaNoExiste(e)) throw e;
            guardarCodigo(idUsuario, hash, cifrado, false); // sin la copia visible; si falta el hash, vuelve a fallar
        }
    }

    private void guardarCodigo(int idUsuario, String hash, String cifrado, boolean conCopia) throws SQLException {
        String sql = conCopia
                ? "UPDATE USUARIO SET codigoRecuperacion = ?, codigoVisible = ? WHERE idUsuario = ?"
                : "UPDATE USUARIO SET codigoRecuperacion = ? WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            if (hash == null) stmt.setNull(1, java.sql.Types.VARCHAR); else stmt.setString(1, hash);
            if (conCopia) {
                if (cifrado == null) stmt.setNull(2, java.sql.Types.VARCHAR); else stmt.setString(2, cifrado);
                stmt.setInt(3, idUsuario);
            } else {
                stmt.setInt(2, idUsuario);
            }
            stmt.executeUpdate();
        }
    }

    /** La copia cifrada del código (para que el administrador principal la vea), o null si no hay. */
    public String obtenerCodigoCifrado(int idUsuario) throws SQLException {
        String sql = "SELECT codigoVisible FROM USUARIO WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            if (columnaNoExiste(e)) return null;
            throw e;
        }
    }

    /** Ids de los usuarios que tienen un código de recuperación activo. */
    public Set<Integer> idsConCodigo() throws SQLException {
        String sql = "SELECT idUsuario FROM USUARIO WHERE codigoRecuperacion IS NOT NULL AND codigoRecuperacion <> ''";
        Set<Integer> ids = new HashSet<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    private boolean mencionaCedula(SQLException e) {
        return e.getMessage() != null
                && (e.getMessage().toLowerCase().contains("cedula") || e.getMessage().toLowerCase().contains("telefono"));
    }

    /** ¿Otro usuario ya tiene esa cédula? */
    public boolean existeCedula(String cedula, int idUsuarioExcluido) throws SQLException {
        String sql = "SELECT COUNT(*) FROM USUARIO WHERE cedula = ? AND idUsuario <> ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, cedula);
            stmt.setInt(2, idUsuarioExcluido);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** La cédula del usuario (solo dígitos), o null si no tiene o si falta el script de la cédula. */
    public String obtenerCedula(int idUsuario) throws SQLException {
        String sql = "SELECT cedula FROM USUARIO WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            if (columnaNoExiste(e)) return null;
            throw e;
        }
    }

    public void actualizarCedula(int idUsuario, String cedula) throws SQLException {
        String sql = "UPDATE USUARIO SET cedula = ? WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, cedula);
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        }
    }

    /** El teléfono del usuario (solo dígitos), o null si no tiene o si falta el script del teléfono. */
    public String obtenerTelefono(int idUsuario) throws SQLException {
        String sql = "SELECT telefono FROM USUARIO WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            if (columnaNoExiste(e)) return null;
            throw e;
        }
    }

    public void actualizarTelefono(int idUsuario, String telefono) throws SQLException {
        String sql = "UPDATE USUARIO SET telefono = ? WHERE idUsuario = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, telefono);
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        }
    }

    /**
     * Cédula y teléfono de todos los usuarios (id → {cédula, teléfono}) para la tabla de Configuración.
     * Si faltan los scripts, devuelve lo que haya (o nada) sin romper la pantalla.
     */
    public java.util.Map<Integer, String[]> contactos() throws SQLException {
        java.util.Map<Integer, String[]> mapa = new java.util.HashMap<>();
        String[] consultas = {"SELECT idUsuario, cedula, telefono FROM USUARIO", "SELECT idUsuario, cedula, NULL FROM USUARIO"};
        for (int i = 0; i < consultas.length; i++) {
            try (Connection conexion = ConexionBD.obtenerConexion();
                 PreparedStatement stmt = conexion.prepareStatement(consultas[i]);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getInt(1), new String[]{rs.getString(2), rs.getString(3)});
                }
                return mapa;
            } catch (SQLException e) {
                if (!columnaNoExiste(e)) throw e;
                mapa.clear();
            }
        }
        return mapa;
    }
}
