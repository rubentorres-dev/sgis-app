package sgis.modelo;

import java.time.LocalDateTime;

/**
 * Una línea del historial de movimientos: algo que pasó con el inventario o las ventas.
 * No corresponde a una tabla: se arma uniendo MOVIMIENTO (entradas y salidas de inventario)
 * con DETALLE_VENTA (cada producto vendido).
 */
public class Actividad {

    public static final String VENTA = "venta";
    public static final String ENTRADA = "entrada";
    public static final String SALIDA = "salida";

    private final LocalDateTime fecha;
    private final String tipo;       // "venta", "entrada" o "salida"
    private final String producto;
    private final int cantidad;
    private final Double valor;      // total de la línea (solo en ventas)
    private final Integer idUsuario; // quién lo registró (null en ventas anteriores al control de usuarios)
    private final String cliente;    // quién compró (solo en ventas)
    private final String metodoPago; // solo en ventas
    private final Integer idVenta;   // solo en ventas

    public Actividad(LocalDateTime fecha, String tipo, String producto, int cantidad, Double valor,
                     Integer idUsuario, String cliente, String metodoPago, Integer idVenta) {
        this.fecha = fecha;
        this.tipo = tipo;
        this.producto = producto;
        this.cantidad = cantidad;
        this.valor = valor;
        this.idUsuario = idUsuario;
        this.cliente = cliente;
        this.metodoPago = metodoPago;
        this.idVenta = idVenta;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public String getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public Double getValor() {
        return valor;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getCliente() {
        return cliente;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public Integer getIdVenta() {
        return idVenta;
    }

    /** true si el movimiento hizo bajar las existencias (ventas y salidas). */
    public boolean esSalidaDeStock() {
        return !ENTRADA.equals(tipo);
    }

    /** Frase que describe lo ocurrido, por ejemplo "Venta N° 12: 3 × Arroz Diana 1kg (Efectivo)". */
    public String getDescripcion() {
        if (VENTA.equals(tipo)) {
            String pago = metodoPago == null || metodoPago.isEmpty() ? ""
                    : " (" + Character.toUpperCase(metodoPago.charAt(0)) + metodoPago.substring(1) + ")";
            return "Venta N° " + idVenta + ": " + cantidad + " × " + producto + pago;
        }
        if (ENTRADA.equals(tipo)) {
            return "Entrada de inventario: " + cantidad + " × " + producto;
        }
        return "Salida de inventario: " + cantidad + " × " + producto;
    }
}
