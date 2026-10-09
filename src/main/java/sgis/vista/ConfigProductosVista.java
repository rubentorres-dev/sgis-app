package sgis.vista;

import sgis.controlador.ProductoController;
import sgis.modelo.Categoria;
import sgis.modelo.Producto;
import sgis.util.FormatoMoneda;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Configuración > Productos: vista administrativa de todos los productos
 * (activos y desactivados) para crearlos, editarlos y reactivarlos.
 */
public class ConfigProductosVista implements Initializable {

    private final ProductoController productoController = new ProductoController();
    private final Map<Integer, String> nombresCategorias = new HashMap<>();
    private FilteredList<Producto> productosFiltrados;

    @FXML private TextField campoBusqueda;
    @FXML private Label lblMensaje;
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colPrecio;
    @FXML private TableColumn<Producto, String> colCantidad;
    @FXML private TableColumn<Producto, Producto> colEstado;
    @FXML private TableColumn<Producto, Producto> colAcciones;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            for (Categoria c : productoController.obtenerCategorias()) {
                nombresCategorias.put(c.getIdCategoria(), c.getNombre());
            }
        } catch (RuntimeException e) {
            mostrarError("Error al cargar las categorías: " + e.getMessage());
        }

        colNombre.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getNombre()));
        colCategoria.setCellValueFactory(param -> new SimpleStringProperty(
                nombresCategorias.getOrDefault(param.getValue().getIdCategoria(), "-")));
        colPrecio.setCellValueFactory(param -> new SimpleStringProperty(
                FormatoMoneda.formatear(param.getValue().getPrecio())));
        colCantidad.setCellValueFactory(param -> new SimpleStringProperty(
                String.valueOf(param.getValue().getCantidadDisponible())));

        colEstado.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Producto p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label();
                if ("activo".equals(p.getEstado())) {
                    badge.setText("Activo");
                    badge.getStyleClass().add("badge-suficiente");
                } else {
                    badge.setText("Desactivado");
                    badge.getStyleClass().add("badge-agotado");
                }
                setGraphic(badge);
            }
        });

        colAcciones.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("✏ Editar");

            @Override
            protected void updateItem(Producto p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                btnEditar.setOnAction(e -> abrirFormulario(p));
                setGraphic(btnEditar);
            }
        });

        cargarProductos();
        campoBusqueda.textProperty().addListener((obs, anterior, nuevo) -> filtrar(nuevo));
    }

    private void cargarProductos() {
        try {
            List<Producto> productos = productoController.obtenerProductos();
            productosFiltrados = new FilteredList<>(FXCollections.observableArrayList(productos), p -> true);
            tablaProductos.setItems(productosFiltrados);
            filtrar(campoBusqueda.getText());
        } catch (RuntimeException e) {
            mostrarError("Error al cargar los productos: " + e.getMessage());
        }
    }

    private void filtrar(String texto) {
        if (productosFiltrados == null) return;
        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        productosFiltrados.setPredicate(p -> buscado.isEmpty() || p.getNombre().toLowerCase().contains(buscado));
    }

    @FXML
    private void abrirFormularioNuevo(ActionEvent event) {
        abrirFormulario(null);
    }

    private void abrirFormulario(Producto productoAEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/productoForm.fxml"));
            Parent root = loader.load();
            ProductoFormVista controlador = loader.getController();
            controlador.configurar(productoAEditar, () -> {
                lblMensaje.setStyle("");
                lblMensaje.setText("✔ Producto guardado correctamente.");
                cargarProductos();
            });

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.setTitle(productoAEditar == null ? "Nuevo producto" : "Editar producto");
            dialogo.setScene(new Scene(root));
            dialogo.showAndWait();
        } catch (Exception e) {
            mostrarError("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void mostrarError(String texto) {
        lblMensaje.setStyle("-fx-text-fill: #C0392B;");
        lblMensaje.setText("✘ " + texto);
    }
}
