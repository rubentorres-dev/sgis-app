package sgis.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Registro de la aplicación en un archivo de texto.
 *
 * Cuando la aplicación se abre desde el acceso directo del escritorio no hay ventana de consola, así
 * que cualquier error quedaría invisible. Por eso todo lo que normalmente saldría en la consola
 * (mensajes y errores) también se guarda en  .sgis/aplicacion.log  dentro de la carpeta personal
 * del usuario. Ese archivo es lo primero que hay que mirar si algo no funciona.
 */
public final class Registro {

    private static final Path ARCHIVO = Paths.get(System.getProperty("user.home"), ".sgis", "aplicacion.log");
    private static final long MAXIMO_BYTES = 2_000_000; // si pasa de ~2 MB se empieza de nuevo

    private Registro() {
    }

    public static Path getArchivo() {
        return ARCHIVO;
    }

    /** Se llama una sola vez, lo primero al arrancar. Nunca debe impedir que la aplicación abra. */
    public static void iniciar() {
        try {
            Files.createDirectories(ARCHIVO.getParent());
            if (Files.exists(ARCHIVO) && Files.size(ARCHIVO) > MAXIMO_BYTES) {
                Files.delete(ARCHIVO);
            }

            PrintStream archivo = new PrintStream(new FileOutputStream(ARCHIVO.toFile(), true), true, StandardCharsets.UTF_8);
            System.setOut(new PrintStream(new Duplicador(System.out, archivo), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new Duplicador(System.err, archivo), true, StandardCharsets.UTF_8));

            // Cualquier error que no se haya manejado queda escrito en el registro.
            Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
                System.err.println("Error no controlado en el hilo " + hilo.getName() + ":");
                error.printStackTrace();
            });

            System.out.println("=== Inicio " + LocalDateTime.now()
                    + " | Java " + System.getProperty("java.version")
                    + " | " + System.getProperty("os.name") + " ===");
        } catch (Exception e) {
            // Sin registro la aplicación funciona igual.
        }
    }

    /** Envía todo lo que se escribe a dos destinos a la vez (la consola de siempre y el archivo). */
    private static class Duplicador extends OutputStream {
        private final OutputStream primero;
        private final OutputStream segundo;

        Duplicador(OutputStream primero, OutputStream segundo) {
            this.primero = primero;
            this.segundo = segundo;
        }

        @Override
        public void write(int b) throws IOException {
            primero.write(b);
            segundo.write(b);
        }

        @Override
        public void write(byte[] datos, int inicio, int largo) throws IOException {
            primero.write(datos, inicio, largo);
            segundo.write(datos, inicio, largo);
        }

        @Override
        public void flush() throws IOException {
            primero.flush();
            segundo.flush();
        }
    }
}
