package sgis.db;

import sgis.modelo.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) para la tabla PRODUCTO.
 * Aquí viven todas las consultas SQL relacionadas con productos.
 * El resto de la aplicación (controlador, vista) nunca escribe SQL
 * directamente: siempre pasa por esta clase.
 */
public class ProductoDAO {

    /**
     * Busca un producto por su id, usando una conexión/transacción existente
     * (para poder validar stock dentro de la misma transacción de una venta).
     * @return el Producto, o null si no existe.
     */
    public Producto obtenerPorId(Connection conexion, int idProducto) throws SQLException {
        String sql = "SELECT idProducto, nombre, descripcion, precio, cantidadDisponible, estado, idCategoria "
                   + "FROM PRODUCTO WHERE idProducto = ? FOR UPDATE";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idProducto);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Producto(
                            rs.getInt("idProducto"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getDouble("precio"),
                            rs.getInt("cantidadDisponible"),
                            rs.getString("estado"),
                            rs.getInt("idCategoria")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Descuenta cantidad del stock de un producto, dentro de una transacción existente.
     */
    public void descontarStock(Connection conexion, int idProducto, int cantidad) throws SQLException {
        String sql = "UPDATE PRODUCTO SET cantidadDisponible = cantidadDisponible - ? WHERE idProducto = ?";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, cantidad);
            stmt.setInt(2, idProducto);
            stmt.executeUpdate();
        }
    }

    /**
     * Actualiza los datos de un producto existente. Si cambió la cantidad disponible, deja
     * registrado el movimiento (entrada si aumentó, salida si disminuyó) con el usuario que
     * hizo el cambio, para que todo cambio de existencias quede trazable.
     * @param idUsuario usuario que realiza la acción (si es null, no se registra el movimiento).
     */
    public void actualizarProducto(Producto p, Integer idUsuario) throws SQLException {
        String sql = "UPDATE PRODUCTO SET nombre = ?, descripcion = ?, precio = ?, "
                   + "cantidadDisponible = ?, estado = ?, idCategoria = ? WHERE idProducto = ?";

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                int cantidadAnterior = leerCantidadConBloqueo(conexion, p.getIdProducto());

                try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
                    stmt.setString(1, p.getNombre());
                    stmt.setString(2, p.getDescripcion());
                    stmt.setDouble(3, p.getPrecio());
                    stmt.setInt(4, p.getCantidadDisponible());
                    stmt.setString(5, p.getEstado());
                    stmt.setInt(6, p.getIdCategoria());
                    stmt.setInt(7, p.getIdProducto());
                    stmt.executeUpdate();
                }

                int diferencia = p.getCantidadDisponible() - cantidadAnterior;
                if (idUsuario != null && diferencia != 0) {
                    registrarMovimiento(conexion, diferencia > 0 ? "entrada" : "salida",
                            Math.abs(diferencia), p.getIdProducto(), idUsuario);
                }
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    /**
     * "Elimina" un producto marcándolo como inactivo (no se borra físicamente,
     * para no romper el historial de movimientos y ventas ya asociado a él).
     */
    public void desactivarProducto(int idProducto) throws SQLException {
        String sql = "UPDATE PRODUCTO SET estado = 'inactivo' WHERE idProducto = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idProducto);
            stmt.executeUpdate();
        }
    }

    /**
     * Devuelve la lista completa de productos registrados en la base de datos.
     */
    public List<Producto> listarProductos() throws SQLException {
        String sql = "SELECT idProducto, nombre, descripcion, precio, cantidadDisponible, estado, idCategoria "
                   + "FROM PRODUCTO ORDER BY nombre";

        List<Producto> productos = new ArrayList<>();

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Producto p = new Producto(
                        rs.getInt("idProducto"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getDouble("precio"),
                        rs.getInt("cantidadDisponible"),
                        rs.getString("estado"),
                        rs.getInt("idCategoria")
                );
                productos.add(p);
            }
        }
        return productos;
    }

    /**
     * Inserta un nuevo producto. Si trae existencias iniciales, deja registrado el movimiento
     * de entrada (con el usuario que lo creó) para que el historial de inventario quede completo.
     * @param idUsuario usuario que realiza la acción (si es null, no se registra el movimiento).
     */
    public void registrarProducto(Producto p, Integer idUsuario) throws SQLException {
        String sql = "INSERT INTO PRODUCTO (nombre, descripcion, precio, cantidadDisponible, estado, idCategoria) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                int idNuevo;
                try (PreparedStatement stmt = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, p.getNombre());
                    stmt.setString(2, p.getDescripcion());
                    stmt.setDouble(3, p.getPrecio());
                    stmt.setInt(4, p.getCantidadDisponible());
                    stmt.setString(5, p.getEstado());
                    stmt.setInt(6, p.getIdCategoria());
                    stmt.executeUpdate();

                    try (ResultSet claves = stmt.getGeneratedKeys()) {
                        if (!claves.next()) {
                            throw new SQLException("No se pudo obtener el id del producto recién creado.");
                        }
                        idNuevo = claves.getInt(1);
                    }
                }

                if (idUsuario != null && p.getCantidadDisponible() > 0) {
                    registrarMovimiento(conexion, "entrada", p.getCantidadDisponible(), idNuevo, idUsuario);
                }
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    private int leerCantidadConBloqueo(Connection conexion, int idProducto) throws SQLException {
        String sql = "SELECT cantidadDisponible FROM PRODUCTO WHERE idProducto = ? FOR UPDATE";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idProducto);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cantidadDisponible");
                }
                throw new SQLException("El producto con id " + idProducto + " no existe.");
            }
        }
    }

    private void registrarMovimiento(Connection conexion, String tipo, int cantidad,
                                     int idProducto, int idUsuario) throws SQLException {
        String sql = "INSERT INTO MOVIMIENTO (tipo, cantidad, fecha, idProducto, idUsuario) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, tipo);
            stmt.setInt(2, cantidad);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, idProducto);
            stmt.setInt(5, idUsuario);
            stmt.executeUpdate();
        }
    }
}
