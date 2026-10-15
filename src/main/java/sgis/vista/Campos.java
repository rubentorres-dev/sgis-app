package sgis.vista;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

/** Restricciones de escritura para los campos de números (cédula y teléfono). */
public final class Campos {

    private Campos() {
    }

    /** Solo números (y puntos, si se permiten), con a lo sumo maxDigitos dígitos: el siguiente número ya no se escribe. */
    public static UnaryOperator<TextFormatter.Change> filtro(int maxDigitos, boolean permitirPuntos) {
        String permitido = permitirPuntos ? "[0-9.]*" : "[0-9]*";
        return cambio -> {
            String nuevo = cambio.getControlNewText();
            return nuevo.matches(permitido) && nuevo.replaceAll("[^0-9]", "").length() <= maxDigitos ? cambio : null;
        };
    }

    public static void soloNumeros(TextField campo, int maxDigitos, boolean permitirPuntos) {
        campo.setTextFormatter(new TextFormatter<String>(filtro(maxDigitos, permitirPuntos)));
    }
}
