package sgis.modelo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Clase de modelo que representa una venta realizada en caja.
 * Corresponde 1:1 con la tabla VENTA de la base de datos.
 */
public class Venta {

    private int idVenta;
    private LocalDateTime fecha;
    private String estadoEnvio; // "pendiente", "enviado", "fallido" o "completado"
    private int idCliente;
    private String metodoPago = "efectivo"; // "efectivo", "tarjeta" o "transferencia"
    // Solo para mostrar el comprobante (no son columnas de la tabla VENTA):
    private String nombreCliente;
    private String correoCliente;

    private Integer idUsuario;                // usuario del sistema que registró la venta (puede ser null en ventas antiguas)

    // No se guarda en la tabla VENTA; se arma en memoria para el comprobante.
    private List<DetalleVenta> detalles;

    public Venta() {
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getEstadoEnvio() {
        return estadoEnvio;
    }

    public void setEstadoEnvio(String estadoEnvio) {
        this.estadoEnvio = estadoEnvio;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getCorreoCliente() {
        return correoCliente;
    }

    public void setCorreoCliente(String correoCliente) {
        this.correoCliente = correoCliente;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    public double getTotal() {
        if (detalles == null) return 0;
        return detalles.stream().mapToDouble(DetalleVenta::getSubtotal).sum();
    }
}
