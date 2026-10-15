package sgis.controlador;

import sgis.db.VentaDAO;
import sgis.modelo.DetalleVenta;
import sgis.modelo.ResumenDia;
import sgis.modelo.Venta;
import sgis.modelo.VentaResumen;
import sgis.servicio.ComprobanteService;
import sgis.servicio.EmailService;
import sgis.util.Cedula;
import sgis.util.MensajesBD;
import sgis.util.Telefono;
import sgis.util.DatosClienteException;
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
    public String registrarVenta(String nombreCliente, String correoCliente, String cedulaCliente, String telefonoCliente, String metodoPago,
                                 Integer idUsuario, List<DetalleVenta> lineas) {
        // La pantalla ya valida casi todo; esto protege al sistema si algo llega sin pasar por ella.
        if (lineas == null || lineas.isEmpty()) {
            return "Agrega al menos un producto a la venta.";
        }
        for (DetalleVenta linea : lineas) {
            if (linea == null || linea.getCantidad() <= 0) {
                return "Las cantidades deben ser mayores que 0.";
            }
        }
        if (nombreCliente == null || nombreCliente.isBlank()) {
            return "Escribe el nombre del cliente.";
        }
        String errorCedula = Cedula.validar(cedulaCliente);
        if (errorCedula != null) {
            return errorCedula;
        }
        final String cedula = Cedula.normalizar(cedulaCliente);
        String errorTelefono = Telefono.validar(telefonoCliente);
        if (errorTelefono != null) {
            return errorTelefono;
        }
        final String telefono = Telefono.normalizar(telefonoCliente);
        if (!ClienteController.correoValido(correoCliente)) {
            return "Escribe un correo válido para el cliente (por ejemplo, nombre@correo.com).";
        }

        Venta venta = new Venta();
        venta.setDetalles(lineas);
        venta.setMetodoPago(metodoPago);
        venta.setIdUsuario(idUsuario);

        try {
            // 1. Guardar la venta, el detalle y descontar stock (todo transaccional)
            ventaDAO.registrarVenta(venta, nombreCliente, correoCliente, cedula, telefono);
        } catch (StockInsuficienteException | DatosClienteException e) {
            return e.getMessage();
        } catch (SQLException e) {
            String falta = MensajesBD.scriptFaltante(e);
            if (falta != null) {
                return falta;
            }
            return "Error al registrar la venta: " + e.getMessage();
        }

        // 2. Generar el comprobante (esto ya no puede fallar la venta, que ya quedó guardada)
        ultimaVenta = venta;
        ultimoNombreCliente = nombreCliente;
        ultimoCorreoCliente = correoCliente;
        ultimoComprobante = comprobanteService.generarComprobante(venta, nombreCliente);

        // 3. Enviarlo por correo EN SEGUNDO PLANO: sin internet o con un servidor que no responde, la pantalla
        //    no se congela. El estado queda «pendiente» y pasa a «enviado» o «fallido» cuando termine el intento.
        final int idVenta = venta.getIdVenta();
        final String comprobante = ultimoComprobante;
        Thread envio = new Thread(() -> {
            boolean enviado = emailService.enviarComprobante(
                    correoCliente,
                    "Comprobante de compra Nº " + idVenta + " - Abasto La Barata",
                    comprobante
            );
            try {
                ventaDAO.actualizarEstadoEnvioSiPendiente(idVenta, enviado ? "enviado" : "fallido");
            } catch (SQLException e) {
                // La venta ya se guardó correctamente; si esto falla, solo se pierde
                // la actualización del estado del envío, no la venta en sí.
                System.out.println("No se pudo actualizar el estado de envío: " + e.getMessage());
            }
        }, "envio-correo");
        envio.setDaemon(true);
        envio.start();

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
            if (esEstadoNoPermitido(e)) {
                // La base de datos es anterior al script actualizar_estado_comprobante.sql: se corrige sola y se reintenta.
                try {
                    ventaDAO.asegurarEstadoCompletado();
                    ventaDAO.actualizarEstadoEnvio(idVenta, "completado");
                    return null;
                } catch (SQLException e2) {
                    return "El comprobante no pudo marcarse como completado: falta ejecutar el script "
                            + "actualizar_estado_comprobante.sql en la base de datos.";
                }
            }
            return "No se pudo actualizar el estado del comprobante: " + e.getMessage();
        }
    }

    /** «Data truncated for column 'estadoEnvio'»: MySQL no acepta el valor porque la columna no tiene «completado». */
    private boolean esEstadoNoPermitido(SQLException e) {
        return e.getErrorCode() == 1265
                || (e.getMessage() != null && e.getMessage().contains("Data truncated") && e.getMessage().contains("estadoEnvio"));
    }
}
