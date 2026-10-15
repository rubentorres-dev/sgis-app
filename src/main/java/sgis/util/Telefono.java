package sgis.util;

/** Reglas del número de teléfono: solo números, de 7 a 10 dígitos (por ejemplo, 3001234567). */
public final class Telefono {

    public static final int MIN_DIGITOS = 7;
    public static final int MAX_DIGITOS = 10;

    private Telefono() {
    }

    /** Deja solo los dígitos. */
    public static String normalizar(String texto) {
        return texto == null ? "" : texto.replaceAll("[^0-9]", "");
    }

    /** @return null si el teléfono es válido, o el mensaje de por qué no. */
    public static String validar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "Escribe el teléfono (solo números, de 7 a 10 dígitos).";
        }
        if (!texto.trim().matches("[0-9\\s-]+")) {
            return "El teléfono solo puede tener números (de 7 a 10 dígitos).";
        }
        int digitos = normalizar(texto).length();
        if (digitos < MIN_DIGITOS || digitos > MAX_DIGITOS) {
            return "El teléfono debe tener entre 7 y 10 dígitos.";
        }
        return null;
    }

    /** «3001234567» → «324 537 4597». */
    public static String formatear(String digitos) {
        if (digitos == null || digitos.isBlank()) {
            return "";
        }
        String d = digitos.trim();
        return d.length() == 10 ? d.substring(0, 3) + " " + d.substring(3, 6) + " " + d.substring(6) : d;
    }
}
