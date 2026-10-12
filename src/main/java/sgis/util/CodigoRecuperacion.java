package sgis.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Código de recuperación del perfil del propietario del sistema (el usuario protegido).
 *
 * - Son 6 dígitos (solo números), fáciles de escribir y de recordar.
 * - Se muestra UNA sola vez al propietario, cuando inicia sesión y todavía no tiene uno.
 * - En la base de datos solo se guarda su hash (SHA-256), nunca el código.
 * - Vale una sola vez: al usarlo se descarta y se genera otro en el siguiente inicio de sesión.
 * - Como son solo un millón de combinaciones, la seguridad la da el bloqueo: 5 intentos fallidos
 *   bloquean la recuperación 5 minutos, y esos intentos se cuentan en la base de datos (no se
 *   reinician al cerrar la aplicación). Ver UsuarioController.recuperarContrasenaPropietario.
 *
 * Los códigos de 8 y de 16 caracteres que generaron versiones anteriores siguen funcionando.
 */
public final class CodigoRecuperacion {

    public static final int DIGITOS = 6;

    private CodigoRecuperacion() {
    }

    /** Genera un código de 6 dígitos con un generador de números aleatorios criptográficamente seguro. */
    public static String generar() {
        SecureRandom azar = new SecureRandom();
        return String.format("%0" + DIGITOS + "d", azar.nextInt(1_000_000));
    }

    /** Quita espacios y guiones y pasa a mayúsculas: se puede escribir como 123456 o 123 456. */
    public static String normalizar(String codigo) {
        if (codigo == null) {
            return "";
        }
        return codigo.replaceAll("[\\s-]", "").toUpperCase();
    }

    /** true si, ya normalizado, son 6 dígitos (o un código anterior de 8 o 16 caracteres). */
    public static boolean tieneFormatoValido(String codigo) {
        String n = normalizar(codigo);
        if (n.matches("\\d{" + DIGITOS + "}")) {
            return true;
        }
        return n.length() == 8 || n.length() == 16; // códigos de versiones anteriores
    }

    /** Hash que se guarda en la base de datos. */
    public static String hash(String codigo) {
        return PasswordUtil.hash(normalizar(codigo));
    }

    /** Compara el código escrito con el hash guardado, en tiempo constante. */
    public static boolean coincide(String codigoEscrito, String hashGuardado) {
        if (hashGuardado == null || hashGuardado.isEmpty()) {
            return false;
        }
        byte[] a = hash(codigoEscrito).getBytes(StandardCharsets.UTF_8);
        byte[] b = hashGuardado.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
