package sgis;

import sgis.controlador.RolController;
import sgis.servicio.RespaldoService;
import sgis.util.Permisos;
import sgis.util.Registro;
import sgis.vista.Navegacion;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Clase principal de la aplicación JavaFX "Administrador de ventas".
 * Abre la pantalla de inicio de sesión (login.fxml); desde ahí el usuario
 * entra al resto de pantallas según su rol.
 *
 * IMPORTANTE: esta clase NO se ejecuta directamente (por eso no tiene
 * el punto de entrada main real) — el punto de entrada es Main.java,
 * que es una clase "lanzadora" separada. Esto evita el error
 * "JavaFX runtime components are missing" al ejecutar con java -classpath.
 */
public class App extends Application {

    private static final String NOMBRE_APP = "Administrador de ventas";
    /** Versión que se ve en la barra de título (v1.8 = teléfono, cédula de máximo 10 dígitos y aviso de descarga con chulo verde). */
    private static final String VERSION = "1.8";

    @Override
    public void start(Stage stage) {
        stage.setTitle(NOMBRE_APP + " · v" + VERSION);

        // Ícono de la ventana (carrito de compras)
        try (InputStream icono = getClass().getResourceAsStream("/images/icono_carrito.png")) {
            if (icono != null) {
                stage.getIcons().add(new Image(icono));
            }
        } catch (IOException e) {
            // Sin ícono la aplicación funciona igual.
        }

        // Los permisos de cada rol se leen de la base de datos (tabla ROL)
        Permisos.configurar(() -> new RolController().obtenerRoles());

        // Respaldo automático de la base de datos cada hora (RNF-05)
        RespaldoService.iniciarProgramado();

        try {
            Parent login = Navegacion.cargar("/fxml/login.fxml");
            stage.setScene(new Scene(login, 1200, 720));
            stage.show();
        } catch (Exception e) {
            // Sin consola (acceso directo del escritorio) un error al arrancar pasaría desapercibido:
            // se deja en el registro y se muestra un aviso con la ubicación del archivo.
            e.printStackTrace();
            Alert aviso = new Alert(Alert.AlertType.ERROR,
                    "No se pudo abrir la aplicación:\n" + e.getMessage()
                    + "\n\nLos detalles quedaron guardados en:\n" + Registro.getArchivo());
            aviso.setTitle(NOMBRE_APP);
            aviso.setHeaderText(null);
            aviso.showAndWait();
            Platform.exit();
        }
    }
}
