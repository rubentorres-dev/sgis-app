package sgis.vista;

import sgis.controlador.ProductoController;
import sgis.modelo.Categoria;
import sgis.modelo.Producto;
import sgis.util.FormatoMoneda;
import sgis.util.Permisos;
import sgis.servicio.AuditoriaService;
import sgis.util.Sesion;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador de la pantalla Productos (productos.fxml): listar, buscar,
 * crear, editar y desactivar productos.
 */
public class ProductosVista implements Initializable {

    private final ProductoController productoController = new ProductoController();
    private final Map<Integer, String> nombresCategorias = new HashMap<>();


    @FXML private TextField campoBusqueda;
    @FXML private Label lblMensaje;
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colPrecio;
    @FXML private TableColumn<Producto, Integer> colCantidad;
    @FXML private TableColumn<Producto, Producto> colEstado;
    @FXML private TableColumn<Producto, Producto> colAcciones;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Solo el Administrador puede editar y borrar; el Administrador de inventario solo consulta y crea.
        colAcciones.setVisible(Permisos.puedeEditarYBorrar(Sesion.getUsuarioActual()));

        cargarCategoriasEnMemoria();

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(FormatoMoneda.formatear(param.getValue().getPrecio())));
        colCantidad.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue().getCantidadDisponible()));

        colCategoria.setCellValueFactory(param -> {
            String nombre = nombresCategorias.getOrDefault(param.getValue().getIdCategoria(), "-");
            return new javafx.beans.property.SimpleStringProperty(nombre);
        });

        colEstado.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Producto producto, boolean vacio) {
                super.updateItem(producto, vacio);
                if (vacio || producto == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(Insignias.stock(producto.getCantidadDisponible()));
            }
        });

        colAcciones.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("✏ Editar");
            private final Button btnEliminar = new Button("🗑 Eliminar");
            private final HBox contenedor = new HBox(6, btnEditar, btnEliminar);

            @Override
            protected void updateItem(Producto producto, boolean vacio) {
                super.updateItem(producto, vacio);
                if (vacio || producto == null) {
                    setGraphic(null);
                    return;
                }
                btnEditar.setOnAction(e -> abrirFormulario(producto));
                btnEliminar.setOnAction(e -> confirmarEliminar(producto));
                setGraphic(contenedor);
            }
        });

        cargarProductos();

        campoBusqueda.textProperty().addListener((obs, anterior, nuevo) -> filtrar(nuevo));
    }

    private FilteredList<Producto> listaFiltrable;

    private void cargarProductos() {
        try {
            // Los productos desactivados ("eliminados") ya no se muestran aquí;
            // el administrador puede verlos y reactivarlos en Configuración > Productos.
            List<Producto> productos = productoController.obtenerProductos().stream()
                    .filter(p -> "activo".equals(p.getEstado()))
                    .collect(Collectors.toList());
            ObservableList<Producto> datos = FXCollections.observableArrayList(productos);
            listaFiltrable = new FilteredList<>(datos, p -> true);
            tablaProductos.setItems(listaFiltrable);
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar productos: " + e.getMessage());
        }
    }

    private void filtrar(String texto) {
        if (listaFiltrable == null) return;
        String buscado = texto == null ? "" : texto.toLowerCase();
        listaFiltrable.setPredicate(p -> p.getNombre().toLowerCase().contains(buscado));
    }

    private void cargarCategoriasEnMemoria() {
        try {
            List<Categoria> categorias = productoController.obtenerCategorias();
            for (Categoria c : categorias) {
                nombresCategorias.put(c.getIdCategoria(), c.getNombre());
            }
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar categorías: " + e.getMessage());
        }
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
            controlador.configurar(productoAEditar, this::cargarProductos);

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.setTitle(productoAEditar == null ? "Nuevo Producto" : "Editar Producto");
            dialogo.setScene(new Scene(root));
            dialogo.showAndWait();
        } catch (Exception e) {
            lblMensaje.setText("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void confirmarEliminar(Producto producto) {
        boolean acepto = Dialogos.confirmar("¿Desactivar el producto \"" + producto.getNombre() + "\"? "
                + "No se borrará su historial, solo dejará de aparecer como disponible.");
        if (acepto) {
            String error = productoController.desactivarProducto(producto.getIdProducto());
            if (error == null) {
                AuditoriaService.registrar(Sesion.getUsuarioActual(), "Desactivó producto", producto.getNombre());
                lblMensaje.setText("✔ Producto desactivado.");
                cargarProductos();
            } else {
                lblMensaje.setText("✘ " + error);
            }
        }
    }






}
