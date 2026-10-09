package sgis.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Copia cifrada de las contraseñas, para que un Administrador pueda verlas desde Configuración.
 *
 * - El inicio de sesión NO usa esta copia: sigue validándose contra el hash de siempre.
 * - Se cifra con AES-256-GCM. La llave se genera sola la primera vez y se guarda en un archivo
 *   de este computador (carpeta personal, .sgis/clave.key), NO en la base de datos. Por eso la base
 *   de datos y los respaldos .sql solo contienen texto cifrado que no se puede leer sin ese archivo.
 * - Si el archivo de la llave se pierde (o la base de datos se lleva a otro computador sin él),
 *   las contraseñas guardadas dejan de poder verse; el Administrador puede generar una nueva con "Clave".
 */
public final class CifradoContrasena {

    private static final Path ARCHIVO_LLAVE = Paths.get(System.getProperty("user.home"), ".sgis", "clave.key");
    private static final int LARGO_IV = 12;
    private static final int BITS_ETIQUETA = 128;

    private static SecretKeySpec llave;

    private CifradoContrasena() {
    }

    /** @return el texto cifrado (listo para guardar), o null si no se pudo cifrar. */
    public static String cifrar(String texto) {
        try {
            byte[] iv = new byte[LARGO_IV];
            new SecureRandom().nextBytes(iv);

            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, obtenerLlave(), new GCMParameterSpec(BITS_ETIQUETA, iv));
            byte[] cifrado = cifrador.doFinal(texto.getBytes(StandardCharsets.UTF_8));

            byte[] todo = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, todo, 0, iv.length);
            System.arraycopy(cifrado, 0, todo, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(todo);
        } catch (Exception e) {
            System.out.println("No se pudo cifrar la copia de la contraseña: " + e.getMessage());
            return null;
        }
    }

    /** @return la contraseña, o null si no hay copia o no se puede descifrar (llave distinta o dato dañado). */
    public static String descifrar(String guardado) {
        if (guardado == null || guardado.isBlank()) {
            return null;
        }
        try {
            byte[] todo = Base64.getDecoder().decode(guardado);

            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.DECRYPT_MODE, obtenerLlave(), new GCMParameterSpec(BITS_ETIQUETA, todo, 0, LARGO_IV));
            byte[] claro = cifrador.doFinal(todo, LARGO_IV, todo.length - LARGO_IV);
            return new String(claro, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static synchronized SecretKeySpec obtenerLlave() throws IOException {
        if (llave == null) {
            byte[] bytes;
            if (Files.exists(ARCHIVO_LLAVE)) {
                bytes = Base64.getDecoder().decode(Files.readString(ARCHIVO_LLAVE).trim());
            } else {
                bytes = new byte[32]; // 256 bits
                new SecureRandom().nextBytes(bytes);
                Files.createDirectories(ARCHIVO_LLAVE.getParent());
                Files.writeString(ARCHIVO_LLAVE, Base64.getEncoder().encodeToString(bytes));

                // En sistemas que lo permiten, solo el dueño del archivo puede leerlo.
                File archivo = ARCHIVO_LLAVE.toFile();
                archivo.setReadable(false, false);
                archivo.setReadable(true, true);
            }
            llave = new SecretKeySpec(bytes, "AES");
        }
        return llave;
    }
}
