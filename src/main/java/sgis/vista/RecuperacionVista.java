package sgis.vista;

import sgis.controlador.UsuarioController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Ventana de recuperación de acceso, para TODOS los usuarios (también el administrador principal).
 *
 * Se abre desde «¿Olvidaste tu contraseña?» en el inicio de sesión. Pide solo el código de recuperación
 * de 6 dígitos y la contraseña nueva. El usuario es el que se escribió en el inicio de sesión y NO se puede
 * cambiar aquí. Mismo estilo minimalista del inicio de sesión.
 */
public final class RecuperacionVista {

    private RecuperacionVista() {
    }

    /**
     * @param login        usuario al que se le recupera el acceso (el escrito en el inicio de sesión; no se edita)
     * @param alRecuperar  se ejecuta cuando la contraseña se cambió con éxito
     */
    public static void abrir(String login, Runnable alRecuperar) {
        UsuarioController controlador = new UsuarioController();

        Label titulo = new Label("Recuperar acceso");
        titulo.getStyleClass().add("login-titulo");

        Label usuario = new Label(login == null ? "" : login.trim());
        usuario.getStyleClass().add("login-subtitulo");
        usuario.setStyle("-fx-font-weight: bold; -fx-text-fill: #14213D; -fx-font-size: 15px;");

        Label subtitulo = new Label("Escribe tu código de 6 dígitos.");
        subtitulo.getStyleClass().add("login-subtitulo");

        TextField campoCodigo = new TextField();
        campoCodigo.getStyleClass().add("login-campo");
        campoCodigo.setPromptText("Código de 6 dígitos");

        CampoContrasena campoNueva = new CampoContrasena();
        campoNueva.setPromptText("Contraseña nueva");
        campoNueva.setMaxWidth(Double.MAX_VALUE);

        CampoContrasena campoConfirmar = new CampoContrasena();
        campoConfirmar.setPromptText("Repite la contraseña");
        campoConfirmar.setMaxWidth(Double.MAX_VALUE);

        Label aviso = new Label("Si no recuerdas el código, ponte en contacto con el administrador.");
        aviso.getStyleClass().add("login-subtitulo");
        aviso.setWrapText(true);
        aviso.setMaxWidth(Double.MAX_VALUE);
        aviso.setAlignment(Pos.CENTER);
        aviso.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label mensaje = new Label("");
        mensaje.getStyleClass().add("login-mensaje");
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(Double.MAX_VALUE);

        Button recuperar = new Button("Recuperar");
        recuperar.getStyleClass().add("login-boton");
        recuperar.setMaxWidth(Double.MAX_VALUE);
        recuperar.setDefaultButton(true);

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("login-enlace");

        VBox raiz = new VBox(12, titulo, usuario, subtitulo, campoCodigo, campoNueva, campoConfirmar, aviso, mensaje, recuperar, cancelar);
        raiz.getStyleClass().add("form-fondo");
        raiz.setAlignment(Pos.CENTER);
        raiz.setPadding(new Insets(36, 40, 28, 40));
        raiz.setPrefWidth(380);
        raiz.getStylesheets().add(RecuperacionVista.class.getResource("/css/estilos.css").toExternalForm());

        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Recuperar acceso");
        ventana.setResizable(false);
        ventana.setScene(new Scene(raiz));

        cancelar.setOnAction(e -> ventana.close());
        recuperar.setOnAction(e -> {
            if (!campoNueva.getText().equals(campoConfirmar.getText())) {
                mensaje.setText("Las contraseñas no coinciden.");
                return;
            }
            String error = controlador.recuperarContrasena(
                    login, campoCodigo.getText(), campoNueva.getText());
            if (error == null) {
                ventana.close();
                if (alRecuperar != null) {
                    alRecuperar.run();
                }
            } else {
                mensaje.setText(error);
            }
        });

        Platform.runLater(campoCodigo::requestFocus);
        ventana.showAndWait();
    }
}
