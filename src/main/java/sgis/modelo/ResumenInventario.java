package sgis.modelo;

/**
 * Objeto simple que agrupa los indicadores (KPIs) del Panel Principal.
 * No corresponde a ninguna tabla de la base de datos: se calcula
 * "al vuelo" a partir de PRODUCTO, VENTA y MOVIMIENTO (RF-07).
 */
public class ResumenInventario {

    private int totalProductos;
    private int stockSuficiente;
    private int stockMedio;
    private int stockBajo;
    private int stockAgotado;
    private double valorTotalInventario;
    private int ventasHoy;
    private double totalVentasHoy;
    private int movimientosHoy;

    public int getTotalProductos() {
        return totalProductos;
    }

    public void setTotalProductos(int totalProductos) {
        this.totalProductos = totalProductos;
    }

    public int getStockSuficiente() {
        return stockSuficiente;
    }

    public void setStockSuficiente(int stockSuficiente) {
        this.stockSuficiente = stockSuficiente;
    }

    public int getStockMedio() {
        return stockMedio;
    }

    public void setStockMedio(int stockMedio) {
        this.stockMedio = stockMedio;
    }

    public int getStockBajo() {
        return stockBajo;
    }

    public void setStockBajo(int stockBajo) {
        this.stockBajo = stockBajo;
    }

    public int getStockAgotado() {
        return stockAgotado;
    }

    public void setStockAgotado(int stockAgotado) {
        this.stockAgotado = stockAgotado;
    }

    public double getValorTotalInventario() {
        return valorTotalInventario;
    }

    public void setValorTotalInventario(double valorTotalInventario) {
        this.valorTotalInventario = valorTotalInventario;
    }

    public int getVentasHoy() {
        return ventasHoy;
    }

    public void setVentasHoy(int ventasHoy) {
        this.ventasHoy = ventasHoy;
    }

    public double getTotalVentasHoy() {
        return totalVentasHoy;
    }

    public void setTotalVentasHoy(double totalVentasHoy) {
        this.totalVentasHoy = totalVentasHoy;
    }

    public int getMovimientosHoy() {
        return movimientosHoy;
    }

    public void setMovimientosHoy(int movimientosHoy) {
        this.movimientosHoy = movimientosHoy;
    }
}
