package sgis.vista;

import sgis.controlador.UsuarioController;
import sgis.modelo.Usuario;
import sgis.util.PasswordUtil;
import sgis.util.Permisos;
import sgis.util.Sesion;
import sgis.util.Telefono;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;

/**
 * Configuración > Usuarios del sistema: crear, buscar, editar (rol y contraseña),
 * activar/desactivar y eliminar las cuentas con las que se entra a la aplicación.
 * Solo es accesible para el Administrador.
 */
public class ConfigUsuariosVista implements Initializable {

    private final UsuarioController usuarioController = new UsuarioController();
    private FilteredList<Usuario> usuariosFiltrados;

    /** Contraseñas que el Administrador tiene a la vista ahora mismo (id de usuario → texto). */
    private final Map<Integer, String> reveladas = new HashMap<>();
    private final Image imagenOjo = new Image(getClass().getResource("/images/iconos/ojo.png").toExternalForm());
    private final Image imagenOjoTachado = new Image(getClass().getResource("/images/iconos/ojo_tachado.png").toExternalForm());

    /** Códigos de recuperación que el administrador principal tiene a la vista ahora mismo (id de usuario → código). */
    private final Map<Integer, String> codigosRevelados = new HashMap<>();
    /** Ids de los usuarios que ya tienen un código de recuperación. */
    private Set<Integer> conCodigo = new HashSet<>();
    /** true si quien tiene la sesión abierta es el administrador principal (el único que genera códigos nuevos). */
    private boolean esPrincipal;
    /** Cédula y teléfono de cada usuario (id → {cédula, teléfono}). */
    private Map<Integer, String[]> contactos = new HashMap<>();
    /** Cédulas que el Administrador tiene a la vista ahora mismo (id de usuario → cédula con puntos). */
    private final Map<Integer, String> cedulasReveladas = new HashMap<>();

