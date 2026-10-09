package sgis.vista;

import sgis.controlador.CategoriaController;
import sgis.modelo.Categoria;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Configuración > Categorías: crear, renombrar y eliminar las categorías de productos.
 */
public class ConfigCategoriasVista implements Initializable {

    private final CategoriaController categoriaController = new CategoriaController();

    @FXML private Label lblMensaje;
    @FXML private TableView<Categoria> tablaCategorias;
    @FXML private TableColumn<Categoria, String> colNombre;
    @FXML private TableColumn<Categoria, String> colProductos;
    @FXML private TableColumn<Categoria, Categoria> colAcciones;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colNombre.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getNombre()));
        colProductos.setCellValueFactory(param ->
                new SimpleStringProperty(String.valueOf(param.getValue().getCantidadProductos())));

        colAcciones.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnRenombrar = new Button("✏ Renombrar");
            private final Button btnEliminar = new Button("🗑 Eliminar");
            private final HBox contenedor = new HBox(6, btnRenombrar, btnEliminar);

            @Override
            protected void updateItem(Categoria categoria, boolean vacio) {
                super.updateItem(categoria, vacio);
                if (vacio || categoria == null) {
                    setGraphic(null);
                    return;
                }
                btnRenombrar.setOnAction(e -> renombrar(categoria));
                btnEliminar.setOnAction(e -> confirmarEliminar(categoria));
                setGraphic(contenedor);
            }
        });

        cargarCategorias();
    }

    private void cargarCategorias() {
        try {
            List<Categoria> categorias = categoriaController.obtenerCategoriasConConteo();
            tablaCategorias.setItems(FXCollections.observableArrayList(categorias));
        } catch (RuntimeException e) {
            mostrarError("Error al cargar las categorías: " + e.getMessage());
        }
    }

    @FXML
    private void nuevaCategoria(ActionEvent event) {
        Dialogos.pedirTexto("Nueva categoría", "Nombre de la categoría:", "").ifPresent(nombre -> {
            String error = categoriaController.crear(nombre);
            resultado(error, "Categoría creada.");
        });
    }

    private void renombrar(Categoria categoria) {
        Dialogos.pedirTexto("Renombrar categoría", "Nuevo nombre:", categoria.getNombre()).ifPresent(nombre -> {
            String error = categoriaController.renombrar(categoria, nombre);
            resultado(error, "Categoría actualizada.");
        });
    }

    private void confirmarEliminar(Categoria categoria) {
        if (Dialogos.confirmar("¿Eliminar la categoría \"" + categoria.getNombre() + "\"?")) {
            resultado(categoriaController.eliminar(categoria), "Categoría eliminada.");
        }
    }

    private void resultado(String error, String mensajeExito) {
        if (error == null) {
            lblMensaje.setStyle("");
            lblMensaje.setText("✔ " + mensajeExito);
            cargarCategorias();
        } else {
            mostrarError(error);
        }
    }

    private void mostrarError(String texto) {
        lblMensaje.setStyle("-fx-text-fill: #C0392B;");
        lblMensaje.setText("✘ " + texto);
    }
}
