package sgis.vista;

import sgis.controlador.RolController;
import sgis.controlador.UsuarioController;
import sgis.modelo.Rol;
import sgis.modelo.Usuario;
import sgis.util.Permisos;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Configuración > Roles: ver el acceso de cada rol y editarlo, desactivarlo o eliminarlo.
 * Solo accesible para el Administrador.
 *
 * El rol Administrador es del sistema: se puede consultar, pero no se desactiva ni se elimina,
 * porque sin él nadie podría administrar la aplicación.
 */
public class ConfigRolesVista implements Initializable {

    private final RolController rolController = new RolController();
    private final UsuarioController usuarioController = new UsuarioController();
    private final Map<String, Integer> usuariosPorRol = new HashMap<>();

    /** true cuando falta la tabla de roles: se muestran los roles predeterminados solo para consulta. */
    private boolean soloLectura = false;

    @FXML private Label lblMensaje;
    @FXML private Button btnNuevoRol;
    @FXML private TableView<Rol> tablaRoles;
    @FXML private TableColumn<Rol, String> colNombre;
    @FXML private TableColumn<Rol, String> colPantallas;
    @FXML private TableColumn<Rol, String> colEditar;
    @FXML private TableColumn<Rol, String> colUsuarios;
    @FXML private TableColumn<Rol, Rol> colEstado;
    @FXML private TableColumn<Rol, Rol> colAcciones;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colNombre.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getNombre() + (p.getValue().isEsSistema() ? "  (del sistema)" : "")));
        colPantallas.setCellValueFactory(p -> new SimpleStringProperty(Permisos.pantallasAmigables(p.getValue())));
        colEditar.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().isPuedeEditar() ? "Sí" : "No"));
        colUsuarios.setCellValueFactory(p -> new SimpleStringProperty(
                String.valueOf(usuariosPorRol.getOrDefault(p.getValue().getNombre(), 0))));

        colEstado.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Rol rol, boolean vacio) {
                super.updateItem(rol, vacio);
                if (vacio || rol == null) {
                    setGraphic(null);
                    return;
                }
                Label insignia = new Label(rol.isActivo() ? "Activo" : "Inactivo");
                insignia.getStyleClass().add(rol.isActivo() ? "badge-suficiente" : "badge-agotado");
                setGraphic(insignia);
            }
        });

        colAcciones.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("✏ Editar");
            private final Button btnEstado = new Button();
            private final Button btnEliminar = new Button("🗑 Eliminar");
            private final HBox contenedor = new HBox(6, btnEditar, btnEstado, btnEliminar);
            private final Label sinTabla = new Label("Falta la tabla de roles");

            @Override
            protected void updateItem(Rol rol, boolean vacio) {
                super.updateItem(rol, vacio);
                if (vacio || rol == null) {
                    setGraphic(null);
                    return;
                }
                if (soloLectura) {
                    sinTabla.setStyle("-fx-text-fill: #8A6D00;");
                    setGraphic(sinTabla);
                    return;
                }
                btnEstado.setText(rol.isActivo() ? "Desactivar" : "Activar");
                btnEditar.setOnAction(e -> RolFormVista.abrir(rol, nombre -> guardado("Rol actualizado.")));
                btnEstado.setOnAction(e -> cambiarEstado(rol));
                btnEliminar.setOnAction(e -> confirmarEliminar(rol));
                setGraphic(contenedor);
            }
        });

        cargarRoles();
    }

    private void cargarRoles() {
        usuariosPorRol.clear();
        try {
            for (Usuario u : usuarioController.obtenerUsuarios()) {
                usuariosPorRol.merge(u.getRol(), 1, Integer::sum);
            }
        } catch (RuntimeException e) {
            // sin la cuenta de usuarios por rol, la tabla igual se muestra
        }

        try {
            List<Rol> roles = rolController.obtenerRoles();
            tablaRoles.setItems(FXCollections.observableArrayList(roles));
            soloLectura = false;
            btnNuevoRol.setDisable(false);
        } catch (RuntimeException e) {
            // Si no se pueden leer los roles (por ejemplo, falta la tabla), la lista no queda vacía:
            // se muestran los dos roles predeterminados y debajo el aviso de qué hay que hacer.
            soloLectura = true;
            btnNuevoRol.setDisable(true);
            tablaRoles.setItems(FXCollections.observableArrayList(Permisos.listarRoles()));
            lblMensaje.setStyle("-fx-text-fill: #8A6D00;");
            lblMensaje.setText("⚠ " + e.getMessage()
                    + " Después vuelve a abrir esta pestaña. Mientras tanto se muestran los dos roles predeterminados.");
        }
        tablaRoles.refresh();
    }

    @FXML
    private void nuevoRol(ActionEvent event) {
        RolFormVista.abrir(null, nombre -> guardado("Rol \"" + nombre + "\" creado."));
    }

    private void cambiarEstado(Rol rol) {
        if (rol.isEsSistema()) {
            Dialogos.informar("El rol " + rol.getNombre() + " no se puede desactivar: sin él nadie podría "
                    + "administrar el sistema.");
            return;
        }

        boolean activar = !rol.isActivo();
        if (!activar) {
            int usuarios = usuariosPorRol.getOrDefault(rol.getNombre(), 0);
            String aviso = usuarios > 0
                    ? "\n\n" + usuarios + " usuario(s) tienen este rol y no podrán iniciar sesión mientras esté desactivado."
                    : "";
            if (!Dialogos.confirmar("¿Desactivar el rol \"" + rol.getNombre() + "\"? "
                    + "Ya no se podrá asignar a usuarios nuevos." + aviso)) {
                return;
            }
        }

        String error = rolController.cambiarEstado(rol, activar);
        if (error == null) {
            guardado(activar ? "Rol activado." : "Rol desactivado.");
        } else {
            mostrarError(error);
        }
    }

    private void confirmarEliminar(Rol rol) {
        if (rol.isEsSistema()) {
            Dialogos.informar("El rol " + rol.getNombre() + " no se puede eliminar: sin él nadie podría "
                    + "administrar el sistema.");
            return;
        }
        if (Dialogos.confirmar("¿Eliminar el rol \"" + rol.getNombre() + "\"?")) {
            String error = rolController.eliminarRol(rol);
            if (error == null) {
                guardado("Rol eliminado.");
            } else {
                mostrarError(error);
            }
        }
    }

    private void guardado(String mensaje) {
        lblMensaje.setStyle("");
        lblMensaje.setText("✔ " + mensaje);
        cargarRoles();
    }

    private void mostrarError(String texto) {
        lblMensaje.setStyle("-fx-text-fill: #C0392B;");
        lblMensaje.setText("✘ " + texto);
    }
}
