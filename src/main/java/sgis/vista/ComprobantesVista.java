package sgis.vista;

import sgis.controlador.VentaController;
import sgis.modelo.Venta;
import sgis.modelo.VentaResumen;
import sgis.servicio.ComprobanteService;
import sgis.servicio.FacturaService;
import sgis.util.Archivos;
import sgis.util.EstadoComprobante;
import sgis.util.FormatoMoneda;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Búsqueda de comprobantes de venta (comprobantes.fxml).
 *
 * Cada venta guardada es un registro del que se puede reconstruir en cualquier momento su
 * comprobante: se busca, se ve y se descarga otra vez en PDF (por ejemplo, si el cliente lo
 * necesita). Al descargar el PDF, el comprobante queda marcado como "Completado".
 */
public class ComprobantesVista implements Initializable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VentaController ventaController = new VentaController();
    private final ComprobanteService comprobanteService = new ComprobanteService();
    private final FacturaService facturaService = new FacturaService();

    private FilteredList<VentaResumen> ventasFiltradas;
    private Venta ventaMostrada;      // la venta cuyo comprobante se está viendo
    private Runnable alActualizar;    // para que la pantalla que abrió esta ventana se refresque

    @FXML private TextField campoBusqueda;
    @FXML private Label lblResultado;
    @FXML private TableView<VentaResumen> tablaVentas;
    @FXML private TableColumn<VentaResumen, String> colId;
    @FXML private TableColumn<VentaResumen, String> colFecha;
    @FXML private TableColumn<VentaResumen, String> colCliente;
    @FXML private TableColumn<VentaResumen, String> colTotal;
    @FXML private TableColumn<VentaResumen, String> colEstado;
    @FXML private TextArea areaComprobante;
    @FXML private Label lblEstadoDetalle;
    @FXML private Button btnDescargar;
    @FXML private Label lblMensaje;

    /**
     * Abre la ventana de comprobantes (la usan Ventas y Reportes).
     * @param idVentaInicial venta que debe aparecer seleccionada, o null.
     * @param alActualizar acción a ejecutar si algo cambia (ej. refrescar la tabla de quien la abrió).
     */
    public static void abrir(Integer idVentaInicial, Runnable alActualizar) {
        try {
            FXMLLoader loader = new FXMLLoader(ComprobantesVista.class.getResource("/fxml/comprobantes.fxml"));
            Parent root = loader.load();
            ComprobantesVista controlador = loader.getController();
            controlador.configurar(idVentaInicial, alActualizar);

            Stage ventana = new Stage();
            ventana.initModality(Modality.APPLICATION_MODAL);
            ventana.setTitle("Comprobantes de venta");
            ventana.setScene(new Scene(root));
            ventana.showAndWait();
        } catch (Exception e) {
            Dialogos.informar("No se pudo abrir la búsqueda de comprobantes: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(p.getValue().getIdVenta())));
        colFecha.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getFecha().format(FORMATO_FECHA)));
        colCliente.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getNombreCliente()));
        colTotal.setCellValueFactory(p -> new SimpleStringProperty(FormatoMoneda.formatear(p.getValue().getTotal())));
        colEstado.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getEstadoEnvio()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean vacio) {
                super.updateItem(estado, vacio);
                if (vacio || estado == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(Insignias.comprobante(estado));
            }
        });

        tablaVentas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nueva) -> {
            if (nueva != null) {
                mostrarComprobante(nueva.getIdVenta());
            }
        });

        cargarVentas(null);
        campoBusqueda.textProperty().addListener((obs, anterior, nuevo) -> filtrar(nuevo));
    }

    /** Se llama justo después de cargar el FXML. */
    public void configurar(Integer idVentaInicial, Runnable alActualizar) {
        this.alActualizar = alActualizar;
        if (idVentaInicial != null) {
            seleccionar(idVentaInicial);
        }
    }

    private void cargarVentas(Integer idParaSeleccionar) {
        try {
            List<VentaResumen> ventas = ventaController.obtenerVentasResumen();
            ventasFiltradas = new FilteredList<>(FXCollections.observableArrayList(ventas), v -> true);
            tablaVentas.setItems(ventasFiltradas);
            filtrar(campoBusqueda.getText());
            if (idParaSeleccionar != null) {
                seleccionar(idParaSeleccionar);
            }
        } catch (RuntimeException e) {
            Mensajes.error(lblMensaje, "Error al cargar las ventas: " + e.getMessage());
        }
    }

    private void seleccionar(int idVenta) {
        for (VentaResumen venta : tablaVentas.getItems()) {
            if (venta.getIdVenta() == idVenta) {
                tablaVentas.getSelectionModel().select(venta);
                tablaVentas.scrollTo(venta);
                return;
            }
        }
    }

    /** Filtra por N° de venta, cliente, correo, método de pago, fecha o estado. */
    private void filtrar(String texto) {
        if (ventasFiltradas == null) return;

        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        ventasFiltradas.setPredicate(v -> buscado.isEmpty()
                || String.valueOf(v.getIdVenta()).equals(buscado)
                || v.getNombreCliente().toLowerCase().contains(buscado)
                || v.getCorreoCliente().toLowerCase().contains(buscado)
                || (v.getMetodoPago() != null && v.getMetodoPago().toLowerCase().contains(buscado))
                || v.getFecha().format(FORMATO_DIA).contains(buscado)
                || EstadoComprobante.etiqueta(v.getEstadoEnvio()).toLowerCase().contains(buscado));

        if (buscado.isEmpty()) {
            lblResultado.setText("");
        } else if (ventasFiltradas.isEmpty()) {
            lblResultado.setText("✘ No se encontró ninguna venta.");
        } else {
            lblResultado.setText("✔ " + ventasFiltradas.size() + " resultado(s).");
        }
    }

    private void mostrarComprobante(int idVenta) {
        try {
            Venta venta = ventaController.obtenerVentaCompleta(idVenta);
            ventaMostrada = venta;
            Mensajes.normal(lblMensaje, "");
            if (venta == null) {
                areaComprobante.clear();
                lblEstadoDetalle.setText("");
                btnDescargar.setDisable(true);
                return;
            }
            areaComprobante.setText(comprobanteService.generarComprobante(venta, venta.getNombreCliente()));
            lblEstadoDetalle.setText("Estado: " + EstadoComprobante.etiqueta(venta.getEstadoEnvio())
                    + ". " + EstadoComprobante.detalle(venta.getEstadoEnvio()));
            btnDescargar.setDisable(false);
        } catch (RuntimeException e) {
            Mensajes.error(lblMensaje, e.getMessage());
        }
    }

    @FXML
    private void descargarFactura() {
        if (ventaMostrada == null) {
            return;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar factura");
        selector.setInitialFileName("factura_venta_" + ventaMostrada.getIdVenta() + ".pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        File destino = selector.showSaveDialog(btnDescargar.getScene().getWindow());
        if (destino == null) {
            return; // el usuario canceló
        }
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        }

        try {
            String[] contacto = ventaController.contactoDelCliente(ventaMostrada.getIdVenta());
            facturaService.generarFactura(ventaMostrada, ventaMostrada.getNombreCliente(),
                    ventaMostrada.getCorreoCliente(), contacto[0], contacto[1], destino);

            // El PDF se generó y se guardó: el comprobante queda completado.
            int idVenta = ventaMostrada.getIdVenta();
            String error = ventaController.marcarComprobanteCompletado(idVenta);
            if (error == null) {
                cargarVentas(idVenta); // refresca el estado en la lista
                if (alActualizar != null) {
                    alActualizar.run();
                }
            }
            // El PDF ya se guardó: el aviso es de éxito aunque el estado no se haya podido actualizar (se explica al lado).
            Mensajes.exito(lblMensaje, error == null ? "Factura descargada" : "Factura descargada. " + error, destino.getAbsolutePath());
            Archivos.abrir(destino);
        } catch (IOException | RuntimeException e) {
            Mensajes.error(lblMensaje, "No se pudo generar la factura: " + e.getMessage());
        }
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) lblMensaje.getScene().getWindow();
        stage.close();
    }
}
