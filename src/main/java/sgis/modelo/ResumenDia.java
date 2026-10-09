package sgis.modelo;

import java.time.LocalDate;

/**
 * Total vendido en un día (para la gráfica de "Ventas de la semana").
 */
public class ResumenDia {

    private final LocalDate fecha;
    private final int cantidadVentas;
    private final double total;

    public ResumenDia(LocalDate fecha, int cantidadVentas, double total) {
        this.fecha = fecha;
        this.cantidadVentas = cantidadVentas;
        this.total = total;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public int getCantidadVentas() {
        return cantidadVentas;
    }

    public double getTotal() {
        return total;
    }
}
