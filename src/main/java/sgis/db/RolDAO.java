package sgis.db;

import sgis.modelo.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla ROL.
 */
public class RolDAO {

    public List<Rol> listar() throws SQLException {
        try {
            return listar(true);
        } catch (SQLException e) {
            // Si la tabla se creó con la versión anterior del script (sin la columna activo), se lee igual
            // y todos los roles se consideran activos.
            if ("42S22".equals(e.getSQLState())) {
                return listar(false);
            }
            throw e;
        }
    }

    private List<Rol> listar(boolean conColumnaActivo) throws SQLException {
        String sql = "SELECT idRol, nombre, descripcion, pantallas, puedeEditar, esSistema, "
                   + (conColumnaActivo ? "activo" : "1 AS activo")
                   + " FROM ROL ORDER BY esSistema DESC, nombre";
        List<Rol> roles = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                roles.add(new Rol(
                        rs.getInt("idRol"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getString("pantallas"),
                        rs.getBoolean("puedeEditar"),
                        rs.getBoolean("esSistema"),
                        rs.getBoolean("activo")));
            }
        }
        return roles;
    }

    /** Activa o desactiva un rol. Los roles del sistema no se tocan. */
    public void cambiarActivo(int idRol, boolean activo) throws SQLException {
        String sql = "UPDATE ROL SET activo = ? WHERE idRol = ? AND esSistema = 0";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setBoolean(1, activo);
            stmt.setInt(2, idRol);
            stmt.executeUpdate();
        }
    }

    public boolean existeNombre(String nombre) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM ROL WHERE LOWER(nombre) = LOWER(?)";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, nombre);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        }
    }

    public void crear(Rol rol) throws SQLException {
        String sql = "INSERT INTO ROL (nombre, descripcion, pantallas, puedeEditar, esSistema) VALUES (?, ?, ?, ?, 0)";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, rol.getNombre());
            stmt.setString(2, rol.getDescripcion());
            stmt.setString(3, rol.getPantallasTexto());
            stmt.setBoolean(4, rol.isPuedeEditar());
            stmt.executeUpdate();
        }
    }

    /** Cambia la descripción, las pantallas y el permiso de editar. Los roles del sistema no se tocan. */
    public void actualizar(Rol rol) throws SQLException {
        String sql = "UPDATE ROL SET descripcion = ?, pantallas = ?, puedeEditar = ? WHERE idRol = ? AND esSistema = 0";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, rol.getDescripcion());
            stmt.setString(2, rol.getPantallasTexto());
            stmt.setBoolean(3, rol.isPuedeEditar());
            stmt.setInt(4, rol.getIdRol());
            stmt.executeUpdate();
        }
    }

    /** Cuántos usuarios tienen este rol. */
    public int contarUsuarios(String nombreRol) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM USUARIO WHERE rol = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, nombreRol);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    public void eliminar(int idRol) throws SQLException {
        String sql = "DELETE FROM ROL WHERE idRol = ? AND esSistema = 0";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idRol);
            stmt.executeUpdate();
        }
    }
}
