package sgis.servicio;

import sgis.modelo.DetalleVenta;
import sgis.modelo.Venta;
import sgis.util.FormatoMoneda;

import java.time.format.DateTimeFormatter;

/**
 * Genera el texto del comprobante de compra.
 * IMPORTANTE (RF-08 / nota legal): este comprobante es solo un recibo
 * informativo de la compra. NO es una factura electrónica con validez
 * fiscal ante la DIAN.
 */
public class ComprobanteService {

    public String generarComprobante(Venta venta, String nombreCliente) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        StringBuilder sb = new StringBuilder();
        sb.append("==============================================\n");
        sb.append("   SUPERMERCADO ABASTO LA BARATA\n");
        sb.append("   Comprobante de compra Nº ").append(venta.getIdVenta()).append("\n");
        sb.append("==============================================\n");
        sb.append("Fecha: ").append(venta.getFecha().format(formato)).append("\n");
        sb.append("Cliente: ").append(nombreCliente).append("\n");
        sb.append("Método de pago: ").append(venta.getMetodoPago()).append("\n");
        sb.append("----------------------------------------------\n");
        sb.append(String.format("%-20s %5s %12s %12s%n", "Producto", "Cant.", "Precio", "Subtotal"));

        for (DetalleVenta linea : venta.getDetalles()) {
            sb.append(String.format("%-20s %5d %12s %12s%n",
                    linea.getNombreProducto(), linea.getCantidad(),
                    FormatoMoneda.formatear(linea.getPrecioUnitario()),
                    FormatoMoneda.formatear(linea.getSubtotal())));
        }

        sb.append("----------------------------------------------\n");
        sb.append("TOTAL: ").append(FormatoMoneda.formatear(venta.getTotal())).append("\n");
        sb.append("==============================================\n");
        sb.append("Este comprobante es un recibo informativo de la\n");
        sb.append("compra realizada. No corresponde a una factura\n");
        sb.append("electrónica con validez fiscal ante la DIAN.\n");

        return sb.toString();
    }
}
