package sgis.vista;

import sgis.util.EstadoComprobante;
import sgis.util.EstadoStock;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/**
 * Crea las "insignias" de color (etiquetas redondeadas) que se usan en las tablas:
 * estado del stock y estado del comprobante.
 */
public final class Insignias {

    private Insignias() {
    }

    /** Insignia del nivel de stock: Agotado, Stock bajo, Stock medio o Suficiente. */
    public static Label stock(int cantidad) {
        EstadoStock.Nivel nivel = EstadoStock.nivelDe(cantidad);
        Label insignia = new Label(nivel.getEtiqueta());
        insignia.getStyleClass().add(nivel.getClaseCss());
        Tooltip.install(insignia, new Tooltip(cantidad + " unidades"));
        return insignia;
    }

    /** Insignia del comprobante de una venta: Completado o Pendiente. */
    public static Label comprobante(String estadoEnvio) {
        Label insignia = new Label(EstadoComprobante.etiqueta(estadoEnvio));
        insignia.getStyleClass().add(EstadoComprobante.completado(estadoEnvio) ? "badge-suficiente" : "badge-medio");
        Tooltip.install(insignia, new Tooltip(EstadoComprobante.detalle(estadoEnvio)));
        return insignia;
    }
}
