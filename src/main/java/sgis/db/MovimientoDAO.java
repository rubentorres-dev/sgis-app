package sgis.db;

import sgis.modelo.Actividad;
import sgis.modelo.Movimiento;
import sgis.util.StockInsuficienteException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla MOVIMIENTO (entradas y salidas de inventario).
 * Las operaciones de registrar entrada/salida son transaccionales:
 * o se actualiza el stock del producto Y se guarda el movimiento juntos,
 * o no se guarda nada (para que nunca queden desincronizados).
 */
public class MovimientoDAO {

    /**
     * Registra una ENTRADA de producto: aumenta el stock y guarda el movimiento.
     */
    public void registrarEntrada(int idProducto, int cantidad, int idUsuario) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                actualizarStock(conexion, idProducto, cantidad); // suma
                insertarMovimiento(conexion, "entrada", cantidad, idProducto, idUsuario);
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    /**
     * Registra una SALIDA de producto: valida que haya stock suficiente
     * (RF-04), y si lo hay, disminuye el stock y guarda el movimiento.
     * Si no hay suficiente stock, lanza StockInsuficienteException y
     * no se modifica nada en la base de datos.
     */
    public void registrarSalida(int idProducto, int cantidad, int idUsuario)
            throws SQLException, StockInsuficienteException {

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                int disponible = consultarStockConBloqueo(conexion, idProducto);

                if (cantidad > disponible) {
                    conexion.rollback();
                    throw new StockInsuficienteException(
                        "Cantidad solicitada (" + cantidad + ") supera la existencia disponible (" + disponible + ")."
                    );
                }

                actualizarStock(conexion, idProducto, -cantidad); // resta
                insertarMovimiento(conexion, "salida", cantidad, idProducto, idUsuario);
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    /**
     * Devuelve el historial de movimientos, del más reciente al más antiguo.
     */
    public List<Movimiento> listarMovimientos() throws SQLException {
        String sql = "SELECT idMovimiento, tipo, cantidad, fecha, idProducto, idUsuario "
                   + "FROM MOVIMIENTO ORDER BY fecha DESC";

        List<Movimiento> movimientos = new ArrayList<>();

        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Movimiento m = new Movimiento(
                        rs.getInt("idMovimiento"),
                        rs.getString("tipo"),
                        rs.getInt("cantidad"),
                        rs.getTimestamp("fecha").toLocalDateTime(),
                        rs.getInt("idProducto"),
                        rs.getInt("idUsuario")
                );
                movimientos.add(m);
            }
        }
        return movimientos;
    }

    // ---------------------------------------------------------------
    // Métodos internos de apoyo (usan la MISMA conexión/transacción)
    // ---------------------------------------------------------------

    private int consultarStockConBloqueo(Connection conexion, int idProducto) throws SQLException {
        // "FOR UPDATE" bloquea la fila del producto hasta que termine la transacción,
        // para que dos salidas simultáneas del mismo producto no se pisen entre sí.
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

    private void actualizarStock(Connection conexion, int idProducto, int variacion) throws SQLException {
        String sql = "UPDATE PRODUCTO SET cantidadDisponible = cantidadDisponible + ? WHERE idProducto = ?";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, variacion);
            stmt.setInt(2, idProducto);
            stmt.executeUpdate();
        }
    }

    private void insertarMovimiento(Connection conexion, String tipo, int cantidad,
                                     int idProducto, int idUsuario) throws SQLException {
        String sql = "INSERT INTO MOVIMIENTO (tipo, cantidad, fecha, idProducto, idUsuario) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, tipo);
            stmt.setInt(2, cantidad);
            stmt.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
            stmt.setInt(4, idProducto);
            stmt.setInt(5, idUsuario);
            stmt.executeUpdate();
        }
    }

    /**
     * Historial completo, del más reciente al más antiguo: entradas y salidas de inventario junto
     * con cada producto vendido (qué se vendió, quién lo vendió y quién lo compró).
     */
    public List<Actividad> listarHistorial() throws SQLException {
        String sql =
                "SELECT m.fecha AS fecha, m.tipo AS tipo, p.nombre AS producto, m.cantidad AS cantidad, "
              + "       NULL AS valor, m.idUsuario AS idUsuario, NULL AS cliente, NULL AS metodoPago, NULL AS idVenta "
              + "FROM MOVIMIENTO m JOIN PRODUCTO p ON p.idProducto = m.idProducto "
              + "UNION ALL "
              + "SELECT v.fecha, 'venta', p.nombre, dv.cantidad, dv.cantidad * dv.precioUnitario, "
              + "       v.idUsuario, c.nombre, v.metodoPago, v.idVenta "
              + "FROM DETALLE_VENTA dv "
              + "JOIN VENTA v ON v.idVenta = dv.idVenta "
              + "JOIN PRODUCTO p ON p.idProducto = dv.idProducto "
              + "JOIN CLIENTE c ON c.idCliente = v.idCliente "
              + "ORDER BY fecha DESC, idVenta DESC "
              + "LIMIT 5000";

        List<Actividad> historial = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                double valorLeido = rs.getDouble("valor");
                Double valor = rs.wasNull() ? null : valorLeido;
                int idUsuarioLeido = rs.getInt("idUsuario");
                Integer idUsuario = rs.wasNull() ? null : idUsuarioLeido;
                int idVentaLeido = rs.getInt("idVenta");
                Integer idVenta = rs.wasNull() ? null : idVentaLeido;

                historial.add(new Actividad(
                        rs.getTimestamp("fecha").toLocalDateTime(),
                        rs.getString("tipo"),
                        rs.getString("producto"),
                        rs.getInt("cantidad"),
                        valor,
                        idUsuario,
                        rs.getString("cliente"),
                        rs.getString("metodoPago"),
                        idVenta));
            }
        }
        return historial;
    }
}
