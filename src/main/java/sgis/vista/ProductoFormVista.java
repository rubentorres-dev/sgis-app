package sgis.vista;

import sgis.controlador.CategoriaController;
import sgis.controlador.ProductoController;
import sgis.modelo.Categoria;
import sgis.modelo.Producto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controlador del formulario de Nuevo/Editar Producto (productoForm.fxml).
 * Se abre como un diálogo modal desde ProductosVista.
 */
public class ProductoFormVista implements Initializable {

    private final ProductoController productoController = new ProductoController();
    private final CategoriaController categoriaController = new CategoriaController();

    /** Opción al final de la lista de categorías: cualquier usuario puede crear una categoría nueva desde aquí. */
    private final Categoria opcionNuevaCategoria = new Categoria(-1, "➕ Nueva categoría");

    @FXML private Label lblTitulo;
    @FXML private TextField campoNombre;
    @FXML private TextField campoDescripcion;
    @FXML private ComboBox<Categoria> comboCategoria;
    @FXML private TextField campoPrecio;
    @FXML private TextField campoCantidad;
    @FXML private ComboBox<String> comboEstado;
    @FXML private Label lblMensaje;

    private Producto productoAEditar; // null si es un producto nuevo
    private Runnable alGuardar;       // se ejecuta después de guardar, para refrescar la tabla

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboEstado.setItems(FXCollections.observableArrayList("activo", "inactivo"));
        recargarCategorias();
        comboCategoria.valueProperty().addListener((obs, anterior, nueva) -> {
            if (nueva == opcionNuevaCategoria) {
                Platform.runLater(() -> crearCategoriaDesdeLista(anterior));
            }
        });
    }

    /** Carga las categorías en la lista y deja «➕ Nueva categoría» como última opción. */
    private void recargarCategorias() {
        try {
            List<Categoria> opciones = new ArrayList<>(productoController.obtenerCategorias());
            opciones.add(opcionNuevaCategoria);
            comboCategoria.setItems(FXCollections.observableArrayList(opciones));
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar categorías: " + e.getMessage());
        }
    }

    /** Pide el nombre, crea la categoría (no hace falta tener acceso a Configuración) y la deja elegida. */
    private void crearCategoriaDesdeLista(Categoria seleccionAnterior) {
        Optional<String> nombre = Dialogos.pedirTexto("Nueva categoría", "Nombre de la categoría:", "");
        if (nombre.isEmpty()) {
            comboCategoria.setValue(seleccionAnterior);
            return;
        }
        String error = categoriaController.crear(nombre.get());
        if (error != null) {
            comboCategoria.setValue(seleccionAnterior);
            lblMensaje.setStyle("");
            lblMensaje.setText("✘ " + error);
            return;
        }
        recargarCategorias();
        String buscado = nombre.get().trim();
        for (Categoria c : comboCategoria.getItems()) {
            if (c.getNombre().equalsIgnoreCase(buscado)) {
                comboCategoria.setValue(c);
                break;
            }
        }
        lblMensaje.setStyle("-fx-text-fill: #1E9E5B;");
        lblMensaje.setText("✔ Categoría \"" + buscado + "\" creada.");
    }

    /**
     * Debe llamarse justo después de cargar el FXML, antes de mostrar la ventana.
     * @param producto el producto a editar, o null si se va a crear uno nuevo.
     * @param alGuardar acción a ejecutar después de guardar exitosamente (ej. refrescar la tabla).
     */
    public void configurar(Producto producto, Runnable alGuardar) {
        this.productoAEditar = producto;
        this.alGuardar = alGuardar;

        if (producto != null) {
            lblTitulo.setText("Editar Producto");
            campoNombre.setText(producto.getNombre());
            campoDescripcion.setText(producto.getDescripcion());
            campoPrecio.setText(String.valueOf(producto.getPrecio()));
            campoCantidad.setText(String.valueOf(producto.getCantidadDisponible()));
            comboEstado.setValue(producto.getEstado());
            for (Categoria c : comboCategoria.getItems()) {
                if (c.getIdCategoria() == producto.getIdCategoria()) {
                    comboCategoria.setValue(c);
                    break;
                }
            }
        } else {
            comboEstado.setValue("activo");
        }
    }

    @FXML
    private void guardar() {
        String nombre = campoNombre.getText();
        Categoria categoria = comboCategoria.getValue();

        if (nombre == null || nombre.isBlank()) {
            lblMensaje.setText("El nombre es obligatorio.");
            return;
        }
        if (categoria == null || categoria == opcionNuevaCategoria) {
            lblMensaje.setText("Selecciona una categoría.");
            return;
        }

        double precio;
        int cantidad;
        try {
            precio = Double.parseDouble(campoPrecio.getText().trim());
            cantidad = Integer.parseInt(campoCantidad.getText().trim());
        } catch (NumberFormatException e) {
            lblMensaje.setText("Precio y cantidad deben ser números válidos.");
            return;
        }

        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(campoDescripcion.getText());
        p.setPrecio(precio);
        p.setCantidadDisponible(cantidad);
        p.setEstado(comboEstado.getValue());
        p.setIdCategoria(categoria.getIdCategoria());

        String error;
        if (productoAEditar == null) {
            error = productoController.registrarProducto(p);
        } else {
            p.setIdProducto(productoAEditar.getIdProducto());
            error = productoController.actualizarProducto(p);
        }

        if (error == null) {
            if (alGuardar != null) alGuardar.run();
            cerrarVentana();
        } else {
            lblMensaje.setText(error);
        }
    }

    @FXML
    private void cancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) lblTitulo.getScene().getWindow();
        stage.close();
    }
}
