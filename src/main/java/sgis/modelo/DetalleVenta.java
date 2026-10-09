package sgis.modelo;

/**
 * Clase de modelo que representa una línea de una venta
 * (un producto y la cantidad comprada dentro de una venta específica).
 * Corresponde 1:1 con la tabla DETALLE_VENTA de la base de datos.
 */
public class DetalleVenta {

    private int idDetalleVenta;
    private int cantidad;
    private double precioUnitario;
    private int idVenta;
    private int idProducto;

    // Solo para mostrar en el comprobante; no se guarda en esta tabla.
    private String nombreProducto;

    public DetalleVenta() {
    }

    public DetalleVenta(int idProducto, String nombreProducto, int cantidad, double precioUnitario) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public double getSubtotal() {
        return cantidad * precioUnitario;
    }

    public int getIdDetalleVenta() {
        return idDetalleVenta;
    }

    public void setIdDetalleVenta(int idDetalleVenta) {
        this.idDetalleVenta = idDetalleVenta;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }
}
