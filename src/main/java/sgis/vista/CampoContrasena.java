package sgis.vista;

import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * Campo de contraseña con un ojito para mostrarla u ocultarla mientras se escribe.
 * Se usa en el login y en los formularios de usuarios. Solo muestra lo que la persona está
 * escribiendo: las contraseñas guardadas en la base de datos siempre están cifradas.
 */
public class CampoContrasena extends StackPane {

    private final PasswordField oculto = new PasswordField();
    private final TextField visible = new TextField();
    private final ImageView icono = new ImageView();
    private final Image imagenOjo;
    private final Image imagenOjoTachado;
    private boolean mostrando = false;

    public CampoContrasena() {
        oculto.getStyleClass().add("form-campo");
        visible.getStyleClass().add("form-campo");

        // Espacio a la derecha para que el texto no quede debajo del ojito.
        String relleno = "-fx-padding: 11 46 11 14;";
        oculto.setStyle(relleno);
        visible.setStyle(relleno);

        // Los dos campos comparten el mismo texto.
        visible.textProperty().bindBidirectional(oculto.textProperty());
        visible.setVisible(false);
        visible.setManaged(false);

        imagenOjo = new Image(CampoContrasena.class.getResource("/images/iconos/ojo.png").toExternalForm());
        imagenOjoTachado = new Image(CampoContrasena.class.getResource("/images/iconos/ojo_tachado.png").toExternalForm());
        icono.setImage(imagenOjo);
        icono.setFitWidth(20);
        icono.setFitHeight(20);
        icono.setPreserveRatio(true);

        Button boton = new Button();
        boton.setGraphic(icono);
        boton.setFocusTraversable(false);
        boton.setCursor(Cursor.HAND);
        boton.setStyle("-fx-background-color: transparent; -fx-padding: 6 10 6 10;");
        Tooltip.install(boton, new Tooltip("Mostrar u ocultar la contraseña"));
        boton.setOnAction(e -> alternar());

        StackPane.setAlignment(boton, Pos.CENTER_RIGHT);
        getChildren().addAll(oculto, visible, boton);
    }

    private void alternar() {
        mostrando = !mostrando;
        visible.setVisible(mostrando);
        visible.setManaged(mostrando);
        oculto.setVisible(!mostrando);
        oculto.setManaged(!mostrando);
        icono.setImage(mostrando ? imagenOjoTachado : imagenOjo);

        TextField activo = mostrando ? visible : oculto;
        activo.requestFocus();
        activo.positionCaret(activo.getLength());
    }

    public String getText() {
        return oculto.getText();
    }

    public void setText(String texto) {
        oculto.setText(texto);
    }

    public void clear() {
        oculto.clear();
    }

    public StringProperty textProperty() {
        return oculto.textProperty();
    }

    public String getPromptText() {
        return oculto.getPromptText();
    }

    public void setPromptText(String texto) {
        oculto.setPromptText(texto);
        visible.setPromptText(texto);
    }

    @Override
    public void requestFocus() {
        (mostrando ? visible : oculto).requestFocus();
    }
}
