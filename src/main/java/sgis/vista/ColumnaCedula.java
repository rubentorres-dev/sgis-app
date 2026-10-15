package sgis.vista;

import sgis.controlador.ClienteController;
import sgis.modelo.Cliente;
import sgis.util.Sesion;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Columna «Cédula» de las tablas de clientes: aparece oculta con puntos y el ojito la muestra u oculta.
 * Cada vez que se muestra queda registrado en la auditoría.
 */
public final class ColumnaCedula {

    private ColumnaCedula() {
    }

    /**
     * @param reveladas  cédulas que están a la vista ahora (id de cliente → cédula con puntos); quien llama la vacía al recargar
     * @param alError    qué hacer si no se puede mostrar (por ejemplo, el rol no tiene acceso)
     */
    public static void instalar(TableColumn<Cliente, Cliente> columna, TableView<Cliente> tabla, ClienteController controlador,
                                Map<Integer, String> reveladas, Consumer<String> alError) {
        Image imagenOjo = new Image(ColumnaCedula.class.getResource("/images/iconos/ojo.png").toExternalForm());
        Image imagenOjoTachado = new Image(ColumnaCedula.class.getResource("/images/iconos/ojo_tachado.png").toExternalForm());

        columna.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        columna.setCellFactory(col -> new TableCell<>() {
            private final Label texto = new Label();
            private final ImageView icono = new ImageView();
            private final Button ojo = new Button();
            private final HBox caja = new HBox(6, texto, ojo);

            {
                icono.setFitWidth(18);
                icono.setFitHeight(18);
                icono.setPreserveRatio(true);
                ojo.setGraphic(icono);
                ojo.setStyle("-fx-background-color: transparent; -fx-padding: 2 4 2 4; -fx-cursor: hand;");
                caja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Cliente c, boolean vacio) {
                super.updateItem(c, vacio);
                if (vacio || c == null) {
                    setGraphic(null);
                    return;
                }
                boolean tiene = c.getCedula() != null && !c.getCedula().isBlank();
                boolean visible = reveladas.containsKey(c.getIdCliente());
                texto.setText(!tiene ? "Sin cédula" : (visible ? reveladas.get(c.getIdCliente()) : "••••••••••"));
                texto.setStyle(visible ? "-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-weight: bold;" : "");
                icono.setImage(visible ? imagenOjoTachado : imagenOjo);
                ojo.setVisible(tiene);
                ojo.setManaged(tiene);
                ojo.setOnAction(e -> {
                    if (reveladas.containsKey(c.getIdCliente())) {
                        reveladas.remove(c.getIdCliente());
                    } else {
                        try {
                            reveladas.put(c.getIdCliente(), controlador.verCedula(Sesion.getUsuarioActual(), c));
                        } catch (SecurityException ex) {
                            alError.accept(ex.getMessage());
                            return;
                        }
                    }
                    tabla.refresh();
                });
                setGraphic(caja);
            }
        });
    }
}
