package sgis.vista;

import sgis.controlador.UsuarioController;
import sgis.modelo.Usuario;
import sgis.modelo.Rol;
import sgis.util.Permisos;
import sgis.util.Sesion;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Formulario para crear o editar un usuario del sistema (usuarioSistemaForm.fxml).
 * Se usa desde la pantalla Configuración y desde el login (en este último caso
 * exige la autorización de un administrador).
 */
public class UsuarioSistemaFormVista implements Initializable {

    /** Última opción de la lista de roles: al elegirla se pide el nombre y se crea el rol. */
    private static final String NUEVO_ROL = "➕ Nuevo rol";

    private final UsuarioController usuarioController = new UsuarioController();

    @FXML private Label lblTitulo;
    @FXML private TextField campoNombre;
    @FXML private TextField campoLogin;
    @FXML private ComboBox<String> comboRol;
    @FXML private VBox boxAccesoRol;
    @FXML private CheckBox chkAccPanel;
    @FXML private CheckBox chkAccProductos;
    @FXML private CheckBox chkAccVentas;
    @FXML private CheckBox chkAccReportes;
    @FXML private CheckBox chkAccUsuarios;
    @FXML private CheckBox chkAccConfiguracion;
    @FXML private CheckBox chkAccEditar;
    @FXML private Label lblContrasena;
    @FXML private Label lblConfirmar;
    @FXML private CampoContrasena campoContrasena;
    @FXML private CampoContrasena campoConfirmar;
    @FXML private VBox boxAutorizacion;
    @FXML private TextField campoAdminUsuario;
    @FXML private CampoContrasena campoAdminContrasena;
    @FXML private Label lblMensaje;

    private Usuario usuarioAEditar; // null si es un usuario nuevo
    private String rolActualDelUsuario; // su rol al abrir el formulario (puede estar desactivado)
    private boolean requiereAutorizacion;
    private Runnable alGuardar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Las casillas solo muestran el acceso del rol elegido: no se pueden marcar aquí, porque cambiar
        // un rol afectaría a todos los usuarios que lo tienen (eso se hace en Configuración > Roles).
        for (CheckBox casilla : Arrays.asList(chkAccPanel, chkAccProductos, chkAccVentas, chkAccReportes,
                chkAccUsuarios, chkAccConfiguracion, chkAccEditar)) {
            casilla.setMouseTransparent(true);
            casilla.setFocusTraversable(false);
        }

