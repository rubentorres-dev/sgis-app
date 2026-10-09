package sgis.util;

/**
 * Estado del comprobante de una venta (columna estadoEnvio de la tabla VENTA):
 *
 *   completado → se entregó el comprobante en PDF (factura generada)
 *   enviado    → se envió por correo electrónico
 *   fallido    → el envío por correo falló y todavía no se ha entregado el PDF
 *   pendiente  → aún no se ha entregado por ningún medio
 *
 * Para el usuario solo hay dos estados: "Completado" (completado o enviado) y "Pendiente".
 */
public final class EstadoComprobante {

    public static final String COMPLETADO = "completado";
    public static final String ENVIADO = "enviado";
    public static final String FALLIDO = "fallido";
    public static final String PENDIENTE = "pendiente";

    private EstadoComprobante() {
    }

    public static boolean completado(String estadoEnvio) {
        return COMPLETADO.equals(estadoEnvio) || ENVIADO.equals(estadoEnvio);
    }

    public static String etiqueta(String estadoEnvio) {
        return completado(estadoEnvio) ? "Completado" : "Pendiente";
    }

    /** Explicación para el mensaje emergente. */
    public static String detalle(String estadoEnvio) {
        if (COMPLETADO.equals(estadoEnvio)) {
            return "El comprobante se entregó en PDF.";
        }
        if (ENVIADO.equals(estadoEnvio)) {
            return "El comprobante se envió por correo electrónico.";
        }
        if (FALLIDO.equals(estadoEnvio)) {
            return "El envío por correo falló y aún no se ha entregado el PDF. Ábrelo en Comprobantes para generarlo.";
        }
        return "Aún no se ha entregado el comprobante.";
    }
}
