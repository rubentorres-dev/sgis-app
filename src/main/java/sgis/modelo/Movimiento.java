package sgis.modelo;

import java.time.LocalDateTime;

/**
 * Clase de modelo que representa un movimiento de inventario
 * (una entrada o una salida de productos).
 * Corresponde 1:1 con la tabla MOVIMIENTO de la base de datos.
 */
public class Movimiento {

    private int idMovimiento;
    private String tipo; // "entrada" o "salida"
    private int cantidad;
    private LocalDateTime fecha;
    private int idProducto;
    private int idUsuario;

    public Movimiento() {
    }

    public Movimiento(int idMovimiento, String tipo, int cantidad, LocalDateTime fecha,
                       int idProducto, int idUsuario) {
        this.idMovimiento = idMovimiento;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.fecha = fecha;
        this.idProducto = idProducto;
        this.idUsuario = idUsuario;
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }
}
