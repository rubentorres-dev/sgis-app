package sgis.vista;

import sgis.controlador.UsuarioController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.TextAlignment;

/**
 * Pantalla que reemplaza al inicio de sesión cuando el sistema está bloqueado por un ataque de fuerza bruta.
 * Muestra un candado en el centro y el aviso; solo un Administrador, con su usuario y contraseña, puede
 * desbloquear el sistema. Mismo estilo minimalista del inicio de sesión.
 */
public final class SistemaBloqueadoVista {

    private SistemaBloqueadoVista() {
    }

    /** @param alDesbloquear se ejecuta cuando un administrador desbloqueó el sistema (vuelve el inicio de sesión) */
    public static Parent crear(Runnable alDesbloquear) {
        UsuarioController controlador = new UsuarioController();

        // Candado dibujado con formas (no depende de ninguna imagen ni fuente)
        Color rojo = Color.web("#C0392B");
        SVGPath arco = new SVGPath();
        arco.setContent("M18 32 V22 A14 14 0 0 1 46 22 V32");
        arco.setFill(Color.TRANSPARENT);
        arco.setStroke(rojo);
        arco.setStrokeWidth(7);
        arco.setStrokeLineCap(StrokeLineCap.ROUND);
        Rectangle cuerpo = new Rectangle(8, 30, 48, 40);
        cuerpo.setArcWidth(16);
        cuerpo.setArcHeight(16);
        cuerpo.setFill(rojo);
        Circle ojo = new Circle(32, 46, 5.5, Color.WHITE);
        Rectangle ranura = new Rectangle(30, 48, 4, 12);
        ranura.setFill(Color.WHITE);
        Pane candado = new Pane(arco, cuerpo, ojo, ranura);
        candado.setPrefSize(64, 74);
        candado.setMinSize(64, 74);
        candado.setMaxSize(64, 74);

        Label titulo = new Label("Sistema bloqueado");
        titulo.getStyleClass().add("login-titulo");

        Label aviso = new Label("El sistema ha sido bloqueado porque se detectó un ataque de fuerza bruta "
                + "contra el sistema. Para acceder, comunícate con los administradores.");
        aviso.getStyleClass().add("login-subtitulo");
        aviso.setWrapText(true);
        aviso.setMaxWidth(Double.MAX_VALUE);
        aviso.setAlignment(Pos.CENTER);
        aviso.setTextAlignment(TextAlignment.CENTER);

        Button abrir = new Button("Desbloquear como administrador");
        abrir.getStyleClass().add("login-enlace");

        TextField usuario = new TextField();
        usuario.getStyleClass().add("login-campo");
        usuario.setPromptText("Usuario administrador");
        CampoContrasena contrasena = new CampoContrasena();
        contrasena.setPromptText("Contraseña");
        contrasena.setMaxWidth(Double.MAX_VALUE);

        Label mensaje = new Label("");
        mensaje.getStyleClass().add("login-mensaje");
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(Double.MAX_VALUE);

        Button desbloquear = new Button("Desbloquear");
        desbloquear.getStyleClass().add("login-boton");
        desbloquear.setMaxWidth(Double.MAX_VALUE);

        VBox formulario = new VBox(10, usuario, contrasena, mensaje, desbloquear);
        formulario.setVisible(false);
        formulario.setManaged(false);

        abrir.setOnAction(e -> {
            formulario.setVisible(true);
            formulario.setManaged(true);
            abrir.setVisible(false);
            abrir.setManaged(false);
            desbloquear.setDefaultButton(true);
            usuario.requestFocus();
        });
        desbloquear.setOnAction(e -> {
            String error = controlador.desbloquearSistema(usuario.getText(), contrasena.getText());
            if (error == null) {
                if (alDesbloquear != null) {
                    alDesbloquear.run();
                }
            } else {
                mensaje.setText(error);
                contrasena.clear();
            }
        });

        VBox tarjeta = new VBox(14, candado, titulo, aviso, abrir, formulario);
        tarjeta.getStyleClass().add("login-tarjeta");
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(36, 40, 30, 40));
        tarjeta.setPrefWidth(400);
        tarjeta.setMinWidth(400);
        tarjeta.setMaxWidth(400);

        StackPane raiz = new StackPane(tarjeta);
        raiz.getStyleClass().add("login-fondo");
        raiz.getStylesheets().add(SistemaBloqueadoVista.class.getResource("/css/estilos.css").toExternalForm());
        return raiz;
    }
}
