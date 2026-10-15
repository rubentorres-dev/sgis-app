package sgis.vista;

import sgis.controlador.ClienteController;
import sgis.modelo.Cliente;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador del formulario para registrar o modificar a una persona
 * (usuarioForm.fxml). Se abre como diálogo modal desde UsuariosVista.
 */
public class UsuarioFormVista implements Initializable {

    private final ClienteController clienteController = new ClienteController();

    @FXML private Label lblTitulo;
    @FXML private TextField campoNombre;
    @FXML private TextField campoCorreo;
    @FXML private CampoContrasena campoCedula;
    @FXML private TextField campoTelefono;
    @FXML private Label lblMensaje;

    private Cliente clienteAEditar; // null si es una persona nueva
    private Runnable alGuardar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // La cédula no pasa de 10 dígitos y el teléfono tampoco: el siguiente número ya no se escribe.
        campoCedula.soloNumeros(10);
        Campos.soloNumeros(campoTelefono, 10, false);
    }

    /**
     * Debe llamarse justo después de cargar el FXML, antes de mostrar la ventana.
     * @param cliente   la persona a modificar, o null si se va a registrar una nueva.
     * @param sugerenciaNombre texto para precargar el nombre (ej. lo que se buscó), o null.
     * @param alGuardar acción a ejecutar tras guardar (ej. refrescar la tabla).
     */
    public void configurar(Cliente cliente, String sugerenciaNombre, Runnable alGuardar) {
        this.clienteAEditar = cliente;
        this.alGuardar = alGuardar;

        if (cliente != null) {
            lblTitulo.setText("Modificar cliente");
            campoNombre.setText(cliente.getNombre());
            campoCorreo.setText(cliente.getCorreo());
            campoCedula.setText(cliente.getCedula() == null ? "" : cliente.getCedula());
            campoTelefono.setText(cliente.getTelefono() == null ? "" : cliente.getTelefono());
        } else if (sugerenciaNombre != null && !sugerenciaNombre.isBlank()) {
            // Si lo que se buscó parece un correo, va al campo de correo; si no, al de nombre.
            if (sugerenciaNombre.contains("@")) {
                campoCorreo.setText(sugerenciaNombre.trim());
            } else {
                campoNombre.setText(sugerenciaNombre.trim());
            }
        }
    }

    @FXML
    private void guardar() {
        String nombre = campoNombre.getText();
        String correo = campoCorreo.getText();
        String cedula = campoCedula.getText();
        String telefono = campoTelefono.getText();

        String error;
        if (clienteAEditar == null) {
            error = clienteController.registrarCliente(nombre, correo, cedula, telefono);
        } else {
            error = clienteController.actualizarCliente(clienteAEditar, nombre, correo, cedula, telefono);
        }

        if (error == null) {
            if (alGuardar != null) alGuardar.run();
            cerrarVentana();
        } else {
            lblMensaje.setText("✘ " + error);
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
