package sgis.vista;

import sgis.controlador.UsuarioController;
import sgis.modelo.Usuario;
import sgis.servicio.AuditoriaService;
import sgis.util.ControlIntentosLogin;
import sgis.util.Sesion;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla de inicio de sesión (login.fxml).
 */
public class LoginVista implements Initializable {

    private final UsuarioController usuarioController = new UsuarioController();

    @FXML private VBox tarjeta;
    @FXML private TextField campoUsuario;
    @FXML private CampoContrasena campoContrasena;
    @FXML private Label lblMensaje;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Aparición suave de la tarjeta
        tarjeta.setOpacity(0);
        FadeTransition aparicion = new FadeTransition(Duration.millis(450), tarjeta);
        aparicion.setFromValue(0);
        aparicion.setToValue(1);
        aparicion.play();

        Platform.runLater(() -> campoUsuario.requestFocus());

        // Si MySQL no está encendido, se avisa desde el principio (la revisión corre aparte para no congelar la ventana).
        Thread revision = new Thread(() -> {
            String problema = usuarioController.comprobarConexion();
            if (problema != null) {
                Platform.runLater(() -> mostrarError("⚠ " + problema));
            }
        }, "revision-bd");
        revision.setDaemon(true);
        revision.start();
    }

    @FXML
    private void ingresar(ActionEvent event) {
        String login = campoUsuario.getText();
        String contrasena = campoContrasena.getText();

        if (login == null || login.isBlank() || contrasena == null || contrasena.isEmpty()) {
            mostrarError("Escribe tu usuario y tu contraseña.");
            return;
        }

        // RNF-02: si el usuario está bloqueado por intentos fallidos, ni siquiera se consulta.
        long segundos = ControlIntentosLogin.segundosDeBloqueoRestantes(login);
        if (segundos > 0) {
            mostrarError("Acceso bloqueado por demasiados intentos fallidos. Intenta de nuevo en "
                    + textoTiempo(segundos) + " o comunícate con el administrador.");
            return;
        }

        try {
            Usuario usuario = usuarioController.iniciarSesion(login, contrasena);
            if (usuario != null) {
                ControlIntentosLogin.registrarExito(login);
                Sesion.iniciar(usuario);
                AuditoriaService.registrar(usuario, "Inició sesión", null);
                Navegacion.cambiarEscena(event, "/fxml/panelPrincipal.fxml");
            } else {
                int restantes = ControlIntentosLogin.registrarFallo(login);
                if (restantes == 0) {
                    AuditoriaService.registrar(null, "Acceso bloqueado por intentos fallidos", "Usuario: " + login.trim());
                } else {
                    AuditoriaService.registrar(null, "Intento de acceso fallido",
                            "Usuario: " + login.trim() + " (quedan " + restantes + " intentos)");
                }
                if (restantes == 0) {
                    mostrarError("Usuario o contraseña incorrectos. Se bloqueó el acceso por "
                            + ControlIntentosLogin.MINUTOS_BLOQUEO + " minutos. "
                            + "Si olvidaste tu contraseña, comunícate con el administrador.");
                } else if (restantes <= 2) {
                    mostrarError("Usuario o contraseña incorrectos. Intentos restantes: " + restantes + ". "
                            + "Si olvidaste tu contraseña, comunícate con el administrador.");
                } else {
                    mostrarError("Usuario o contraseña incorrectos. Intentos restantes: " + restantes + ".");
                }
                campoContrasena.clear();
            }
        } catch (RuntimeException ex) {
            mostrarError("No se pudo iniciar sesión: " + ex.getMessage());
        }
    }

    /**
     * Abre el formulario para crear un usuario nuevo. Como cualquiera podría llegar a esta
     * pantalla, el formulario exige la autorización de un Administrador
     * de la empresa antes de guardar.
     */
    @FXML
    private void abrirNuevoUsuario(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/usuarioSistemaForm.fxml"));
            Parent root = loader.load();
            UsuarioSistemaFormVista controlador = loader.getController();
            controlador.configurar(null, true, () ->
                    mostrarExito("✔ Usuario creado. Ya puedes iniciar sesión con él."));

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.setTitle("Crear usuario");
            dialogo.setScene(new Scene(root));
            dialogo.showAndWait();
        } catch (Exception e) {
            mostrarError("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    /** Abre el formulario para cambiar la contraseña (o avisar que hay que hablar con el administrador). */
    @FXML
    private void abrirCambiarContrasena(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cambiarContrasena.fxml"));
            Parent root = loader.load();
            CambiarContrasenaVista controlador = loader.getController();
            controlador.configurar(campoUsuario.getText(), () -> {
                campoContrasena.clear();
                mostrarExito("✔ Contraseña actualizada. Inicia sesión con la nueva.");
            });

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.setTitle("Cambiar contraseña");
            dialogo.setScene(new Scene(root));
            dialogo.showAndWait();
        } catch (Exception e) {
            mostrarError("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void mostrarError(String texto) {
        lblMensaje.setStyle("");
        lblMensaje.setText(texto);
    }

    private void mostrarExito(String texto) {
        lblMensaje.setStyle("-fx-text-fill: #1E9E5B;");
        lblMensaje.setText(texto);
    }

    private String textoTiempo(long segundos) {
        long minutos = segundos / 60;
        long resto = segundos % 60;
        return minutos > 0 ? minutos + " min " + resto + " s" : resto + " s";
    }
}
