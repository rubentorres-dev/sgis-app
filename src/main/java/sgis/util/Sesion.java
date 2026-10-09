package sgis.util;

import sgis.modelo.Usuario;

/**
 * Guarda el usuario que inició sesión, para que cualquier pantalla FXML
 * (que es instanciada por FXMLLoader, no por nosotros directamente)
 * pueda saber quién es el usuario actual sin tener que pasarlo a mano
 * por cada constructor.
 */
public class Sesion {

    private static Usuario usuarioActual;

    private Sesion() {
    }

    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public static void cerrar() {
        usuarioActual = null;
    }
}
