package sgis.vista;

import sgis.controlador.ClienteController;
import sgis.controlador.ProductoController;
import sgis.controlador.VentaController;
import sgis.modelo.DetalleVenta;
import sgis.modelo.Producto;
import sgis.modelo.Venta;
import sgis.servicio.FacturaService;
import sgis.util.Archivos;
import sgis.util.FormatoMoneda;
import sgis.util.Sesion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador de la pantalla Ventas (ventas.fxml).
 * Maneja el carrito de compra en memoria y, al confirmar, delega todo
 * el registro (venta + detalle + stock + comprobante + correo) a VentaController.
 */
public class VentasVista implements Initializable {

    private final ProductoController productoController = new ProductoController();
    private final VentaController ventaController = new VentaController();
    private final ClienteController clienteController = new ClienteController();
    private final FacturaService facturaService = new FacturaService();
    private AutocompletarCliente autocompletar;

    private final ObservableList<DetalleVenta> carrito = FXCollections.observableArrayList();

    @FXML private ComboBox<Producto> comboProducto;
    @FXML private TextField campoCantidadCarrito;
    @FXML private TableView<DetalleVenta> tablaCarrito;
    @FXML private TableColumn<DetalleVenta, String> colProducto;
    @FXML private TableColumn<DetalleVenta, String> colPrecioUnitario;
    @FXML private TableColumn<DetalleVenta, Integer> colCantidad;
    @FXML private TableColumn<DetalleVenta, String> colSubtotal;
    @FXML private TableColumn<DetalleVenta, DetalleVenta> colAccionesCarrito;

    @FXML private TextField campoNombreCliente;
    @FXML private TextField campoCorreoCliente;
    @FXML private ComboBox<String> comboMetodoPago;
    @FXML private Button btnFactura;
    @FXML private Label lblTotal;
    @FXML private Label lblMensaje;
    @FXML private TextArea areaComprobante;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cargarProductos();
        comboMetodoPago.setItems(FXCollections.observableArrayList("Efectivo", "Tarjeta", "Transferencia"));
        comboMetodoPago.setValue("Efectivo");

