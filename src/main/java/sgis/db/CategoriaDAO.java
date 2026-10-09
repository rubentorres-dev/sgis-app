package sgis.db;

import sgis.modelo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla CATEGORIA.
 */
public class CategoriaDAO {

    public List<Categoria> listarCategorias() throws SQLException {
        String sql = "SELECT idCategoria, nombre FROM CATEGORIA ORDER BY nombre";
        List<Categoria> categorias = new ArrayList<>();

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categorias.add(new Categoria(rs.getInt("idCategoria"), rs.getString("nombre")));
            }
        }
        return categorias;
    }

    /** Lista las categorías indicando cuántos productos tiene cada una. */
    public List<Categoria> listarConConteo() throws SQLException {
        String sql = "SELECT c.idCategoria, c.nombre, COUNT(p.idProducto) AS productos "
                   + "FROM CATEGORIA c LEFT JOIN PRODUCTO p ON p.idCategoria = c.idCategoria "
                   + "GROUP BY c.idCategoria, c.nombre ORDER BY c.nombre";
        List<Categoria> categorias = new ArrayList<>();

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Categoria c = new Categoria(rs.getInt("idCategoria"), rs.getString("nombre"));
                c.setCantidadProductos(rs.getInt("productos"));
                categorias.add(c);
            }
        }
        return categorias;
    }

    /** Indica si ya existe una categoría con ese nombre (sin distinguir mayúsculas). */
    public boolean existeNombre(String nombre, int idExcluido) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM CATEGORIA WHERE LOWER(nombre) = LOWER(?) AND idCategoria <> ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, nombre);
            stmt.setInt(2, idExcluido);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        }
    }

    public void registrar(String nombre) throws SQLException {
        String sql = "INSERT INTO CATEGORIA (nombre) VALUES (?)";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, nombre);
            stmt.executeUpdate();
        }
    }

    public void renombrar(int idCategoria, String nuevoNombre) throws SQLException {
        String sql = "UPDATE CATEGORIA SET nombre = ? WHERE idCategoria = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, nuevoNombre);
            stmt.setInt(2, idCategoria);
            stmt.executeUpdate();
        }
    }

    public void eliminar(int idCategoria) throws SQLException {
        String sql = "DELETE FROM CATEGORIA WHERE idCategoria = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idCategoria);
            stmt.executeUpdate();
        }
    }
}
