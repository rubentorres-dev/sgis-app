package sgis.db;

import sgis.modelo.Usuario;
import sgis.util.CifradoContrasena;
import sgis.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
            if (!columnaNoExiste(e)) throw e;
            insertarUsuario(u, contrasenaPlana, false);
        }
    }

    private void insertarUsuario(Usuario u, String contrasenaPlana, boolean conCopia) throws SQLException {
        String sql = conCopia
                ? "INSERT INTO USUARIO (nombre, rol, usuarioLogin, contrasena, estado, contrasenaVisible) VALUES (?, ?, ?, ?, ?, ?)"
                : "INSERT INTO USUARIO (nombre, rol, usuarioLogin, contrasena, estado) VALUES (?, ?, ?, ?, ?)";

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {

            stmt.setString(1, u.getNombre());
            stmt.setString(2, u.getRol());
            stmt.setString(3, u.getUsuarioLogin());
            stmt.setString(4, PasswordUtil.hash(contrasenaPlana));
            stmt.setString(5, u.getEstado() != null ? u.getEstado() : "activo");
            if (conCopia) {
                stmt.setString(6, CifradoContrasena.cifrar(contrasenaPlana));
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
}