        comboRol.valueProperty().addListener((obs, anterior, nuevo) -> {
            if (NUEVO_ROL.equals(nuevo)) {
                // Se espera a que termine el evento de selección antes de abrir el cuadro.
                Platform.runLater(() -> crearRolDesdeLista(anterior));
            } else {
                mostrarAccesoDelRol(nuevo);
            }
        });
    }

    /**
     * Debe llamarse justo después de cargar el FXML, antes de mostrar la ventana.
     * @param usuario el usuario a editar, o null si se va a crear uno nuevo.
     * @param requiereAutorizacion true si hay que pedir las credenciales de un administrador (login).
     * @param alGuardar acción a ejecutar tras guardar (ej. refrescar la tabla).
     */
    public void configurar(Usuario usuario, boolean requiereAutorizacion, Runnable alGuardar) {
        this.usuarioAEditar = usuario;
        this.rolActualDelUsuario = usuario == null ? null : usuario.getRol();
        this.requiereAutorizacion = requiereAutorizacion;
        this.alGuardar = alGuardar;

        // La opción "Nuevo rol" no se ofrece desde el inicio de sesión (todavía no hay una sesión iniciada).
        comboRol.setItems(FXCollections.observableArrayList(construirOpcionesDeRol()));

        // Desde el inicio de sesión el formulario se mantiene compacto: sin el panel de acceso del rol.
        boxAccesoRol.setVisible(!requiereAutorizacion);
        boxAccesoRol.setManaged(!requiereAutorizacion);

        boxAutorizacion.setVisible(requiereAutorizacion);
        boxAutorizacion.setManaged(requiereAutorizacion);

        if (usuario != null) {
            lblTitulo.setText("Editar usuario");
            campoNombre.setText(usuario.getNombre());
            campoLogin.setText(usuario.getUsuarioLogin());
            campoLogin.setDisable(true); // el nombre de usuario no se cambia
            comboRol.setValue(usuario.getRol());
            // Al editar, los dos campos de contraseña cambian de sentido: primero se pega la clave temporal
            // (la que generó el botón Clave) y luego se escribe la contraseña nueva. Vacíos = no cambiarla.
            lblContrasena.setText("Clave temporal (pégala aquí solo si vas a cambiar la contraseña)");
            campoContrasena.setPromptText("Pega la clave temporal");
            lblConfirmar.setText("Contraseña nueva (mínimo 6 caracteres)");
            campoConfirmar.setPromptText("Escribe la contraseña nueva");
        } else {
            lblTitulo.setText("Nuevo usuario");
            // Por defecto, el rol de menos permisos que exista (o el primero de la lista).
            comboRol.setValue(comboRol.getItems().contains(Permisos.ADMIN_INVENTARIO)
                    ? Permisos.ADMIN_INVENTARIO : comboRol.getItems().get(0));
        }
    }

    @FXML
    private void guardar() {
        String nombre = campoNombre.getText();
        String rol = comboRol.getValue();
        String contrasena = campoContrasena.getText();
        String confirmar = campoConfirmar.getText();

        boolean escribioClave = contrasena != null && !contrasena.isEmpty();
        boolean escribioNueva = confirmar != null && !confirmar.isEmpty();
        if (usuarioAEditar == null) {
            if (!String.valueOf(contrasena).equals(confirmar)) {
                mostrarError("Las contraseñas no coinciden.");
                return;
            }
        } else if (escribioClave != escribioNueva) {
            mostrarError("Para cambiar la contraseña escribe la clave temporal y la contraseña nueva (o deja las dos vacías).");
            return;
        }

        if (requiereAutorizacion) {
            String errorAutorizacion = usuarioController.autorizarAdministrativo(
                    campoAdminUsuario.getText(), campoAdminContrasena.getText());
            if (errorAutorizacion != null) {
                mostrarError(errorAutorizacion);
                return;
            }
        }

        String error;
        if (usuarioAEditar == null) {
            error = usuarioController.registrarUsuario(nombre, rol, campoLogin.getText(), contrasena);
        } else {
            Usuario editado = new Usuario();
            editado.setIdUsuario(usuarioAEditar.getIdUsuario());
            editado.setNombre(nombre);
            editado.setRol(rol);
            error = usuarioController.actualizarUsuario(editado, Sesion.getUsuarioActual());
            if (error == null && escribioClave) {
                error = usuarioController.cambiarContrasenaConClaveTemporal(usuarioAEditar, contrasena, confirmar);
            }
        }

        if (error == null) {
            if (alGuardar != null) {
                alGuardar.run();
            }
            cerrarVentana();
        } else {
            mostrarError(error);
        }
    }

    /** Los roles que existen y, al final, la opción "Nuevo rol". */
    private List<String> construirOpcionesDeRol() {
        List<String> opciones = new ArrayList<>(Arrays.asList(Permisos.roles()));
        // Si el rol del usuario fue desactivado, se sigue mostrando para no cambiárselo sin querer.
        if (rolActualDelUsuario != null && !opciones.contains(rolActualDelUsuario)) {
            opciones.add(rolActualDelUsuario);
        }
        if (!requiereAutorizacion) {
            opciones.add(NUEVO_ROL);
        }
        return opciones;
    }

    /**
     * Abre el cuadro para crear el rol (nombre y casillas de acceso) y lo deja seleccionado.
     * Si se cancela, la lista vuelve a la opción anterior.
     */
    private void crearRolDesdeLista(String seleccionAnterior) {
        String[] creado = new String[1];
        RolFormVista.abrir(null, nombre -> creado[0] = nombre);

        if (creado[0] == null) {
            comboRol.setValue(seleccionAnterior);
            return;
        }
        comboRol.setItems(FXCollections.observableArrayList(construirOpcionesDeRol()));
        comboRol.setValue(creado[0]);
        lblMensaje.setStyle("-fx-text-fill: #1E9E5B;");
        lblMensaje.setText("✔ Rol \"" + creado[0] + "\" creado.");
    }

    /** Marca las casillas con el acceso del rol elegido. */
    private void mostrarAccesoDelRol(String nombreRol) {
        Rol rol = nombreRol == null ? null : Permisos.rolDe(nombreRol);
        boolean administrador = Permisos.esAdministrativo(nombreRol);

        chkAccPanel.setSelected(rol != null || administrador);
        chkAccProductos.setSelected(administrador || (rol != null && rol.permite("PRODUCTOS")));
        chkAccVentas.setSelected(administrador || (rol != null && rol.permite("VENTAS")));
        chkAccReportes.setSelected(administrador || (rol != null && rol.permite("REPORTES")));
        chkAccUsuarios.setSelected(administrador || (rol != null && rol.permite("USUARIOS")));
        chkAccConfiguracion.setSelected(administrador);
        chkAccEditar.setSelected(administrador || (rol != null && rol.isPuedeEditar()));
    }

    @FXML
    private void cancelar() {
        cerrarVentana();
    }

    private void mostrarError(String texto) {
        lblMensaje.setStyle("-fx-text-fill: #C0392B;");
        lblMensaje.setText("✘ " + texto);
    }

    private void cerrarVentana() {
        Stage stage = (Stage) lblTitulo.getScene().getWindow();
        stage.close();
    }
}
