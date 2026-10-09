package sgis.vista;

import sgis.controlador.ClienteController;
import sgis.modelo.Cliente;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Configuración > Clientes: buscar, modificar y eliminar a las personas registradas
 * como clientes (las que compran o piden factura).
 */
public class ConfigClientesVista implements Initializable {

    private final ClienteController clienteController = new ClienteController();
    private FilteredList<Cliente> clientesFiltrados;

    @FXML private TextField campoBusqueda;
    @FXML private Label lblResultado;
    @FXML private Label lblMensaje;
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colId;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, Cliente> colAcciones;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(param -> new SimpleStringProperty(String.valueOf(param.getValue().getIdCliente())));
        colNombre.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getNombre()));
        colCorreo.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getCorreo()));

        colAcciones.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("✏ Modificar");
            private final Button btnEliminar = new Button("🗑 Eliminar");
            private final HBox contenedor = new HBox(6, btnEditar, btnEliminar);

            @Override
            protected void updateItem(Cliente cliente, boolean vacio) {
                super.updateItem(cliente, vacio);
                if (vacio || cliente == null) {
                    setGraphic(null);
                    return;
                }
                btnEditar.setOnAction(e -> abrirFormulario(cliente));
                btnEliminar.setOnAction(e -> confirmarEliminar(cliente));
                setGraphic(contenedor);
            }
        });

        cargarClientes();
        campoBusqueda.textProperty().addListener((obs, anterior, nuevo) -> filtrar(nuevo));
    }

    private void cargarClientes() {
        try {
            List<Cliente> clientes = clienteController.obtenerClientes();
            clientesFiltrados = new FilteredList<>(FXCollections.observableArrayList(clientes), c -> true);
            tablaClientes.setItems(clientesFiltrados);
            filtrar(campoBusqueda.getText());
        } catch (RuntimeException e) {
            mostrarError("Error al cargar los clientes: " + e.getMessage());
        }
    }

    private void filtrar(String texto) {
        if (clientesFiltrados == null) return;

        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        clientesFiltrados.setPredicate(c ->
                buscado.isEmpty()
                || c.getNombre().toLowerCase().contains(buscado)
                || c.getCorreo().toLowerCase().contains(buscado));

        if (buscado.isEmpty()) {
            lblResultado.setText("");
        } else if (clientesFiltrados.isEmpty()) {
            lblResultado.setText("✘ No está registrado.");
        } else {
            lblResultado.setText("✔ " + clientesFiltrados.size() + " resultado(s).");
        }
    }

    private void confirmarEliminar(Cliente cliente) {
        if (Dialogos.confirmar("¿Eliminar definitivamente a \"" + cliente.getNombre()
                + "\"? Esta acción no se puede deshacer.")) {
            String error = clienteController.eliminarCliente(cliente);
            if (error == null) {
                lblMensaje.setStyle("");
                lblMensaje.setText("✔ Cliente eliminado.");
                cargarClientes();
            } else {
                mostrarError(error);
            }
        }
    }

    private void abrirFormulario(Cliente cliente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/usuarioForm.fxml"));
            Parent root = loader.load();
            UsuarioFormVista controlador = loader.getController();
            controlador.configurar(cliente, null, () -> {
                lblMensaje.setStyle("");
                lblMensaje.setText("✔ Datos guardados correctamente.");
                cargarClientes();
            });

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.setTitle("Modificar cliente");
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
