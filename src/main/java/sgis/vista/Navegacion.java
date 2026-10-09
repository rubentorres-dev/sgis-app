package sgis.vista;

import sgis.util.Permisos;
import sgis.util.Sesion;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utilidad para cambiar de pantalla (FXML) dentro de la misma ventana.
 * También hace cumplir los permisos por rol y recuerda cuál es la pantalla actual
 * (para que el menú lateral marque la opción correcta).
 */
public class Navegacion {

    private static Permisos.Pantalla pantallaActual = Permisos.Pantalla.PANEL;

    public static Permisos.Pantalla getPantallaActual() {
        return pantallaActual;
    }

    /** Carga un FXML y registra a qué pantalla corresponde. */
    public static Parent cargar(String rutaFxml) throws IOException {
        Permisos.Pantalla pantalla = pantallaDe(rutaFxml);
        if (pantalla != null) {
            pantallaActual = pantalla;
        }
        return FXMLLoader.load(Navegacion.class.getResource(rutaFxml));
    }

    public static void cambiarEscena(ActionEvent evento, String rutaFxml) {
        Permisos.Pantalla destino = pantallaDe(rutaFxml);
        if (destino != null && !Permisos.puedeAcceder(Sesion.getUsuarioActual(), destino)) {
            Dialogos.informar("Tu rol no tiene permiso para entrar a esta sección.");
            return;
        }

        try {
            Parent root = cargar(rutaFxml);
            Stage stage = (Stage) ((Node) evento.getSource()).getScene().getWindow();
            Scene escenaActual = stage.getScene();
            stage.setScene(new Scene(root, escenaActual.getWidth(), escenaActual.getHeight()));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar la pantalla " + rutaFxml + ": " + e.getMessage(), e);
        }
    }

    private static Permisos.Pantalla pantallaDe(String ruta) {
        switch (ruta) {
            case "/fxml/panelPrincipal.fxml":
                return Permisos.Pantalla.PANEL;
            case "/fxml/productos.fxml":
                return Permisos.Pantalla.PRODUCTOS;
            case "/fxml/ventas.fxml":
                return Permisos.Pantalla.VENTAS;
            case "/fxml/reportes.fxml":
                return Permisos.Pantalla.REPORTES;
            case "/fxml/usuarios.fxml":
                return Permisos.Pantalla.USUARIOS;
            case "/fxml/configuracion.fxml":
                return Permisos.Pantalla.CONFIGURACION;
            default:
                return null; // login y formularios: no son secciones del menú
        }
    }
}
