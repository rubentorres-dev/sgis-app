package sgis.vista;

import sgis.controlador.ProductoController;
import sgis.controlador.ReporteController;
import sgis.modelo.Producto;
import sgis.modelo.ResumenInventario;
import sgis.modelo.Usuario;
import sgis.util.FormatoMoneda;
import sgis.util.Sesion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador de la pantalla Panel Principal (panelPrincipal.fxml).
 * Trae los productos y los KPIs reales de la base de datos y los
 * muestra con el mismo estilo ya validado en el diseño de Rocket.
 */
public class PanelPrincipalVista implements Initializable {

    private final ProductoController productoController = new ProductoController();
    private final ReporteController reporteController = new ReporteController();

    @FXML private Label lblBienvenida;
    @FXML private Label lblMensaje;
    @FXML private Label kpiTotalProductos;
    @FXML private Label kpiStockSuficiente;
    @FXML private Label kpiStockMedio;
    @FXML private Label kpiStockBajo;
    @FXML private Label kpiStockAgotado;
    @FXML private Label kpiVentasHoy;

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colPrecio;
    @FXML private TableColumn<Producto, Integer> colCantidad;
    @FXML private TableColumn<Producto, Producto> colEstado;

    @FXML private PieChart graficaProductos;
    @FXML private Label lblDonaTitulo;
    @FXML private Label lblDonaValor;
    @FXML private Label lblDonaDetalle;
    @FXML private VBox leyendaProductos;

    private static final String[] COLORES_BASE = {
            "#2ECC71", "#2C7BE5", "#F5A623", "#E74C3C", "#9B59B6",
            "#1ABC9C", "#E67E22", "#34495E", "#FD79A8", "#7F8C8D"};

    private static final double PESO_VISUAL_MINIMO = 0.04; // como máximo 4 % del círculo por sección mínima

    private int totalUnidades = 0;
    private Porcion seleccionada; // porción marcada con un clic (o null)

    /** Datos de una porción de la dona (un producto). */
    private static class Porcion {
        final PieChart.Data data;
        final String nombre;
        final int unidades;
        final double porcentaje;
        final String color;

        Porcion(PieChart.Data data, String nombre, int unidades, double porcentaje, String color) {
            this.data = data;
            this.nombre = nombre;
            this.unidades = unidades;
            this.porcentaje = porcentaje;
            this.color = color;
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Usuario usuario = Sesion.getUsuarioActual();
        if (usuario != null) {
            lblBienvenida.setText("¡Bienvenido, " + usuario.getNombre() + "! (" + usuario.getRol() + ")");
        }

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(FormatoMoneda.formatear(param.getValue().getPrecio())));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadDisponible"));

        // Columna de estado: no muestra el texto "activo/inactivo" de la base de datos,
        // sino un badge de color calculado a partir de la cantidad disponible
        // (Agotado / Stock bajo / Stock medio / Suficiente).
        colEstado.setCellValueFactory(param ->
                new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colEstado.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
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

        cargarProductos();
        cargarResumen();
        cargarGraficaProductos();
    }

    private void cargarProductos() {
        try {
            List<Producto> productos = productoController.obtenerProductos().stream()
                    .filter(p -> "activo".equals(p.getEstado()))
                    .collect(Collectors.toList());
            ObservableList<Producto> datos = FXCollections.observableArrayList(productos);
            tablaProductos.setItems(datos);
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar productos: " + e.getMessage());
        }
    }

