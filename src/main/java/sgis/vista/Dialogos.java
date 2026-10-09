package sgis.vista;

import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.Optional;

/**
 * Cuadros de diálogo con el mismo estilo minimalista del resto de la aplicación
 * (confirmaciones, avisos y entradas de texto).
 */
public final class Dialogos {

    private Dialogos() {
    }

    private static void aplicarEstilo(DialogPane panel) {
        panel.getStylesheets().add(Dialogos.class.getResource("/css/estilos.css").toExternalForm());
        panel.getStyleClass().add("dialogo-minimal");
    }

    /** Pregunta "¿Confirmas?" y devuelve true si el usuario aceptó. */
    public static boolean confirmar(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        alerta.setTitle("Confirmar");
        alerta.setHeaderText(null);
        alerta.setGraphic(null);
        aplicarEstilo(alerta.getDialogPane());
        ((Button) alerta.getDialogPane().lookupButton(ButtonType.OK)).setText("Confirmar");
        ((Button) alerta.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");

        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == ButtonType.OK;
    }

    /** Muestra un aviso informativo. */
    public static void informar(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alerta.setTitle("Aviso");
        alerta.setHeaderText(null);
        alerta.setGraphic(null);
        aplicarEstilo(alerta.getDialogPane());
        ((Button) alerta.getDialogPane().lookupButton(ButtonType.OK)).setText("Entendido");
        alerta.showAndWait();
    }

    /** Pide un texto corto. Devuelve vacío si el usuario cancela. */
    public static Optional<String> pedirTexto(String titulo, String etiqueta, String valorInicial) {
        TextInputDialog dialogo = new TextInputDialog(valorInicial == null ? "" : valorInicial);
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(null);
        dialogo.setGraphic(null);
        dialogo.setContentText(etiqueta);
        aplicarEstilo(dialogo.getDialogPane());
        ((Button) dialogo.getDialogPane().lookupButton(ButtonType.OK)).setText("Guardar");
        ((Button) dialogo.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");
        return dialogo.showAndWait();
    }

    /**
     * Muestra la clave temporal que acaba de generar el Administrador. El botón copia SOLO la clave
     * (sin el texto "Usuario: ..."), lista para pegarla en el campo "Clave temporal" de Editar.
     */
    public static void mostrarCredenciales(String nombre, String usuario, String contrasena) {
        ButtonType copiar = new ButtonType("Copiar clave", ButtonBar.ButtonData.LEFT);
        Alert alerta = new Alert(Alert.AlertType.INFORMATION,
                "Clave temporal de " + nombre + " (usuario " + usuario + "):\n\n"
                + contrasena + "\n\n"
                + "Para dejar una contraseña definitiva: pulsa Copiar clave, luego ✏ Editar a este usuario, "
                + "pega la clave en «Clave temporal» y escribe la contraseña nueva.",
                copiar, ButtonType.OK);
        alerta.setTitle("Clave temporal");
        alerta.setHeaderText(null);
        alerta.setGraphic(null);
        aplicarEstilo(alerta.getDialogPane());
        ((Button) alerta.getDialogPane().lookupButton(ButtonType.OK)).setText("Listo");

        Button botonCopiar = (Button) alerta.getDialogPane().lookupButton(copiar);
        botonCopiar.addEventFilter(ActionEvent.ACTION, evento -> {
            ClipboardContent contenido = new ClipboardContent();
            contenido.putString(contrasena); // solo el código de la clave
            Clipboard.getSystemClipboard().setContent(contenido);
            botonCopiar.setText("¡Copiada!");
            evento.consume(); // el cuadro sigue abierto
        });

        alerta.showAndWait();
    }
}
