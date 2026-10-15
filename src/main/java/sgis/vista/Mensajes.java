package sgis.vista;

import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Mensajes de estado con un icono DIBUJADO (círculo verde con un chulo, o rojo con una X). No dependen de que la
 * fuente del computador tenga los símbolos ✔ y ✘, que en algunos equipos se ven como una X.
 */
public final class Mensajes {

    private static final String VERDE = "#1E9E5B";
    private static final String ROJO = "#C0392B";

    private Mensajes() {
    }

    /** Un mensaje normal (sin icono ni color): deja la etiqueta como estaba antes de un éxito o un error. */
    public static void normal(Label etiqueta, String texto) {
        etiqueta.setGraphic(null);
        etiqueta.setTooltip(null);
        etiqueta.setStyle("");
        etiqueta.setText(texto);
    }

    /** Éxito: chulo verde y texto verde. El detalle (por ejemplo, la ruta del archivo) va en el cuadro al pasar el mouse. */
    public static void exito(Label etiqueta, String texto, String detalle) {
        configurar(etiqueta, texto, icono(VERDE, "M0 5 L3.5 8.5 L10 1.5"), VERDE);
        etiqueta.setTooltip(detalle == null || detalle.isBlank() ? null : new Tooltip(detalle));
    }

    /** Error real: X roja y texto rojo. */
    public static void error(Label etiqueta, String texto) {
        configurar(etiqueta, texto, icono(ROJO, "M0 0 L8 8 M8 0 L0 8"), ROJO);
        etiqueta.setTooltip(null);
    }

    private static void configurar(Label etiqueta, String texto, Node icono, String color) {
        etiqueta.setText(texto);
        etiqueta.setGraphic(icono);
        etiqueta.setContentDisplay(ContentDisplay.LEFT);
        etiqueta.setGraphicTextGap(8);
        etiqueta.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private static Node icono(String colorFondo, String trazo) {
        Circle circulo = new Circle(9, Color.web(colorFondo));
        SVGPath marca = new SVGPath();
        marca.setContent(trazo);
        marca.setFill(Color.TRANSPARENT);
        marca.setStroke(Color.WHITE);
        marca.setStrokeWidth(2.2);
        marca.setStrokeLineCap(StrokeLineCap.ROUND);
        marca.setStrokeLineJoin(StrokeLineJoin.ROUND);
        StackPane caja = new StackPane(circulo, marca);
        caja.setMinSize(18, 18);
        caja.setPrefSize(18, 18);
        caja.setMaxSize(18, 18);
        return caja;
    }
}
