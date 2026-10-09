package sgis.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * Utilidades para archivos generados por la aplicación (informes, facturas).
 */
public final class Archivos {

    private Archivos() {
    }

    /**
     * Abre el archivo con el programa predeterminado del computador (desde ahí se puede imprimir).
     * Si no se puede abrir, no pasa nada: el archivo ya quedó guardado.
     */
    public static void abrir(File archivo) {
        Thread hilo = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(archivo);
                }
            } catch (IOException | RuntimeException e) {
                // sin acción
            }
        }, "abrir-archivo");
        hilo.setDaemon(true);
        hilo.start();
    }
}
