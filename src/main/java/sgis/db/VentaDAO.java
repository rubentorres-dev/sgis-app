package sgis.db;

import sgis.modelo.DetalleVenta;
import sgis.modelo.Producto;
import sgis.modelo.ResumenDia;
import sgis.modelo.Venta;
import sgis.modelo.VentaResumen;
import sgis.util.DatosClienteException;
import sgis.util.StockInsuficienteException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * DAO para registrar una venta completa: cliente, venta, líneas de detalle
 * y descuento de stock, todo dentro de UNA sola transacción (CU-05).
 * Si cualquier producto no tiene stock suficiente, no se guarda nada.
 */
public class VentaDAO {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    /**
     * Registra la venta completa en UNA transacción: cliente, venta, detalle y descuento de stock.
     * Si dos ventas chocan por un interbloqueo (deadlock) de la base de datos, se reintenta sola:
     * la venta que fallaba no se había guardado, así que repetirla es seguro.
     */
    public void registrarVenta(Venta venta, String nombreCliente, String correoCliente, String cedulaCliente, String telefonoCliente)
            throws SQLException, StockInsuficienteException, DatosClienteException {
        int intento = 0;
        while (true) {
            try {
                registrarVentaUnaVez(venta, nombreCliente, correoCliente, cedulaCliente, telefonoCliente);
                return;
            } catch (SQLException e) {
                // SQLState 40001 = interbloqueo.
                if ("40001".equals(e.getSQLState()) && ++intento < 4) {
                    try {
                        Thread.sleep(30L * intento + (long) (Math.random() * 40));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                    continue;
                }
                throw e;
            }
        }
    }

    private void registrarVentaUnaVez(Venta venta, String nombreCliente, String correoCliente, String cedulaCliente, String telefonoCliente)
            throws SQLException, StockInsuficienteException, DatosClienteException {

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                // 1. Cliente (buscar o crear)
                int idCliente = clienteDAO.obtenerOCrearCliente(conexion, nombreCliente, correoCliente, cedulaCliente, telefonoCliente);
                venta.setIdCliente(idCliente);
                venta.setFecha(LocalDateTime.now());
                venta.setEstadoEnvio("pendiente");

                // 2. Validar stock de TODOS los productos antes de descontar nada.
                //    - Si un producto está repetido en varias líneas, se suman sus cantidades.
                //    - Los productos se bloquean SIEMPRE en orden de id: así dos ventas simultáneas con los
                //      mismos productos no se bloquean entre sí (interbloqueo).
                Map<Integer, Integer> pedido = new TreeMap<>();
                Map<Integer, String> nombres = new HashMap<>();
                for (DetalleVenta linea : venta.getDetalles()) {
                    pedido.merge(linea.getIdProducto(), linea.getCantidad(), Integer::sum);
                    nombres.putIfAbsent(linea.getIdProducto(), linea.getNombreProducto());
                }
                Map<Integer, Producto> productos = new HashMap<>();
                for (Map.Entry<Integer, Integer> pedidoDeProducto : pedido.entrySet()) {
                    Producto producto = productoDAO.obtenerPorId(conexion, pedidoDeProducto.getKey());
                    if (producto == null) {
                        conexion.rollback();
                        throw new StockInsuficienteException(
                            "El producto '" + nombres.get(pedidoDeProducto.getKey()) + "' ya no existe.");
                    }
                    if (!"activo".equals(producto.getEstado())) {
                        conexion.rollback();
                        throw new StockInsuficienteException(
                            "El producto '" + producto.getNombre() + "' está desactivado y no se puede vender.");
                    }
                    if (pedidoDeProducto.getValue() > producto.getCantidadDisponible()) {
                        conexion.rollback();
                        throw new StockInsuficienteException(
                            "Stock insuficiente para '" + producto.getNombre() + "': "
                            + "se pidieron " + pedidoDeProducto.getValue() + ", disponibles " + producto.getCantidadDisponible() + ".");
                    }
                    productos.put(pedidoDeProducto.getKey(), producto);
                }
                for (DetalleVenta linea : venta.getDetalles()) {
                    linea.setPrecioUnitario(productos.get(linea.getIdProducto()).getPrecio()); // precio actual del catálogo
                }

                // 3. Insertar la VENTA
                int idVenta = insertarVenta(conexion, venta);
                venta.setIdVenta(idVenta);

                // 4. Insertar cada línea de DETALLE_VENTA y descontar stock (el descuento nunca deja el stock negativo)
                for (DetalleVenta linea : venta.getDetalles()) {
                    linea.setIdVenta(idVenta);
                    insertarDetalleVenta(conexion, linea);
                    if (!productoDAO.descontarStockSiHay(conexion, linea.getIdProducto(), linea.getCantidad())) {
                        conexion.rollback();
                        throw new StockInsuficienteException("Stock insuficiente para '"
                                + productos.get(linea.getIdProducto()).getNombre() + "'. Intenta de nuevo.");
                    }
                }

                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    /**
     * Actualiza el estado de envío del comprobante (pendiente -> enviado / fallido),
     * después de intentar enviar el correo (esto ya NO es parte de la transacción
     * anterior, porque el envío de correo puede tardar o fallar por red).
     */
    /** Cambia el estado de envío SOLO si sigue «pendiente» (el correo se envía en segundo plano y no debe pisar «completado»). */
    public void actualizarEstadoEnvioSiPendiente(int idVenta, String nuevoEstado) throws SQLException {
        String sql = "UPDATE VENTA SET estadoEnvio = ? WHERE idVenta = ? AND estadoEnvio = 'pendiente'";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idVenta);
            stmt.executeUpdate();
        }
    }

