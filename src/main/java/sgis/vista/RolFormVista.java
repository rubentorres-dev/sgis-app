package sgis.vista;

import sgis.controlador.RolController;
import sgis.modelo.Rol;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Cuadro para crear o editar un rol (rolForm.fxml): nombre, descripción, a qué pantallas puede
 * entrar y si puede editar y borrar.
 */
public class RolFormVista {

    private final RolController rolController = new RolController();

    @FXML private Label lblTitulo;
    @FXML private TextField campoNombre;
    @FXML private CheckBox chkProductos;
    @FXML private CheckBox chkVentas;
    @FXML private CheckBox chkReportes;
    @FXML private CheckBox chkUsuarios;
    @FXML private CheckBox chkEditar;
    @FXML private Label lblMensaje;
    @FXML private VBox boxNotaSistema;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private Rol rolAEditar; // null si es un rol nuevo
    private Consumer<String> alGuardar;

    /**
     * Abre el cuadro de rol (lo usan la pestaña Roles y el formulario de usuarios).
     * @param rolAEditar el rol a editar, o null para crear uno nuevo.
     * @param alGuardar recibe el nombre del rol guardado (para refrescar listas o seleccionarlo).
     */
    public static void abrir(Rol rolAEditar, Consumer<String> alGuardar) {
        try {
            FXMLLoader loader = new FXMLLoader(RolFormVista.class.getResource("/fxml/rolForm.fxml"));
            Parent root = loader.load();
            RolFormVista controlador = loader.getController();
            controlador.configurar(rolAEditar, alGuardar);

            Stage ventana = new Stage();
            ventana.initModality(Modality.APPLICATION_MODAL);
            ventana.setTitle(rolAEditar == null ? "Nuevo rol" : "Editar rol");
            ventana.setScene(new Scene(root));
            ventana.showAndWait();
        } catch (Exception e) {
            Dialogos.informar("No se pudo abrir el formulario de roles: " + e.getMessage());
        }
    }

    /** Se llama justo después de cargar el FXML. */
    public void configurar(Rol rol, Consumer<String> alGuardar) {
        this.rolAEditar = rol;
        this.alGuardar = alGuardar;

        if (rol != null) {
            lblTitulo.setText("Editar rol");
            campoNombre.setText(rol.getNombre());
            campoNombre.setDisable(true); // el nombre de un rol no se cambia
            chkProductos.setSelected(rol.permite("PRODUCTOS"));
            chkVentas.setSelected(rol.permite("VENTAS"));
            chkReportes.setSelected(rol.permite("REPORTES"));
            chkUsuarios.setSelected(rol.permite("USUARIOS"));
            chkEditar.setSelected(rol.isPuedeEditar());

            if (rol.isEsSistema()) {
                // Rol Administrador: solo se muestra. Tiene acceso total y no se puede cambiar.
                lblTitulo.setText("Rol " + rol.getNombre());
                chkProductos.setSelected(true);
                chkVentas.setSelected(true);
                chkReportes.setSelected(true);
                chkUsuarios.setSelected(true);
                chkEditar.setSelected(true);
                for (CheckBox casilla : new CheckBox[]{chkProductos, chkVentas, chkReportes, chkUsuarios, chkEditar}) {
                    casilla.setDisable(true);
                }
                boxNotaSistema.setVisible(true);
                boxNotaSistema.setManaged(true);
                btnGuardar.setVisible(false);
                btnGuardar.setManaged(false);
                btnCancelar.setText("Cerrar");
            }
        }
    }

    @FXML
    private void guardar() {
        Set<String> pantallas = new LinkedHashSet<>();
        if (chkProductos.isSelected()) pantallas.add("PRODUCTOS");
        if (chkVentas.isSelected()) pantallas.add("VENTAS");
        if (chkReportes.isSelected()) pantallas.add("REPORTES");
        if (chkUsuarios.isSelected()) pantallas.add("USUARIOS");

        String nombre = campoNombre.getText();
        // La descripción ya no se pide: al crear queda vacía y al editar se conserva la que tenía.
        String descripcion = rolAEditar == null ? null : rolAEditar.getDescripcion();
        boolean puedeEditar = chkEditar.isSelected();

        String error = rolAEditar == null
                ? rolController.crearRol(nombre, descripcion, pantallas, puedeEditar)
                : rolController.actualizarRol(rolAEditar, descripcion, pantallas, puedeEditar);

        if (error == null) {
            if (alGuardar != null) {
                alGuardar.accept(nombre.trim());
            }
            cerrarVentana();
        } else {
            lblMensaje.setStyle("-fx-text-fill: #C0392B;");
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