    private void cargarResumen() {
        try {
            ResumenInventario r = reporteController.obtenerResumen();
            kpiTotalProductos.setText(String.valueOf(r.getTotalProductos()));
            kpiStockSuficiente.setText(String.valueOf(r.getStockSuficiente()));
            kpiStockMedio.setText(String.valueOf(r.getStockMedio()));
            kpiStockBajo.setText(String.valueOf(r.getStockBajo()));
            kpiStockAgotado.setText(String.valueOf(r.getStockAgotado()));
            kpiVentasHoy.setText(FormatoMoneda.formatear(r.getTotalVentasHoy()));
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al calcular el resumen: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Gráfica circular interactiva: stock por producto
    // ---------------------------------------------------------------
    private void cargarGraficaProductos() {
        List<Map.Entry<String, Integer>> stock;
        try {
            stock = reporteController.obtenerStockPorProducto();
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar la gráfica de productos: " + e.getMessage());
            return;
        }

        totalUnidades = 0;
        for (Map.Entry<String, Integer> fila : stock) {
            totalUnidades += fila.getValue();
        }
        seleccionada = null;
        leyendaProductos.getChildren().clear();

        // Mínimo visual por sección: 4 % del círculo, pero se reduce si hay muchos productos
        // para que, como mucho, la mitad del círculo sea "tamaño mínimo" y el resto siga proporcional.
        double minimoVisual = stock.isEmpty() ? 0 : Math.min(PESO_VISUAL_MINIMO, 0.5 / stock.size());

        List<PieChart.Data> datos = new ArrayList<>();
        List<Porcion> porciones = new ArrayList<>();
        for (int i = 0; i < stock.size(); i++) {
            String nombre = stock.get(i).getKey();
            int unidades = stock.get(i).getValue();
            double porcentaje = unidades * 100.0 / totalUnidades;

            // Las secciones muy pequeñas se dibujan con un tamaño mínimo para poder pasar el mouse
            // o hacer clic sobre ellas. Las cifras que se muestran (unidades y porcentaje) son siempre las reales.
            double pesoVisual = Math.max(unidades, totalUnidades * minimoVisual);

            PieChart.Data dato = new PieChart.Data(nombre, pesoVisual);
            datos.add(dato);
            porciones.add(new Porcion(dato, nombre, unidades, porcentaje, colorPara(i)));
        }
        graficaProductos.setData(FXCollections.observableArrayList(datos));

        for (Porcion porcion : porciones) {
            leyendaProductos.getChildren().add(crearItemLeyenda(porcion));
            decorarPorcion(porcion);
        }

        mostrarSeleccionOTotal();
    }

    /** Los primeros productos usan la paleta de la marca; los demás, colores generados y distintos entre sí. */
    private String colorPara(int indice) {
        if (indice < COLORES_BASE.length) {
            return COLORES_BASE[indice];
        }
        Color c = Color.hsb((indice * 137.508) % 360, 0.60, 0.85);
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    /** Da color, mensaje emergente y reacción al mouse (pasar y clic) a una porción de la dona. */
    private void decorarPorcion(Porcion porcion) {
        Runnable aplicar = () -> {
            Node nodo = porcion.data.getNode();
            nodo.setStyle("-fx-pie-color: " + porcion.color + ";");
            nodo.setCursor(Cursor.HAND);
            Tooltip.install(nodo, new Tooltip(porcion.nombre + "\n" + porcion.unidades
                    + " unidades (" + formatearPorcentaje(porcion.porcentaje) + ")"));

            nodo.setOnMouseEntered(e -> entrar(porcion));
            nodo.setOnMouseExited(e -> salir(porcion));
            nodo.setOnMouseClicked(e -> alternarSeleccion(porcion));
        };

        if (porcion.data.getNode() != null) {
            aplicar.run();
        } else {
            porcion.data.nodeProperty().addListener((obs, anterior, nuevo) -> {
                if (nuevo != null) {
                    aplicar.run();
                }
            });
        }
    }

    /** Porcentaje con una decimal y coma decimal (ej. 0,5%). Nunca muestra 0% si sí hay existencias. */
    static String formatearPorcentaje(double porcentaje) {
        if (porcentaje > 0 && porcentaje < 0.1) {
            return "<0,1%";
        }
        return String.format(java.util.Locale.ROOT, "%.1f", porcentaje).replace('.', ',') + "%";
    }

    private void entrar(Porcion porcion) {
        resaltar(porcion.data.getNode(), true);
        mostrarEnCentro(porcion.nombre, porcion.unidades, porcion.porcentaje);
    }

    private void salir(Porcion porcion) {
        if (seleccionada != porcion) {
            resaltar(porcion.data.getNode(), false);
        }
        mostrarSeleccionOTotal();
    }

    private void alternarSeleccion(Porcion porcion) {
        if (seleccionada == porcion) {
            resaltar(porcion.data.getNode(), false);
            seleccionada = null;
        } else {
            if (seleccionada != null) {
                resaltar(seleccionada.data.getNode(), false);
            }
            seleccionada = porcion;
            resaltar(porcion.data.getNode(), true);
        }
        mostrarSeleccionOTotal();
    }

    private void resaltar(Node nodo, boolean activo) {
        if (nodo == null) return;
        nodo.setScaleX(activo ? 1.06 : 1.0);
        nodo.setScaleY(activo ? 1.06 : 1.0);
        nodo.setEffect(activo ? new DropShadow(10, Color.rgb(0, 0, 0, 0.35)) : null);
    }

    private void mostrarEnCentro(String nombre, int unidades, double porcentaje) {
        lblDonaTitulo.setText(nombre);
        lblDonaValor.setText(formatearPorcentaje(porcentaje));
        lblDonaDetalle.setText(unidades + " unidades");
    }

    private void mostrarSeleccionOTotal() {
        if (seleccionada != null) {
            mostrarEnCentro(seleccionada.nombre, seleccionada.unidades, seleccionada.porcentaje);
        } else if (totalUnidades == 0) {
            lblDonaTitulo.setText("Sin stock");
            lblDonaValor.setText("-");
            lblDonaDetalle.setText("");
        } else {
            lblDonaTitulo.setText("Stock total");
            lblDonaValor.setText(String.valueOf(totalUnidades));
            lblDonaDetalle.setText("unidades");
        }
    }

    /** Fila de la leyenda: también reacciona al mouse y al clic, útil para porciones muy pequeñas. */
    private HBox crearItemLeyenda(Porcion porcion) {
        Circle punto = new Circle(5, Color.web(porcion.color));
        Label texto = new Label(porcion.nombre + " · " + formatearPorcentaje(porcion.porcentaje) + " (" + porcion.unidades + " u.)");
        texto.getStyleClass().add("leyenda-texto");
        HBox fila = new HBox(8, punto, texto);
        fila.getStyleClass().add("leyenda-fila");
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setCursor(Cursor.HAND);
        fila.setOnMouseEntered(e -> entrar(porcion));
        fila.setOnMouseExited(e -> salir(porcion));
        fila.setOnMouseClicked(e -> alternarSeleccion(porcion));
        return fila;
    }
}
