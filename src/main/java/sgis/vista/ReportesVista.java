package sgis.vista;

import sgis.controlador.MovimientoController;
import sgis.controlador.ProductoController;
import sgis.controlador.ReporteController;
import sgis.controlador.UsuarioController;
import sgis.controlador.VentaController;
import sgis.modelo.Actividad;
import sgis.modelo.Producto;
import sgis.modelo.ResumenDia;
import sgis.modelo.RegistroAuditoria;
import sgis.modelo.ResumenInventario;
import sgis.modelo.Usuario;
import sgis.modelo.VentaResumen;
import sgis.servicio.InformeService;
import sgis.servicio.AuditoriaService;
import sgis.servicio.RespaldoService;
import sgis.util.FormatoMoneda;
import sgis.util.Sesion;
import sgis.util.Permisos;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla Reportes (reportes.fxml).
 * Muestra los indicadores generales y, en pestañas: el historial de ventas y de
 * movimientos, las ventas de la semana (gráfica), el informe de ventas en PDF
 * y el respaldo de la base de datos.
 */
public class ReportesVista implements Initializable {

    private final ReporteController reporteController = new ReporteController();
    private final VentaController ventaController = new VentaController();
    private final MovimientoController movimientoController = new MovimientoController();
    private final ProductoController productoController = new ProductoController();
    private final InformeService informeService = new InformeService();
    private final UsuarioController usuarioController = new UsuarioController();
    private final Map<Integer, String> nombresUsuarios = new HashMap<>();
    private FilteredList<Actividad> historialFiltrado;
    private FilteredList<RegistroAuditoria> auditoriaFiltrada;
    private final Map<Integer, String> nombresProductos = new HashMap<>();

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] NOMBRES_DIA = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    /** Lunes de la semana que se está mostrando en la gráfica. */
    private LocalDate inicioSemana =
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

    @FXML private Label kpiTotalProductos;
    @FXML private Label kpiValorInventario;
    @FXML private Label kpiStockSuficiente;
    @FXML private Label kpiStockMedio;
    @FXML private Label kpiStockBajo;
    @FXML private Label kpiStockAgotado;
    @FXML private Label kpiVentasHoy;
    @FXML private Label lblMensaje;

    @FXML private TableView<VentaResumen> tablaVentas;
    @FXML private TableColumn<VentaResumen, Integer> colVentaId;
    @FXML private TableColumn<VentaResumen, String> colVentaCliente;
    @FXML private TableColumn<VentaResumen, String> colVentaTotal;
    @FXML private TableColumn<VentaResumen, String> colVentaMetodoPago;
    @FXML private TableColumn<VentaResumen, VentaResumen> colVentaEstadoEnvio;
    @FXML private TableColumn<VentaResumen, String> colVentaFecha;
    @FXML private TableColumn<VentaResumen, String> colVentaUsuario;

    @FXML private TextField campoBusquedaMov;
    @FXML private Label lblResultadoMov;
    @FXML private TableView<Actividad> tablaMovimientos;
    @FXML private TableColumn<Actividad, String> colMovFecha;
    @FXML private TableColumn<Actividad, String> colMovDescripcion;
    @FXML private TableColumn<Actividad, String> colMovCantidad;
    @FXML private TableColumn<Actividad, String> colMovValor;
    @FXML private TableColumn<Actividad, String> colMovUsuario;
    @FXML private TableColumn<Actividad, String> colMovCliente;

    @FXML private TabPane pestanasReportes;
    @FXML private Tab tabAuditoria;
    @FXML private TextField campoBusquedaAud;
    @FXML private Label lblResultadoAud;
    @FXML private TableView<RegistroAuditoria> tablaAuditoria;
    @FXML private TableColumn<RegistroAuditoria, String> colAudFecha;
    @FXML private TableColumn<RegistroAuditoria, String> colAudUsuario;
    @FXML private TableColumn<RegistroAuditoria, String> colAudAccion;
    @FXML private TableColumn<RegistroAuditoria, String> colAudDetalle;

    @FXML private BarChart<String, Number> graficaSemana;
    @FXML private NumberAxis ejeY;
    @FXML private Label lblSemana;
    @FXML private Label lblTotalSemana;

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Label lblInforme;

    @FXML private Label lblRespaldo;
    @FXML private Label lblCarpetaRespaldo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colVentaId.setCellValueFactory(new PropertyValueFactory<>("idVenta"));
        colVentaCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        // Columna "Comprobante": estado (Completado / Pendiente) y botón "Ver comprobante"
        colVentaEstadoEnvio.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colVentaEstadoEnvio.setCellFactory(col -> new TableCell<>() {
            private final Button btnVer = new Button("Ver comprobante");
            private final HBox contenedor = new HBox(8);

            {
                btnVer.getStyleClass().add("boton-secundario");
                btnVer.setStyle("-fx-padding: 2 10 2 10; -fx-font-size: 11;");
                contenedor.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(VentaResumen venta, boolean vacio) {
                super.updateItem(venta, vacio);
                if (vacio || venta == null) {
                    setGraphic(null);
                    return;
                }
                btnVer.setOnAction(e -> abrirComprobante(venta.getIdVenta()));
                contenedor.getChildren().setAll(Insignias.comprobante(venta.getEstadoEnvio()), btnVer);
                setGraphic(contenedor);
            }
        });

        // Doble clic sobre una venta: abre su comprobante
        tablaVentas.setOnMouseClicked(evento -> {
            if (evento.getClickCount() == 2 && tablaVentas.getSelectionModel().getSelectedItem() != null) {
                abrirComprobante(tablaVentas.getSelectionModel().getSelectedItem().getIdVenta());
            }
        });
        colVentaMetodoPago.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));
        colVentaTotal.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(FormatoMoneda.formatear(param.getValue().getTotal())));
        colVentaFecha.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getFecha().format(FORMATO_FECHA)));

        // Quién registró cada venta y cada movimiento (nombre y rol del usuario del sistema)
        colVentaUsuario.setCellValueFactory(param -> {
            Integer id = param.getValue().getIdUsuario();
            String texto = id == null ? "- (venta anterior al control de usuarios)"
                                      : nombresUsuarios.getOrDefault(id, "Usuario #" + id);
            return new javafx.beans.property.SimpleStringProperty(texto);
        });

        // Historial de movimientos: qué pasó, cuántas unidades, cuánto valió, quién lo hizo y quién compró.
        colMovFecha.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getFecha().format(FORMATO_FECHA)));
        colMovDescripcion.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getDescripcion()));
        // El signo indica el sentido: +20 entraron unidades al inventario, −3 salieron (venta o salida).
        colMovCantidad.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(
                        (param.getValue().esSalidaDeStock() ? "−" : "+") + param.getValue().getCantidad()));
        colMovValor.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(
                        param.getValue().getValor() == null ? "-" : FormatoMoneda.formatear(param.getValue().getValor())));
        colMovUsuario.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(textoUsuarioDe(param.getValue())));
        colMovCliente.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(
                        param.getValue().getCliente() == null ? "-" : param.getValue().getCliente()));

        // Movimientos de configuración (auditoría): quién tocó el sistema
        colAudFecha.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getFecha().format(FORMATO_FECHA)));
        colAudUsuario.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getUsuario()));
        colAudAccion.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getAccion()));
        colAudDetalle.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(
                        param.getValue().getDetalle() == null ? "" : param.getValue().getDetalle()));

        // Eje vertical de la gráfica de la semana en pesos
        ejeY.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number valor) {
                return FormatoMoneda.formatear(valor.doubleValue());
            }

            @Override
            public Number fromString(String texto) {
                return 0;
            }
        });

        // Por defecto, el informe cubre desde el primer día del mes hasta hoy
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());

        cargarNombresProductos();
        cargarNombresUsuarios();
        cargarResumen();
        cargarVentas();
        cargarMovimientos();
        campoBusquedaMov.textProperty().addListener((obs, anterior, nuevo) -> filtrarHistorial(nuevo));
        // La auditoría (quién vio contraseñas, accesos fallidos...) es solo para el Administrador:
        // otros roles con acceso a Reportes no ven esta pestaña.
        if (Permisos.esAdministrativo(Sesion.getUsuarioActual())) {
            cargarAuditoria();
            campoBusquedaAud.textProperty().addListener((obs, anterior, nuevo) -> filtrarAuditoria(nuevo));
        } else {
            pestanasReportes.getTabs().remove(tabAuditoria);
        }
        cargarSemana();
        actualizarEstadoRespaldo();
    }

    // ---------------------------------------------------------------
    // Indicadores e historiales
    // ---------------------------------------------------------------
    private void cargarNombresProductos() {
        try {
            List<Producto> productos = productoController.obtenerProductos();
            for (Producto p : productos) {
                nombresProductos.put(p.getIdProducto(), p.getNombre());
            }
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar productos: " + e.getMessage());
        }
    }

    private void cargarNombresUsuarios() {
        try {
            for (Usuario u : usuarioController.obtenerUsuarios()) {
                nombresUsuarios.put(u.getIdUsuario(), u.getNombre() + " (" + u.getRol() + ")");
            }
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar usuarios: " + e.getMessage());
        }
    }

    private void cargarResumen() {
        try {
            ResumenInventario r = reporteController.obtenerResumen();
            kpiTotalProductos.setText(String.valueOf(r.getTotalProductos()));
            kpiValorInventario.setText(FormatoMoneda.formatear(r.getValorTotalInventario()));
            kpiStockSuficiente.setText(String.valueOf(r.getStockSuficiente()));
            kpiStockMedio.setText(String.valueOf(r.getStockMedio()));
            kpiStockBajo.setText(String.valueOf(r.getStockBajo()));
            kpiStockAgotado.setText(String.valueOf(r.getStockAgotado()));
            kpiVentasHoy.setText(FormatoMoneda.formatear(r.getTotalVentasHoy()));
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al calcular el resumen: " + e.getMessage());
        }
    }

    private void cargarVentas() {
        try {
            List<VentaResumen> ventas = ventaController.obtenerVentasResumen();
            tablaVentas.setItems(FXCollections.observableArrayList(ventas));
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar ventas: " + e.getMessage());
        }
    }

    private void cargarMovimientos() {
        try {
            List<Actividad> historial = movimientoController.obtenerHistorial();
            historialFiltrado = new FilteredList<>(FXCollections.observableArrayList(historial), a -> true);
            tablaMovimientos.setItems(historialFiltrado);
            filtrarHistorial(campoBusquedaMov.getText());
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar movimientos: " + e.getMessage());
        }
    }

    /** Nombre (y rol) de quien hizo el movimiento. */
    private String textoUsuarioDe(Actividad actividad) {
        Integer id = actividad.getIdUsuario();
        if (id == null) {
            return "- (venta anterior al control de usuarios)";
        }
        return nombresUsuarios.getOrDefault(id, "Usuario #" + id);
    }

    /** Filtra el historial por lo que describe, quién lo hizo, el cliente, el producto o la fecha. */
    private void filtrarHistorial(String texto) {
        if (historialFiltrado == null) return;

        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        historialFiltrado.setPredicate(a -> buscado.isEmpty()
                || a.getDescripcion().toLowerCase().contains(buscado)
                || textoUsuarioDe(a).toLowerCase().contains(buscado)
                || (a.getCliente() != null && a.getCliente().toLowerCase().contains(buscado))
                || a.getFecha().format(FORMATO_FECHA).contains(buscado));

        if (buscado.isEmpty()) {
            lblResultadoMov.setText(historialFiltrado.size() + " movimiento(s)");
        } else if (historialFiltrado.isEmpty()) {
            lblResultadoMov.setText("✘ No se encontró nada.");
        } else {
            lblResultadoMov.setText("✔ " + historialFiltrado.size() + " resultado(s).");
        }
    }

    // ---------------------------------------------------------------
    // Comprobantes
    // ---------------------------------------------------------------
    @FXML
    private void abrirComprobantes(ActionEvent event) {
        abrirComprobante(null);
    }

    private void abrirComprobante(Integer idVenta) {
        // Al volver, se recarga el historial porque el estado del comprobante pudo cambiar.
        ComprobantesVista.abrir(idVenta, null);
        cargarVentas();
    }

    // ---------------------------------------------------------------
    // Movimientos de configuración (auditoría)
    // ---------------------------------------------------------------
    private void cargarAuditoria() {
        try {
            List<RegistroAuditoria> registros = AuditoriaService.listar();
            auditoriaFiltrada = new FilteredList<>(FXCollections.observableArrayList(registros), r -> true);
            tablaAuditoria.setItems(auditoriaFiltrada);
            filtrarAuditoria(campoBusquedaAud.getText());
        } catch (RuntimeException e) {
            lblResultadoAud.setText("✘ " + e.getMessage());
        }
    }

    /** Filtra por quién lo hizo, qué hizo, sobre qué o la fecha. */
    private void filtrarAuditoria(String texto) {
        if (auditoriaFiltrada == null) return;

        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        auditoriaFiltrada.setPredicate(r -> buscado.isEmpty()
                || r.getUsuario().toLowerCase().contains(buscado)
                || r.getAccion().toLowerCase().contains(buscado)
                || (r.getDetalle() != null && r.getDetalle().toLowerCase().contains(buscado))
                || r.getFecha().format(FORMATO_FECHA).contains(buscado));

        if (buscado.isEmpty()) {
            lblResultadoAud.setText(auditoriaFiltrada.size() + " registro(s)");
        } else if (auditoriaFiltrada.isEmpty()) {
            lblResultadoAud.setText("✘ No se encontró nada.");
        } else {
            lblResultadoAud.setText("✔ " + auditoriaFiltrada.size() + " resultado(s).");
        }
    }

    @FXML
    private void actualizarAuditoria(ActionEvent event) {
        cargarAuditoria();
    }

    @FXML
    private void actualizarMovimientos(ActionEvent event) {
        cargarMovimientos();
    }

    // ---------------------------------------------------------------
    // Ventas de la semana
    // ---------------------------------------------------------------
    private void cargarSemana() {
        LocalDate fin = inicioSemana.plusDays(6);
        lblSemana.setText("Semana del " + inicioSemana.format(FORMATO_DIA) + " al " + fin.format(FORMATO_DIA));

        Map<LocalDate, ResumenDia> porDia = new HashMap<>();
        try {
            for (ResumenDia dia : ventaController.obtenerTotalesPorDia(inicioSemana, fin)) {
                porDia.put(dia.getFecha(), dia);
            }
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar las ventas de la semana: " + e.getMessage());
            return;
        }

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        double totalSemana = 0;
        int ventasSemana = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate fecha = inicioSemana.plusDays(i);
            ResumenDia dia = porDia.get(fecha);
            double total = dia == null ? 0 : dia.getTotal();
            int cantidad = dia == null ? 0 : dia.getCantidadVentas();
            totalSemana += total;
            ventasSemana += cantidad;

            XYChart.Data<String, Number> barra =
                    new XYChart.Data<>(NOMBRES_DIA[i] + " " + fecha.getDayOfMonth(), total);
            serie.getData().add(barra);
            decorarBarra(barra, FormatoMoneda.formatear(total) + " (" + cantidad + " venta(s))");
        }

        graficaSemana.getData().clear();
        graficaSemana.getData().add(serie);
        lblTotalSemana.setText("Total de la semana: " + FormatoMoneda.formatear(totalSemana)
                + " en " + ventasSemana + " venta(s).");
    }

    /** Pone color verde y un mensaje emergente (al pasar el mouse) a una barra de la gráfica. */
    private void decorarBarra(XYChart.Data<String, Number> barra, String texto) {
        if (barra.getNode() != null) {
            aplicarDecoracion(barra.getNode(), texto);
        }
        barra.nodeProperty().addListener((obs, anterior, nodo) -> {
            if (nodo != null) {
                aplicarDecoracion(nodo, texto);
            }
        });
    }

    private void aplicarDecoracion(javafx.scene.Node nodo, String texto) {
        nodo.setStyle("-fx-bar-fill: #2ECC71;");
        Tooltip.install(nodo, new Tooltip(texto));
    }

    @FXML
    private void semanaAnterior(ActionEvent event) {
        inicioSemana = inicioSemana.minusWeeks(1);
        cargarSemana();
    }

    @FXML
    private void semanaSiguiente(ActionEvent event) {
        inicioSemana = inicioSemana.plusWeeks(1);
        cargarSemana();
    }

    @FXML
    private void semanaActual(ActionEvent event) {
        inicioSemana = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        cargarSemana();
    }

    // ---------------------------------------------------------------
    // Informe de ventas en PDF
    // ---------------------------------------------------------------
    @FXML
    private void generarInforme(ActionEvent event) {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null) {
            lblInforme.setText("✘ Elige las dos fechas (desde y hasta).");
            return;
        }
        if (desde.isAfter(hasta)) {
            lblInforme.setText("✘ La fecha \"desde\" no puede ser posterior a la fecha \"hasta\".");
            return;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar informe de ventas");
        selector.setInitialFileName("informe_ventas_" + desde + "_a_" + hasta + ".pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        File destino = selector.showSaveDialog(lblInforme.getScene().getWindow());
        if (destino == null) {
            return; // el usuario canceló
        }
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        }

        try {
            List<VentaResumen> ventas = ventaController.obtenerVentasResumen(desde, hasta);
            informeService.generarInformeVentas(ventas, desde, hasta, destino);
            lblInforme.setText("✔ Informe guardado en: " + destino.getAbsolutePath()
                    + "  (" + ventas.size() + " venta(s))");
            abrirArchivo(destino);
        } catch (IOException | RuntimeException e) {
            lblInforme.setText("✘ No se pudo generar el informe: " + e.getMessage());
        }
    }

    /** Abre el archivo con el programa predeterminado del computador (desde ahí se puede imprimir). */
    private void abrirArchivo(File archivo) {
        Thread hilo = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(archivo);
                }
            } catch (IOException | RuntimeException e) {
                // Si no se puede abrir, el informe ya quedó guardado igualmente.
            }
        }, "abrir-informe");
        hilo.setDaemon(true);
        hilo.start();
    }

    // ---------------------------------------------------------------
    // Respaldo de la base de datos
    // ---------------------------------------------------------------
    private void actualizarEstadoRespaldo() {
        Optional<LocalDateTime> ultimo = RespaldoService.fechaUltimoRespaldo();
        lblRespaldo.setText(ultimo
                .map(fecha -> "Último respaldo: " + fecha.format(FORMATO_FECHA))
                .orElse("Aún no hay respaldos. Se creará uno automáticamente."));
        lblCarpetaRespaldo.setText("Carpeta de respaldos: " + RespaldoService.getCarpeta());
    }

    @FXML
    private void respaldarAhora(ActionEvent event) {
        try {
            Path archivo = RespaldoService.generarRespaldo();
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Respaldó la base de datos (manual)", archivo.getFileName().toString());
            actualizarEstadoRespaldo();
            lblRespaldo.setText("✔ Respaldo creado: " + archivo.getFileName());
        } catch (SQLException | IOException e) {
            lblRespaldo.setText("✘ No se pudo crear el respaldo: " + e.getMessage());
        }
    }






}
