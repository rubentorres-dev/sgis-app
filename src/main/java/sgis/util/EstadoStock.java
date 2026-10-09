package sgis.util;

/**
 * Niveles de existencias de un producto. Los umbrales viven aquí, en un solo lugar:
 * para cambiar cuándo un producto se considera "bajo", "medio" o "suficiente" basta
 * con editar estas dos constantes.
 *
 *   Agotado:     0 unidades                     (rojo oscuro)
 *   Stock bajo:  de 1 a UMBRAL_BAJO unidades     (rojo)
 *   Stock medio: hasta UMBRAL_MEDIO unidades     (amarillo)
 *   Suficiente:  más que UMBRAL_MEDIO            (verde)
 */
public final class EstadoStock {

    /** Con esta cantidad o menos (y más de 0) el stock es BAJO. */
    public static final int UMBRAL_BAJO = 10;

    /** Con esta cantidad o menos (y más que el umbral bajo) el stock es MEDIO. */
    public static final int UMBRAL_MEDIO = 49;

    public enum Nivel {
        AGOTADO("Agotado", "badge-sinstock"),
        BAJO("Stock bajo", "badge-bajo"),
        MEDIO("Stock medio", "badge-medio"),
        SUFICIENTE("Suficiente", "badge-suficiente");

        private final String etiqueta;
        private final String claseCss;

        Nivel(String etiqueta, String claseCss) {
            this.etiqueta = etiqueta;
            this.claseCss = claseCss;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public String getClaseCss() {
            return claseCss;
        }
    }

    private EstadoStock() {
    }

    public static Nivel nivelDe(int cantidad) {
        if (cantidad <= 0) {
            return Nivel.AGOTADO;
        }
        if (cantidad <= UMBRAL_BAJO) {
            return Nivel.BAJO;
        }
        if (cantidad <= UMBRAL_MEDIO) {
            return Nivel.MEDIO;
        }
        return Nivel.SUFICIENTE;
    }
}
