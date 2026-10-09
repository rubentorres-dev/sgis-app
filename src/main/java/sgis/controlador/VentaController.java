package sgis.controlador;

import sgis.db.VentaDAO;
import sgis.modelo.DetalleVenta;
import sgis.modelo.ResumenDia;
import sgis.modelo.Venta;
import sgis.modelo.VentaResumen;
import sgis.servicio.ComprobanteService;
import sgis.servicio.EmailService;
import sgis.util.StockInsuficienteException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador del módulo de Ventas (CU-05).
 * Orquesta: registrar la venta en la base de datos, generar el comprobante,
 * y enviarlo por correo al cliente.
 */
public class VentaController {

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ComprobanteService comprobanteService = new ComprobanteService();
    private final EmailService emailService = new EmailService();

    /** Último comprobante generado, para poder mostrarlo en pantalla tras la venta. */
    private String ultimoComprobante;

    /** Última venta registrada con éxito, para poder generar su factura en PDF. */
    private Venta ultimaVenta;
    private String ultimoNombreCliente;
    private String ultimoCorreoCliente;

    /**
     * Registra una venta completa.
     * @return null si todo salió bien, o un mensaje de error para mostrar en pantalla.
     */
    public String registrarVenta(String nombreCliente, String correoCliente, String metodoPago,
                                 Integer idUsuario, List<DetalleVenta> lineas) {
        Venta venta = new Venta();
        venta.setDetalles(lineas);
        venta.setMetodoPago(metodoPago);
        venta.setIdUsuario(idUsuario);

        try {
            // 1. Guardar la venta, el detalle y descontar stock (todo transaccional)
            ventaDAO.registrarVenta(venta, nombreCliente, correoCliente);
        } catch (StockInsuficienteException e) {
            return e.getMessage();
        } catch (SQLException e) {
            return "Error al registrar la venta: " + e.getMessage();
        }

        // 2. Generar el comprobante (esto ya no puede fallar la venta, que ya quedó guardada)
        ultimaVenta = venta;
        ultimoNombreCliente = nombreCliente;
        ultimoCorreoCliente = correoCliente;
        ultimoComprobante = comprobanteService.generarComprobante(venta, nombreCliente);

        // 3. Intentar enviarlo por correo
        boolean enviado = emailService.enviarComprobante(
                correoCliente,
                "Comprobante de compra Nº " + venta.getIdVenta() + " - Abasto La Barata",
                ultimoComprobante
        );

        try {
            ventaDAO.actualizarEstadoEnvio(venta.getIdVenta(), enviado ? "enviado" : "fallido");
        } catch (SQLException e) {
            // La venta ya se guardó correctamente; si esto falla, solo se pierde
            // la actualización del estado del envío, no la venta en sí.
            System.out.println("No se pudo actualizar el estado de envío: " + e.getMessage());
        }

        return null; // La venta se registró correctamente (el envío del correo se reporta aparte).
    }

    public String getUltimoComprobante() {
        return ultimoComprobante;
    }

    public Venta getUltimaVenta() {
        return ultimaVenta;
    }

    public String getUltimoNombreCliente() {
        return ultimoNombreCliente;
    }

    public String getUltimoCorreoCliente() {
        return ultimoCorreoCliente;
    }

    public List<VentaResumen> obtenerVentasResumen() {
        try {
            return ventaDAO.listarVentasResumen();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar las ventas: " + e.getMessage(), e);
        }
    }

    /** Ventas entre dos fechas (ambas incluidas), para el informe de ventas. */
    public List<VentaResumen> obtenerVentasResumen(LocalDate desde, LocalDate hasta) {
        try {
            return ventaDAO.listarVentasResumen(desde, hasta);
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar las ventas: " + e.getMessage(), e);
        }
    }

    /** Total vendido por día entre dos fechas (para la gráfica de la semana). */
    public List<ResumenDia> obtenerTotalesPorDia(LocalDate desde, LocalDate hasta) {
        try {
            return ventaDAO.totalesPorDia(desde, hasta);
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar las ventas por día: " + e.getMessage(), e);
        }
    }

    /** Lee una venta completa para reconstruir su comprobante. @return null si no existe. */
    public Venta obtenerVentaCompleta(int idVenta) {
        try {
            return ventaDAO.obtenerVentaCompleta(idVenta);
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar la venta: " + e.getMessage(), e);
        }
    }

    /**
     * Marca el comprobante como completado (se entregó el PDF).
     * @return null si salió bien, o un mensaje de error.
     */
    public String marcarComprobanteCompletado(int idVenta) {
        try {
            ventaDAO.actualizarEstadoEnvio(idVenta, "completado");
            return null;
        } catch (SQLException e) {
            return "No se pudo actualizar el estado del comprobante: " + e.getMessage();
        }
    }
}
