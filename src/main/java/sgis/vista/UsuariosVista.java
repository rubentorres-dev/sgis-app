package sgis.vista;

import sgis.controlador.ClienteController;
import sgis.modelo.Cliente;
import sgis.util.Permisos;
import sgis.util.Sesion;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla "Usuarios" (usuarios.fxml).
 *
 * Esta pantalla muestra a las personas registradas como clientes (quienes compran o
 * necesitan facturación). Permite buscar si una persona ya está registrada, registrar
 * una nueva y modificar sus datos. Los clientes también se registran solos al hacer
 * una venta desde la pantalla Ventas.
 */
public class UsuariosVista implements Initializable {

    private final ClienteController clienteController = new ClienteController();
    private FilteredList<Cliente> clientesFiltrados;

    @FXML private TextField campoBusqueda;
    @FXML private Label lblResultado;
    @FXML private Label lblMensaje;
    @FXML private TableView<Cliente> tablaUsuarios;
    @FXML private TableColumn<Cliente, String> colId;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, Cliente> colAcciones;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Solo el Administrador puede editar y borrar; el Administrador de inventario solo consulta y crea.
        colAcciones.setVisible(Permisos.puedeEditarYBorrar(Sesion.getUsuarioActual()));

        colId.setCellValueFactory(param -> new SimpleStringProperty(String.valueOf(param.getValue().getIdCliente())));
        colNombre.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getNombre()));
        colCorreo.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getCorreo()));

        colAcciones.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("✏ Modificar");
            private final HBox contenedor = new HBox(btnEditar);

            @Override
            protected void updateItem(Cliente cliente, boolean vacio) {
                super.updateItem(cliente, vacio);
                if (vacio || cliente == null) {
                    setGraphic(null);
                    return;
                }
                btnEditar.setOnAction(e -> abrirFormulario(cliente));
                setGraphic(contenedor);
            }
        });

        cargarClientes();
        campoBusqueda.textProperty().addListener((obs, anterior, nuevo) -> filtrar(nuevo));
    }

    private void cargarClientes() {
        try {
            List<Cliente> clientes = clienteController.obtenerClientes();
            ObservableList<Cliente> datos = FXCollections.observableArrayList(clientes);
            clientesFiltrados = new FilteredList<>(datos, c -> true);
            tablaUsuarios.setItems(clientesFiltrados);
            filtrar(campoBusqueda.getText());
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar los usuarios: " + e.getMessage());
        }
    }

    /** Filtra la tabla en vivo por nombre o correo y avisa si la persona está o no registrada. */
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
            lblResultado.setText("✘ No está registrado. Puedes registrarlo con el botón de la derecha.");
        } else {
            lblResultado.setText("✔ " + clientesFiltrados.size() + " resultado(s).");
        }
    }

    @FXML
    private void abrirFormularioNuevo(ActionEvent event) {
        abrirFormulario(null);
    }

    private void abrirFormulario(Cliente clienteAEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/usuarioForm.fxml"));
            Parent root = loader.load();
            UsuarioFormVista controlador = loader.getController();

            // Si se está buscando a alguien que no existe, se precarga el texto buscado
            // como nombre para ahorrar escritura.
            String sugerencia = (clienteAEditar == null && clientesFiltrados != null && clientesFiltrados.isEmpty())
                    ? campoBusqueda.getText() : null;
            controlador.configurar(clienteAEditar, sugerencia, () -> {
                lblMensaje.setText("✔ Datos guardados correctamente.");
                cargarClientes();
            });

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.setTitle(clienteAEditar == null ? "Registrar usuario" : "Modificar usuario");
            dialogo.setScene(new Scene(root));
            dialogo.showAndWait();
        } catch (Exception e) {
            lblMensaje.setText("No se pudo abrir el formulario: " + e.getMessage());
        }
    }






}