    /**
     * Si la columna estadoEnvio todavía no admite «completado» (la base de datos es anterior al script
     * actualizar_estado_comprobante.sql), la corrige. Es seguro llamarlo varias veces.
     */
    public void asegurarEstadoCompletado() throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion(); Statement st = conexion.createStatement()) {
            String tipo = null;
            try (ResultSet rs = st.executeQuery("SHOW COLUMNS FROM VENTA LIKE 'estadoEnvio'")) {
                if (rs.next()) {
                    tipo = rs.getString("Type");
                }
            }
            if (tipo != null && !tipo.toLowerCase().contains("completado")) {
                st.executeUpdate("ALTER TABLE VENTA MODIFY estadoEnvio "
                        + "ENUM('pendiente', 'enviado', 'fallido', 'completado') NOT NULL DEFAULT 'pendiente'");
            }
        }
    }

    public void actualizarEstadoEnvio(int idVenta, String nuevoEstado) throws SQLException {
        String sql = "UPDATE VENTA SET estadoEnvio = ? WHERE idVenta = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idVenta);
            stmt.executeUpdate();
        }
    }

    /**
     * Lee una venta completa (con su cliente y sus productos) para reconstruir su comprobante
     * o su factura en cualquier momento.
     * @return la venta, o null si no existe.
     */
    public Venta obtenerVentaCompleta(int idVenta) throws SQLException {
        String sqlVenta = "SELECT v.idVenta, v.fecha, v.estadoEnvio, v.metodoPago, v.idCliente, v.idUsuario, "
                        + "c.nombre AS nombreCliente, c.correo AS correoCliente "
                        + "FROM VENTA v JOIN CLIENTE c ON c.idCliente = v.idCliente WHERE v.idVenta = ?";
        String sqlDetalle = "SELECT dv.idDetalleVenta, dv.cantidad, dv.precioUnitario, dv.idProducto, p.nombre "
                          + "FROM DETALLE_VENTA dv JOIN PRODUCTO p ON p.idProducto = dv.idProducto "
                          + "WHERE dv.idVenta = ? ORDER BY dv.idDetalleVenta";

        try (Connection conexion = ConexionBD.obtenerConexion()) {
            Venta venta = new Venta();

            try (PreparedStatement stmt = conexion.prepareStatement(sqlVenta)) {
                stmt.setInt(1, idVenta);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    venta.setIdVenta(rs.getInt("idVenta"));
                    venta.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    venta.setEstadoEnvio(rs.getString("estadoEnvio"));
                    venta.setMetodoPago(rs.getString("metodoPago"));
                    venta.setIdCliente(rs.getInt("idCliente"));
                    int idUsuarioLeido = rs.getInt("idUsuario");
                    venta.setIdUsuario(rs.wasNull() ? null : idUsuarioLeido);
                    venta.setNombreCliente(rs.getString("nombreCliente"));
                    venta.setCorreoCliente(rs.getString("correoCliente"));
                }
            }

            List<DetalleVenta> detalles = new ArrayList<>();
            try (PreparedStatement stmt = conexion.prepareStatement(sqlDetalle)) {
                stmt.setInt(1, idVenta);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        DetalleVenta linea = new DetalleVenta(
                                rs.getInt("idProducto"), rs.getString("nombre"),
                                rs.getInt("cantidad"), rs.getDouble("precioUnitario"));
                        linea.setIdDetalleVenta(rs.getInt("idDetalleVenta"));
                        linea.setIdVenta(idVenta);
                        detalles.add(linea);
                    }
                }
            }
            venta.setDetalles(detalles);
            return venta;
        }
    }

    private int insertarVenta(Connection conexion, Venta venta) throws SQLException {
        String sql = "INSERT INTO VENTA (fecha, estadoEnvio, idCliente, metodoPago, idUsuario) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setTimestamp(1, Timestamp.valueOf(venta.getFecha()));
            stmt.setString(2, venta.getEstadoEnvio());
            stmt.setInt(3, venta.getIdCliente());
            stmt.setString(4, venta.getMetodoPago());
            if (venta.getIdUsuario() == null) {
                stmt.setNull(5, Types.INTEGER);
            } else {
                stmt.setInt(5, venta.getIdUsuario());
            }
            stmt.executeUpdate();

            try (var keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("No se pudo obtener el id de la venta recién creada.");
            }
        }
    }

    private void insertarDetalleVenta(Connection conexion, DetalleVenta linea) throws SQLException {
        String sql = "INSERT INTO DETALLE_VENTA (cantidad, precioUnitario, idVenta, idProducto) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, linea.getCantidad());
            stmt.setDouble(2, linea.getPrecioUnitario());
            stmt.setInt(3, linea.getIdVenta());
            stmt.setInt(4, linea.getIdProducto());
            stmt.executeUpdate();
        }
    }

    /**
     * Devuelve el historial de ventas (resumen), del más reciente al más antiguo,
     * para la pantalla de Reportes. Sin fechas: todas las ventas.
     */
    public List<VentaResumen> listarVentasResumen() throws SQLException {
        return listarVentasResumen(null, null);
    }

    /**
     * Igual que el anterior, pero solo las ventas entre dos fechas (ambas incluidas).
     * Si alguna fecha es null, no se aplica ese límite.
     */
    public List<VentaResumen> listarVentasResumen(LocalDate desde, LocalDate hasta) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT v.idVenta, v.fecha, c.nombre AS nombreCliente, c.correo AS correoCliente, "
              + "v.estadoEnvio, v.metodoPago, v.idUsuario, "
              + "COALESCE(SUM(dv.cantidad * dv.precioUnitario), 0) AS total "
              + "FROM VENTA v "
              + "JOIN CLIENTE c ON c.idCliente = v.idCliente "
              + "LEFT JOIN DETALLE_VENTA dv ON dv.idVenta = v.idVenta "
              + "WHERE 1 = 1 ");
        if (desde != null) sql.append("AND DATE(v.fecha) >= ? ");
        if (hasta != null) sql.append("AND DATE(v.fecha) <= ? ");
        sql.append("GROUP BY v.idVenta, v.fecha, c.nombre, c.correo, v.estadoEnvio, v.metodoPago, v.idUsuario ")
           .append("ORDER BY v.fecha DESC");

        List<VentaResumen> ventas = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql.toString())) {

            int indice = 1;
            if (desde != null) stmt.setDate(indice++, java.sql.Date.valueOf(desde));
            if (hasta != null) stmt.setDate(indice++, java.sql.Date.valueOf(hasta));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int idUsuarioLeido = rs.getInt("idUsuario");
                    Integer idUsuario = rs.wasNull() ? null : idUsuarioLeido;
                    ventas.add(new VentaResumen(
                            rs.getInt("idVenta"),
                            rs.getTimestamp("fecha").toLocalDateTime(),
                            rs.getString("nombreCliente"),
                            rs.getString("correoCliente"),
                            rs.getString("estadoEnvio"),
                            rs.getString("metodoPago"),
                            idUsuario,
                            rs.getDouble("total")
                    ));
                }
            }
        }
        return ventas;
    }

    /**
     * Total vendido y número de ventas por cada día entre dos fechas (ambas incluidas).
     * Solo devuelve los días en que hubo ventas.
     */
    public List<ResumenDia> totalesPorDia(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT DATE(v.fecha) AS dia, COUNT(DISTINCT v.idVenta) AS ventas, "
                   + "COALESCE(SUM(dv.cantidad * dv.precioUnitario), 0) AS total "
                   + "FROM VENTA v LEFT JOIN DETALLE_VENTA dv ON dv.idVenta = v.idVenta "
                   + "WHERE DATE(v.fecha) >= ? AND DATE(v.fecha) <= ? "
                   + "GROUP BY DATE(v.fecha) ORDER BY dia";

        List<ResumenDia> dias = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(desde));
            stmt.setDate(2, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dias.add(new ResumenDia(
                            rs.getDate("dia").toLocalDate(),
                            rs.getInt("ventas"),
                            rs.getDouble("total")));
                }
            }
        }
        return dias;
    }
}
