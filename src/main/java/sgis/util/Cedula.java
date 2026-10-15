package sgis.util;

/**
 * Reglas de la cédula de ciudadanía: solo números, de 6 a 10 dígitos. Se acepta escrita con puntos o espacios
 * (por ejemplo «1.037.456.789») y se guarda únicamente con los dígitos.
 */
public final class Cedula {

    public static final int MIN_DIGITOS = 6;
    public static final int MAX_DIGITOS = 10;

    private Cedula() {
    }

    /** Deja solo los dígitos. */
    public static String normalizar(String texto) {
        return texto == null ? "" : texto.replaceAll("[^0-9]", "");
    }

    /** @return null si la cédula es válida, o el mensaje de por qué no. */
    public static String validar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "Escribe la cédula de ciudadanía (solo números, de 6 a 10 dígitos).";
        }
        if (!texto.trim().matches("[0-9.,\\s]+")) {
            return "La cédula solo puede tener números (de 6 a 10 dígitos).";
        }
        int digitos = normalizar(texto).length();
        if (digitos < MIN_DIGITOS || digitos > MAX_DIGITOS) {
            return "La cédula debe tener entre 6 y 10 dígitos.";
        }
        return null;
    }

    /** «1037456789» → «1.037.456.789». */
    public static String formatear(String digitos) {
        if (digitos == null || digitos.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(digitos.trim()).reverse();
        StringBuilder con = new StringBuilder();
        for (int i = 0; i < sb.length(); i++) {
            if (i > 0 && i % 3 == 0) {
                con.append('.');
            }
            con.append(sb.charAt(i));
        }
        return con.reverse().toString();
    }
}
