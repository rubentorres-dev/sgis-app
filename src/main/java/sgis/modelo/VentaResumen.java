package sgis.modelo;

import java.time.LocalDateTime;

/**
 * Resumen de una venta para mostrar en listados (Reportes).
 * No corresponde directamente a una tabla: junta datos de VENTA, CLIENTE
 * y la suma de DETALLE_VENTA.
 */
public class VentaResumen {

    private int idVenta;
    private LocalDateTime fecha;
    private String nombreCliente;
    private String correoCliente;
    private String estadoEnvio;
    private String metodoPago;
    private Integer idUsuario;
    private double total;

    public VentaResumen(int idVenta, LocalDateTime fecha, String nombreCliente, String correoCliente, String estadoEnvio, String metodoPago, Integer idUsuario, double total) {
        this.idVenta = idVenta;
        this.fecha = fecha;
        this.nombreCliente = nombreCliente;
        this.correoCliente = correoCliente;
        this.estadoEnvio = estadoEnvio;
        this.metodoPago = metodoPago;
        this.idUsuario = idUsuario;
        this.total = total;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public String getCorreoCliente() {
        return correoCliente;
    }

    public String getEstadoEnvio() {
        return estadoEnvio;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public double getTotal() {
        return total;
    }
}