        // Autocompletado: al escribir, sugiere clientes ya registrados
        autocompletar = new AutocompletarCliente(campoNombreCliente, campoCorreoCliente);
        cargarClientes();
        btnFactura.setDisable(true); // se activa cuando se registra una venta

        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colPrecioUnitario.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(FormatoMoneda.formatear(param.getValue().getPrecioUnitario())));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colSubtotal.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(FormatoMoneda.formatear(param.getValue().getSubtotal())));

        colAccionesCarrito.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colAccionesCarrito.setCellFactory(col -> new TableCell<>() {
            private final Button btnQuitar = new Button("🗑");
            private final HBox contenedor = new HBox(btnQuitar);

            @Override
            protected void updateItem(DetalleVenta linea, boolean vacio) {
                super.updateItem(linea, vacio);
                if (vacio || linea == null) {
                    setGraphic(null);
                    return;
                }
                btnQuitar.setOnAction(e -> {
                    carrito.remove(linea);
                    actualizarTotal();
                });
                setGraphic(contenedor);
            }
        });

        tablaCarrito.setItems(carrito);
        actualizarTotal();
    }

    private void cargarProductos() {
        try {
            List<Producto> productos = productoController.obtenerProductos().stream()
                    .filter(p -> "activo".equals(p.getEstado()))
                    .collect(Collectors.toList());
            comboProducto.setItems(FXCollections.observableArrayList(productos));
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar productos: " + e.getMessage());
        }
    }

    @FXML
    private void agregarAlCarrito(ActionEvent event) {
        Producto producto = comboProducto.getValue();
        if (producto == null) {
            lblMensaje.setText("Selecciona un producto.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(campoCantidadCarrito.getText().trim());
            if (cantidad <= 0) {
                lblMensaje.setText("La cantidad debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            lblMensaje.setText("Ingresa una cantidad válida.");
            return;
        }

        // Si el producto ya está en el carrito, solo le suma la cantidad.
        for (DetalleVenta linea : carrito) {
            if (linea.getIdProducto() == producto.getIdProducto()) {
                linea.setCantidad(linea.getCantidad() + cantidad);
                tablaCarrito.refresh();
                actualizarTotal();
                return;
            }
        }

        DetalleVenta nuevaLinea = new DetalleVenta(
                producto.getIdProducto(), producto.getNombre(), cantidad, producto.getPrecio());
        carrito.add(nuevaLinea);
        actualizarTotal();
        lblMensaje.setText("");
    }

    private void actualizarTotal() {
        double total = carrito.stream().mapToDouble(DetalleVenta::getSubtotal).sum();
        lblTotal.setText(FormatoMoneda.formatear(total));
    }

    @FXML
    private void confirmarVenta(ActionEvent event) {
        if (carrito.isEmpty()) {
            lblMensaje.setText("Agrega al menos un producto al carrito.");
            return;
        }
        String nombre = campoNombreCliente.getText();
        String correo = campoCorreoCliente.getText();
        if (nombre == null || nombre.isBlank() || correo == null || correo.isBlank()) {
            lblMensaje.setText("Completa el nombre y correo del cliente.");
            return;
        }

        String metodoPago = comboMetodoPago.getValue().toLowerCase();
        Integer idUsuario = Sesion.getUsuarioActual() == null ? null : Sesion.getUsuarioActual().getIdUsuario();
        String error = ventaController.registrarVenta(nombre, correo, metodoPago, idUsuario, List.copyOf(carrito));

        if (error == null) {
            lblMensaje.setText("✔ Venta registrada correctamente. Comprobante generado abajo.");
            areaComprobante.setText(ventaController.getUltimoComprobante());
            carrito.clear();
            actualizarTotal();
            campoNombreCliente.clear();
            campoCorreoCliente.clear();
            comboMetodoPago.setValue("Efectivo");
            cargarProductos(); // refresca precios/stock por si algo cambió
            cargarClientes();  // el cliente de esta venta ya queda registrado
            btnFactura.setDisable(false);
        } else {
            lblMensaje.setText("✘ " + error);
        }
    }

    /** Abre la búsqueda de comprobantes (para entregar de nuevo uno a un cliente). */
    @FXML
    private void buscarComprobante(ActionEvent event) {
        ComprobantesVista.abrir(null, null);
    }

    private void cargarClientes() {
        try {
            autocompletar.setClientes(clienteController.obtenerClientes());
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar los clientes registrados: " + e.getMessage());
        }
    }

    /** Genera en PDF la factura de la última venta registrada en esta pantalla. */
    @FXML
    private void generarFactura(ActionEvent event) {
        Venta venta = ventaController.getUltimaVenta();
        if (venta == null) {
            lblMensaje.setText("Primero registra una venta para poder generar su factura.");
            return;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar factura");
        selector.setInitialFileName("factura_venta_" + venta.getIdVenta() + ".pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        File destino = selector.showSaveDialog(btnFactura.getScene().getWindow());
        if (destino == null) {
            return; // el usuario canceló
        }
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        }

        try {
            facturaService.generarFactura(venta, ventaController.getUltimoNombreCliente(),
                    ventaController.getUltimoCorreoCliente(), destino);
            // El PDF se generó y se guardó: el comprobante de esta venta queda completado.
            String errorEstado = ventaController.marcarComprobanteCompletado(venta.getIdVenta());
            lblMensaje.setText(errorEstado == null
                    ? "✔ Factura guardada en: " + destino.getAbsolutePath() + " (comprobante completado)"
                    : "✔ Factura guardada en: " + destino.getAbsolutePath() + ". " + errorEstado);
            Archivos.abrir(destino);
        } catch (IOException | RuntimeException e) {
            lblMensaje.setText("✘ No se pudo generar la factura: " + e.getMessage());
        }
    }
}
