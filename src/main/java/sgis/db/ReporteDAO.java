package sgis.db;

import sgis.modelo.ResumenInventario;
import sgis.util.EstadoStock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DAO de solo lectura para los indicadores (KPIs) del Panel Principal (RF-07).
 * No existe una tabla REPORTE en la base de datos: estos valores se calculan
 * "al vuelo" a partir de PRODUCTO, VENTA y MOVIMIENTO cada vez que se consultan.
 */
public class ReporteDAO {

    public ResumenInventario obtenerResumen() throws SQLException {
        ResumenInventario resumen = new ResumenInventario();

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            resumen.setTotalProductos(contarProductosActivos(conexion));
            resumen.setStockSuficiente(contarPorRangoStock(conexion, EstadoStock.UMBRAL_MEDIO + 1, Integer.MAX_VALUE));
            resumen.setStockMedio(contarPorRangoStock(conexion, EstadoStock.UMBRAL_BAJO + 1, EstadoStock.UMBRAL_MEDIO));
            resumen.setStockBajo(contarPorRangoStock(conexion, 1, EstadoStock.UMBRAL_BAJO));
            resumen.setStockAgotado(contarPorRangoStock(conexion, 0, 0));
            resumen.setValorTotalInventario(calcularValorInventario(conexion));
            resumen.setVentasHoy(contarVentasHoy(conexion));
            resumen.setTotalVentasHoy(sumarTotalVentasHoy(conexion));
            resumen.setMovimientosHoy(contarMovimientosHoy(conexion));
        }
        return resumen;
    }

    private int contarProductosActivos(Connection conexion) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM PRODUCTO WHERE estado = 'activo'";
        return ejecutarConteo(conexion, sql);
    }

    private int contarPorRangoStock(Connection conexion, int minimo, int maximo) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM PRODUCTO "
                   + "WHERE estado = 'activo' AND cantidadDisponible BETWEEN ? AND ?";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, minimo);
            stmt.setInt(2, maximo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    private double calcularValorInventario(Connection conexion) throws SQLException {
        String sql = "SELECT COALESCE(SUM(precio * cantidadDisponible), 0) AS valor "
                   + "FROM PRODUCTO WHERE estado = 'activo'";
        try (PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getDouble("valor") : 0;
        }
    }

    private int contarVentasHoy(Connection conexion) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM VENTA WHERE DATE(fecha) = CURDATE()";
        return ejecutarConteo(conexion, sql);
    }

    private double sumarTotalVentasHoy(Connection conexion) throws SQLException {
        String sql = "SELECT COALESCE(SUM(dv.cantidad * dv.precioUnitario), 0) AS total "
                   + "FROM VENTA v JOIN DETALLE_VENTA dv ON dv.idVenta = v.idVenta "
                   + "WHERE DATE(v.fecha) = CURDATE()";
        try (PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getDouble("total") : 0;
        }
    }

    private int contarMovimientosHoy(Connection conexion) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM MOVIMIENTO WHERE DATE(fecha) = CURDATE()";
        return ejecutarConteo(conexion, sql);
    }

    private int ejecutarConteo(Connection conexion, String sql) throws SQLException {
        try (PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    /**
     * Unidades en inventario de cada producto activo con existencias, de mayor a menor.
     * Se usa para la gráfica circular "Stock por producto" del Panel Principal.
     * Se devuelve como lista (y no como mapa) porque dos productos podrían llamarse igual.
     */
    public List<Map.Entry<String, Integer>> obtenerStockPorProducto() throws SQLException {
        String sql = "SELECT nombre, cantidadDisponible FROM PRODUCTO "
                   + "WHERE estado = 'activo' AND cantidadDisponible > 0 "
                   + "ORDER BY cantidadDisponible DESC, nombre";

        List<Map.Entry<String, Integer>> stock = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                stock.add(new AbstractMap.SimpleEntry<>(rs.getString("nombre"), rs.getInt("cantidadDisponible")));
            }
        }
        return stock;
    }
}
