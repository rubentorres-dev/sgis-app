package sgis.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Control de intentos fallidos de inicio de sesión (RNF-02).
 *
 * Después de 5 intentos fallidos consecutivos con el mismo usuario, el acceso
 * con ese usuario se bloquea durante 5 minutos. Un inicio de sesión correcto
 * reinicia el contador.
 *
 * Nota: el control vive en memoria, por lo que se reinicia si se cierra la aplicación.
 */
public class ControlIntentosLogin {

    public static final int MAX_INTENTOS = 5;
    public static final long MINUTOS_BLOQUEO = 5;

    private static final Map<String, Integer> fallos = new HashMap<>();
    private static final Map<String, Long> bloqueadoHasta = new HashMap<>();

    private ControlIntentosLogin() {
    }

    private static String clave(String usuario) {
        return usuario == null ? "" : usuario.trim().toLowerCase();
    }

    /** @return segundos que faltan para poder reintentar, o 0 si el usuario no está bloqueado. */
    public static synchronized long segundosDeBloqueoRestantes(String usuario) {
        Long hasta = bloqueadoHasta.get(clave(usuario));
        if (hasta == null) return 0;

        long restante = (hasta - System.currentTimeMillis()) / 1000;
        if (restante <= 0) {
            // Terminó el bloqueo: se limpia todo para dar 5 intentos nuevos.
            bloqueadoHasta.remove(clave(usuario));
            fallos.remove(clave(usuario));
            return 0;
        }
        return restante;
    }

    /**
     * Registra un intento fallido.
     * @return los intentos que le quedan antes del bloqueo (0 si este intento provocó el bloqueo).
     */
    public static synchronized int registrarFallo(String usuario) {
        String k = clave(usuario);
        int total = fallos.getOrDefault(k, 0) + 1;
        fallos.put(k, total);

        if (total >= MAX_INTENTOS) {
            bloqueadoHasta.put(k, System.currentTimeMillis() + MINUTOS_BLOQUEO * 60_000L);
            return 0;
        }
        return MAX_INTENTOS - total;
    }

    /** Borra todos los conteos y bloqueos temporales (se usa cuando un administrador desbloquea el sistema). */
    public static synchronized void reiniciarTodo() {
        fallos.clear();
        bloqueadoHasta.clear();
    }

    public static synchronized void registrarExito(String usuario) {
        fallos.remove(clave(usuario));
        bloqueadoHasta.remove(clave(usuario));
    }
}
