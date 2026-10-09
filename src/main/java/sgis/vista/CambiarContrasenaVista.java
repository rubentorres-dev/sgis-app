package sgis.vista;

import sgis.controlador.UsuarioController;
import sgis.util.ControlIntentosLogin;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Formulario para cambiar la contraseña propia (cambiarContrasena.fxml), que se abre desde
 * "¿Olvidaste tu contraseña?" en el login.
 *
 * - Quien no recuerda la contraseña actual recibe el mensaje de comunicarse con el administrador.
 * - Si el usuario ya agotó sus intentos de acceso, el formulario no se muestra: solo aparece el
 *   anuncio de comunicarse con el administrador.
 */
public class CambiarContrasenaVista {

    private final UsuarioController usuarioController = new UsuarioController();

    @FXML private VBox boxFormulario;
    @FXML private VBox boxBloqueado;
    @FXML private TextField campoUsuario;
    @FXML private CampoContrasena campoActual;
    @FXML private CampoContrasena campoNueva;
    @FXML private CampoContrasena campoConfirmar;
    @FXML private Label lblMensaje;
    @FXML private VBox boxAviso;

    private Runnable alCambiar;

    /** Se llama justo después de cargar el FXML. */
    public void configurar(String usuarioSugerido, Runnable alCambiar) {
        this.alCambiar = alCambiar;
        if (usuarioSugerido != null) {
            campoUsuario.setText(usuarioSugerido.trim());
            if (estaBloqueado(usuarioSugerido)) {
                mostrarBloqueado(); // ya agotó los intentos: ni siquiera se ofrece el formulario
            }
        }
    }

    @FXML
    private void cambiar() {
        if (estaBloqueado(campoUsuario.getText())) {
            mostrarBloqueado();
            return;
        }
        if (!campoNueva.getText().equals(campoConfirmar.getText())) {
            lblMensaje.setText("✘ Las contraseñas nuevas no coinciden.");
            return;
        }

        String error = usuarioController.cambiarContrasenaPropia(
                campoUsuario.getText(), campoActual.getText(), campoNueva.getText());

        if (error == null) {
            if (alCambiar != null) {
                alCambiar.run();
            }
            cerrarVentana();
        } else if (estaBloqueado(campoUsuario.getText())) {
            mostrarBloqueado(); // este intento fue el último: se reemplaza el formulario por el anuncio
        } else {
            lblMensaje.setText("✘ " + error);
            mostrarAviso(); // tras un fallo, recuerda que el administrador puede ayudar
        }
    }

    @FXML
    private void noRecuerdo() {
        lblMensaje.setText("");
        mostrarAviso();
    }

    @FXML
    private void cancelar() {
        cerrarVentana();
    }

    private boolean estaBloqueado(String usuario) {
        return usuario != null && !usuario.isBlank()
                && ControlIntentosLogin.segundosDeBloqueoRestantes(usuario) > 0;
    }

    private void mostrarAviso() {
        boxAviso.setVisible(true);
        boxAviso.setManaged(true);
        ajustarVentana();
    }

    /** Oculta todo el formulario y deja solo el anuncio "Comunícate con el administrador". */
    private void mostrarBloqueado() {
        boxFormulario.setVisible(false);
        boxFormulario.setManaged(false);
        boxBloqueado.setVisible(true);
        boxBloqueado.setManaged(true);
        ajustarVentana();
    }

    private void ajustarVentana() {
        // Al llamarse desde configurar() la ventana todavía no existe; en ese caso no hay nada que ajustar.
        if (boxAviso.getScene() != null && boxAviso.getScene().getWindow() != null) {
            boxAviso.getScene().getWindow().sizeToScene();
        }
    }

    private void cerrarVentana() {
        Stage stage = (Stage) boxFormulario.getScene().getWindow();
        stage.close();
    }
}
