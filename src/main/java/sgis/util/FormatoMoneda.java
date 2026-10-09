package sgis.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Utilidad para mostrar valores monetarios en formato de pesos colombianos:
 * sin decimales y con punto como separador de miles (ej. $1.000.000).
 *
 * Los símbolos se fijan a mano (no dependen del idioma del computador ni de los
 * datos de idioma incluidos en el Java empaquetado), así el resultado es siempre igual.
 */
public class FormatoMoneda {

    private static final DecimalFormatSymbols SIMBOLOS = new DecimalFormatSymbols();
    static {
        SIMBOLOS.setGroupingSeparator('.');
        SIMBOLOS.setDecimalSeparator(',');
    }

    public static String formatear(double valor) {
        // DecimalFormat no es seguro entre hilos, por eso se crea uno por llamada.
        DecimalFormat formato = new DecimalFormat("#,##0", SIMBOLOS);
        return "$" + formato.format(valor);
    }
}
