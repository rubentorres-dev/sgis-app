package sgis.vista;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Ventana que muestra UNA sola vez el código de recuperación del propietario.
 * Mismo estilo minimalista del inicio de sesión: un título, el código y un botón.
 */
public final class CodigoRecuperacionVista {

    private CodigoRecuperacionVista() {
    }

    /** Lo ve la propia persona: su código, una sola vez. */
    public static void mostrar(String codigo) {
        mostrar(codigo, "Tu código", "Para recuperar tu acceso. Guárdalo ahora: no se volverá a mostrar.");
    }

    /** Variante con título y texto propios (por ejemplo, cuando un administrador crea un usuario para otra persona). */
    public static void mostrar(String codigo, String textoTitulo, String textoSubtitulo) {
        Label titulo = new Label(textoTitulo);
        titulo.getStyleClass().add("login-titulo");

        Label subtitulo = new Label(textoSubtitulo);
        subtitulo.getStyleClass().add("login-subtitulo");
        subtitulo.setWrapText(true);
        subtitulo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Se muestra como 123 456 para leerlo mejor; al copiar se copian solo los 6 dígitos.
        String visible = codigo.length() == 6 ? codigo.substring(0, 3) + " " + codigo.substring(3) : codigo;
        Label valor = new Label(visible);
        valor.setMaxWidth(Double.MAX_VALUE);
        valor.setAlignment(Pos.CENTER);
        valor.setStyle("-fx-background-color: #F2F3F7; -fx-background-radius: 14; -fx-padding: 18 24 18 24; "
                + "-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 36px; "
                + "-fx-font-weight: bold; -fx-text-fill: #14213D;");

        Button entendido = new Button("Entendido");
        entendido.getStyleClass().add("login-boton");
        entendido.setMaxWidth(Double.MAX_VALUE);
        entendido.setDefaultButton(true);

        Button copiar = new Button("Copiar código");
        copiar.getStyleClass().add("login-enlace");
        copiar.setOnAction(e -> {
            ClipboardContent contenido = new ClipboardContent();
            contenido.putString(codigo);
            Clipboard.getSystemClipboard().setContent(contenido);
            copiar.setText("¡Copiado!");
        });

        VBox raiz = new VBox(14, titulo, subtitulo, valor, entendido, copiar);
        raiz.getStyleClass().add("form-fondo");
        raiz.setAlignment(Pos.CENTER);
        raiz.setPadding(new javafx.geometry.Insets(36, 40, 28, 40));
        raiz.setPrefWidth(380);
        raiz.getStylesheets().add(CodigoRecuperacionVista.class.getResource("/css/estilos.css").toExternalForm());

        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Código de recuperación");
        ventana.setResizable(false);
        ventana.setScene(new Scene(raiz));
        entendido.setOnAction(e -> ventana.close());
        ventana.showAndWait();
    }
}