    @FXML private TextField campoBusqueda;
    @FXML private Label lblResultado;
    @FXML private Label lblMensaje;
    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colLogin;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, Usuario> colContrasena;
    @FXML private TableColumn<Usuario, Usuario> colCodigo;
    @FXML private TableColumn<Usuario, Usuario> colCedula;
    @FXML private TableColumn<Usuario, String> colTelefono;
    @FXML private TableColumn<Usuario, Usuario> colEstado;
    @FXML private TableColumn<Usuario, Usuario> colAcciones;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colNombre.setCellValueFactory(param -> new SimpleStringProperty(
                param.getValue().getNombre() + (param.getValue().isProtegido() ? "  🔒" : "")));
        colLogin.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getUsuarioLogin()));
        colRol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getRol()));
        // Contraseña: oculta con puntos; el ojo la muestra u oculta (solo el Administrador puede verlas).
        colContrasena.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colContrasena.setCellFactory(col -> new TableCell<>() {
            private final Label texto = new Label();
            private final ImageView icono = new ImageView();
            private final Button ojo = new Button();
            private final HBox caja = new HBox(6, texto, ojo);

            {
                icono.setFitWidth(18);
                icono.setFitHeight(18);
                icono.setPreserveRatio(true);
                ojo.setGraphic(icono);
                ojo.setStyle("-fx-background-color: transparent; -fx-padding: 2 4 2 4; -fx-cursor: hand;");
                caja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                if (vacio || u == null) {
                    setGraphic(null);
                    return;
                }
                boolean visible = reveladas.containsKey(u.getIdUsuario());
                texto.setText(visible ? reveladas.get(u.getIdUsuario()) : "••••••••");
                texto.setStyle(visible ? "-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-weight: bold;" : "");
                icono.setImage(visible ? imagenOjoTachado : imagenOjo);
                ojo.setOnAction(e -> alternarContrasena(u));
                // La contraseña del usuario protegido no se puede ver: sin ojo.
                ojo.setVisible(!u.isProtegido());
                ojo.setManaged(!u.isProtegido());
                setGraphic(caja);
            }
        });

        // Cédula: oculta con puntos; el ojito la muestra (solo Administradores) y la consulta queda en la auditoría.
        // La del usuario protegido tiene candado: nadie puede verla. El teléfono sí se ve.
        colCedula.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colCedula.setCellFactory(col -> new TableCell<>() {
            private final Label texto = new Label();
            private final ImageView icono = new ImageView();
            private final Button ojo = new Button();
            private final Label candado = new Label("🔒");
            private final HBox caja = new HBox(6, texto, ojo, candado);

            {
                icono.setFitWidth(18);
                icono.setFitHeight(18);
                icono.setPreserveRatio(true);
                ojo.setGraphic(icono);
                ojo.setStyle("-fx-background-color: transparent; -fx-padding: 2 4 2 4; -fx-cursor: hand;");
                caja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                if (vacio || u == null) {
                    setGraphic(null);
                    return;
                }
                String[] contacto = contactos.get(u.getIdUsuario());
                String cedula = contacto == null ? null : contacto[0];
                boolean tiene = cedula != null && !cedula.isBlank();
                boolean visible = cedulasReveladas.containsKey(u.getIdUsuario());
                texto.setText(!tiene ? "Sin cédula" : (visible ? cedulasReveladas.get(u.getIdUsuario()) : "••••••••••"));
                texto.setStyle(visible ? "-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-weight: bold;" : "");
                icono.setImage(visible ? imagenOjoTachado : imagenOjo);
                boolean conOjo = tiene && !u.isProtegido();
                ojo.setVisible(conOjo);
                ojo.setManaged(conOjo);
                candado.setVisible(tiene && u.isProtegido());
                candado.setManaged(tiene && u.isProtegido());
                ojo.setOnAction(e -> alternarCedula(u, cedula));
                setGraphic(caja);
            }
        });
        colTelefono.setCellValueFactory(param -> {
            String[] contacto = contactos.get(param.getValue().getIdUsuario());
            return new SimpleStringProperty(contacto == null || contacto[1] == null || contacto[1].isBlank()
                    ? "Sin teléfono" : Telefono.formatear(contacto[1]));
        });

        // Código de recuperación de 6 dígitos: lo ve todo Administrador (el ojo lo muestra u oculta). Generar uno
        // nuevo (↻) lo puede hacer solo el administrador principal.
        Usuario sesion = Sesion.getUsuarioActual();
        colCodigo.setVisible(Permisos.esAdministrativo(sesion));
        esPrincipal = sesion != null && usuarioController.esPropietarioProtegido(sesion.getUsuarioLogin());
        colCodigo.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colCodigo.setCellFactory(col -> new TableCell<>() {
            private final Label texto = new Label();
            private final ImageView icono = new ImageView();
            private final Button ojo = new Button();
            private final Button nuevo = new Button("↻");
            private final HBox caja = new HBox(6, texto, ojo, nuevo);

            {
                icono.setFitWidth(18);
                icono.setFitHeight(18);
                icono.setPreserveRatio(true);
                ojo.setGraphic(icono);
                ojo.setStyle("-fx-background-color: transparent; -fx-padding: 2 4 2 4; -fx-cursor: hand;");
                nuevo.setStyle("-fx-background-color: transparent; -fx-padding: 2 6 2 6; -fx-cursor: hand; "
                        + "-fx-font-size: 15px; -fx-text-fill: #2C7BE5;");
                nuevo.setTooltip(new Tooltip("Generar un código nuevo (el anterior dejará de servir)"));
                caja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                if (vacio || u == null) {
                    setGraphic(null);
                    return;
                }
                if (u.isProtegido()) {
                    // El código del administrador principal se muestra una sola vez y no se puede ver aquí.
                    texto.setText("—");
                    texto.setStyle("");
                    ojo.setVisible(false);
                    ojo.setManaged(false);
                    nuevo.setVisible(false);
                    nuevo.setManaged(false);
                    setGraphic(caja);
                    return;
                }
                boolean tiene = conCodigo.contains(u.getIdUsuario());
                boolean visible = codigosRevelados.containsKey(u.getIdUsuario());
                texto.setText(visible ? codigosRevelados.get(u.getIdUsuario()) : (tiene ? "••••••" : "Sin código"));
                texto.setStyle(visible ? "-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-weight: bold;" : "");
                icono.setImage(visible ? imagenOjoTachado : imagenOjo);
                ojo.setOnAction(e -> alternarCodigo(u));
                ojo.setVisible(tiene);
                ojo.setManaged(tiene);
                nuevo.setVisible(esPrincipal);
                nuevo.setManaged(esPrincipal);
                nuevo.setOnAction(e -> renovarCodigo(u, tiene));
                setGraphic(caja);
            }
        });

        colEstado.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                if (vacio || u == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label();
                if ("activo".equals(u.getEstado())) {
                    badge.setText("Activo");
                    badge.getStyleClass().add("badge-suficiente");
                } else {
                    badge.setText("Inactivo");
                    badge.getStyleClass().add("badge-agotado");
                }
                setGraphic(badge);
            }
        });

        colAcciones.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("✏ Editar");
            private final Button btnClave = new Button("🔑 Clave");
            private final Button btnEstado = new Button();
            private final Button btnEliminar = new Button("🗑 Eliminar");
            private final HBox contenedor = new HBox(6, btnEditar, btnClave, btnEstado, btnEliminar);
            private final Label protegido = new Label("🔒 Usuario protegido");

            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                if (vacio || u == null) {
                    setGraphic(null);
                    return;
                }
                if (u.isProtegido()) {
                    // Usuario anclado (Rubén Torres): nadie puede hacerle nada desde aquí.
                    protegido.setStyle("-fx-text-fill: #78809A;");
                    setGraphic(protegido);
                    return;
                }
                btnEstado.setText("activo".equals(u.getEstado()) ? "Desactivar" : "Activar");
                btnEditar.setOnAction(e -> abrirFormulario(u));
                btnClave.setOnAction(e -> restablecerContrasena(u));
                btnEstado.setOnAction(e -> cambiarEstado(u));
                btnEliminar.setOnAction(e -> confirmarEliminar(u));
                setGraphic(contenedor);
            }
        });

        cargarUsuarios();
        campoBusqueda.textProperty().addListener((obs, anterior, nuevo) -> filtrar(nuevo));
    }

    private void cargarUsuarios() {
        reveladas.clear(); // al recargar, todas las contraseñas vuelven a ocultarse
        codigosRevelados.clear();
        cedulasReveladas.clear();
        contactos = usuarioController.contactosDeUsuarios();
        conCodigo = usuarioController.usuariosConCodigo();
        try {
            List<Usuario> usuarios = usuarioController.obtenerUsuarios();
            usuariosFiltrados = new FilteredList<>(FXCollections.observableArrayList(usuarios), u -> true);
            tablaUsuarios.setItems(usuariosFiltrados);
            filtrar(campoBusqueda.getText());
        } catch (RuntimeException e) {
            lblMensaje.setText("Error al cargar los usuarios: " + e.getMessage());
        }
    }

    private void filtrar(String texto) {
        if (usuariosFiltrados == null) return;

        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        usuariosFiltrados.setPredicate(u ->
                buscado.isEmpty()
                || u.getNombre().toLowerCase().contains(buscado)
                || u.getUsuarioLogin().toLowerCase().contains(buscado)
                || u.getRol().toLowerCase().contains(buscado));

        if (buscado.isEmpty()) {
            lblResultado.setText("");
        } else if (usuariosFiltrados.isEmpty()) {
            lblResultado.setText("✘ No hay usuarios que coincidan.");
        } else {
            lblResultado.setText("✔ " + usuariosFiltrados.size() + " resultado(s).");
        }
    }

    private void cambiarEstado(Usuario u) {
        String nuevoEstado = "activo".equals(u.getEstado()) ? "inactivo" : "activo";
        String error = usuarioController.cambiarEstado(u, nuevoEstado, Sesion.getUsuarioActual());
        if (error == null) {
            lblMensaje.setStyle("");
            lblMensaje.setText("✔ Usuario " + (nuevoEstado.equals("activo") ? "activado." : "desactivado."));
            cargarUsuarios();
        } else {
            mostrarError(error);
        }
    }

    /** Muestra u oculta la contraseña de un usuario (solo el Administrador puede verlas). */
    private void alternarContrasena(Usuario u) {
        if (reveladas.containsKey(u.getIdUsuario())) {
            reveladas.remove(u.getIdUsuario());
        } else {
            try {
                String contrasena = usuarioController.verContrasena(Sesion.getUsuarioActual(), u);
                if (contrasena == null) {
                    reveladas.put(u.getIdUsuario(), "No disponible");
                    lblMensaje.setStyle("");
                    lblMensaje.setText("ℹ La contraseña de " + u.getNombre() + " se creó antes de esta función y no se puede ver. "
                            + "Usa 🔑 Clave para generar una nueva y verla aquí.");
                } else {
                    reveladas.put(u.getIdUsuario(), contrasena);
                }
            } catch (RuntimeException e) {
                mostrarError(e.getMessage());
                return;
            }
        }
        tablaUsuarios.refresh();
    }

    /** Muestra u oculta la cédula de un usuario (solo Administradores; la consulta queda en la auditoría). */
    private void alternarCedula(Usuario u, String cedula) {
        if (cedulasReveladas.containsKey(u.getIdUsuario())) {
            cedulasReveladas.remove(u.getIdUsuario());
        } else {
            try {
                cedulasReveladas.put(u.getIdUsuario(),
                        usuarioController.verCedulaDeUsuario(Sesion.getUsuarioActual(), u, cedula));
            } catch (SecurityException e) {
                mostrarError(e.getMessage());
                return;
            }
        }
        tablaUsuarios.refresh();
    }

    /** Muestra u oculta el código de recuperación de un usuario (solo el administrador principal). */
    private void alternarCodigo(Usuario u) {
        if (codigosRevelados.containsKey(u.getIdUsuario())) {
            codigosRevelados.remove(u.getIdUsuario());
        } else {
            try {
                String codigo = usuarioController.verCodigoRecuperacion(Sesion.getUsuarioActual(), u);
                if (codigo == null) {
                    codigosRevelados.put(u.getIdUsuario(), "No disponible");
                    lblMensaje.setStyle("");
                    lblMensaje.setText("ℹ El código de " + u.getNombre() + " no tiene copia visible. "
                            + (esPrincipal ? "Pulsa ↻ para generar uno nuevo y verlo aquí."
                                           : "Pídele al administrador principal que genere uno nuevo."));
                } else {
                    codigosRevelados.put(u.getIdUsuario(), codigo);
                }
            } catch (RuntimeException e) {
                mostrarError(e.getMessage());
                return;
            }
        }
        tablaUsuarios.refresh();
    }

    /** Genera un código nuevo para un usuario y se lo muestra al administrador principal. */
    private void renovarCodigo(Usuario u, boolean teniaCodigo) {
        if (teniaCodigo && !Dialogos.confirmar("¿Generar un código nuevo para \"" + u.getNombre()
                + "\"? El código actual dejará de servir.")) {
            return;
        }
        try {
            String codigo = usuarioController.renovarCodigoRecuperacion(Sesion.getUsuarioActual(), u);
            lblMensaje.setStyle("");
            lblMensaje.setText("✔ Código generado para " + u.getNombre() + ".");
            cargarUsuarios();
            CodigoRecuperacionVista.mostrar(codigo, "Código de " + u.getNombre(),
                    "Entrégaselo para que pueda recuperar su acceso.");
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    /** Genera una contraseña temporal y se la muestra al administrador una sola vez. */
    private void restablecerContrasena(Usuario u) {
        if (!Dialogos.confirmar("¿Generar una contraseña temporal para \"" + u.getNombre()
                + "\"? La contraseña actual dejará de funcionar.")) {
            return;
        }
        String temporal = PasswordUtil.generarTemporal(8);
        String error = usuarioController.restablecerContrasenaTemporal(u, temporal);
        if (error == null) {
            lblMensaje.setStyle("");
            lblMensaje.setText("✔ Contraseña restablecida para " + u.getNombre() + ".");
            Dialogos.mostrarCredenciales(u.getNombre(), u.getUsuarioLogin(), temporal);
        } else {
            mostrarError(error);
        }
    }

    private void confirmarEliminar(Usuario u) {
        if (Dialogos.confirmar("¿Eliminar definitivamente al usuario \"" + u.getNombre()
                + "\"? Esta acción no se puede deshacer.")) {
            String error = usuarioController.eliminarUsuario(u, Sesion.getUsuarioActual());
            if (error == null) {
                lblMensaje.setStyle("");
                lblMensaje.setText("✔ Usuario eliminado.");
                cargarUsuarios();
            } else {
                mostrarError(error);
            }
        }
    }

    @FXML
    private void abrirFormularioNuevo(ActionEvent event) {
        abrirFormulario(null);
    }

    private void abrirFormulario(Usuario usuarioAEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/usuarioSistemaForm.fxml"));
            Parent root = loader.load();
            UsuarioSistemaFormVista controlador = loader.getController();
            controlador.configurar(usuarioAEditar, false, () -> {
                lblMensaje.setStyle("");
                lblMensaje.setText("✔ Datos guardados correctamente.");
                cargarUsuarios();
            });

            Stage dialogo = new Stage();
            dialogo.initModality(Modality.APPLICATION_MODAL);
            dialogo.setTitle(usuarioAEditar == null ? "Nuevo usuario" : "Editar usuario");
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
