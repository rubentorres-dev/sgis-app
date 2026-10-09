package sgis.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/**
 * Utilidad para cifrar contraseñas antes de guardarlas en la base de datos
 * (nunca se guardan en texto plano, según RNF-02 de seguridad).
 * Usa SHA-256, un algoritmo de hash unidireccional: no se puede "descifrar",
 * solo se puede comparar si una contraseña coincide con el hash guardado.
 */
public class PasswordUtil {

    public static String hash(String contrasenaPlana) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(contrasenaPlana.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash no disponible", e);
        }
    }

    public static boolean coincide(String contrasenaPlana, String hashGuardado) {
        return hash(contrasenaPlana).equals(hashGuardado);
    }

    // Sin caracteres que se confunden al leerlos (0/O, 1/l/I).
    private static final String CARACTERES_TEMPORALES =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    /** Genera una contraseña temporal aleatoria (para que el administrador se la entregue a una persona). */
    public static String generarTemporal(int largo) {
        SecureRandom azar = new SecureRandom();
        StringBuilder texto = new StringBuilder(largo);
        for (int i = 0; i < largo; i++) {
            texto.append(CARACTERES_TEMPORALES.charAt(azar.nextInt(CARACTERES_TEMPORALES.length())));
        }
        return texto.toString();
    }
}
