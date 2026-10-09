package sgis.vista;

import sgis.modelo.Usuario;
import sgis.servicio.AuditoriaService;
import sgis.util.Permisos;
import sgis.util.Permisos.Pantalla;
import sgis.util.Sesion;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador del menú lateral (menuLateral.fxml), compartido por todas las pantallas.
 * Muestra solo las opciones que el rol del usuario puede usar y marca la pantalla actual.
 */
public class MenuLateralVista implements Initializable {

    @FXML private Button btnPanel;
    @FXML private Button btnProductos;
    @FXML private Button btnVentas;
    @FXML private Button btnReportes;
    @FXML private Button btnUsuarios;
    @FXML private Button btnConfiguracion;
    @FXML private Button btnCerrarSesion;
    @FXML private Label lblUsuarioActual;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Usuario usuario = Sesion.getUsuarioActual();

        Map<Pantalla, Button> botones = new EnumMap<>(Pantalla.class);
        botones.put(Pantalla.PANEL, btnPanel);
        botones.put(Pantalla.PRODUCTOS, btnProductos);
        botones.put(Pantalla.VENTAS, btnVentas);
        botones.put(Pantalla.REPORTES, btnReportes);
        botones.put(Pantalla.USUARIOS, btnUsuarios);
        botones.put(Pantalla.CONFIGURACION, btnConfiguracion);

        Pantalla actual = Navegacion.getPantallaActual();
        for (Map.Entry<Pantalla, Button> entrada : botones.entrySet()) {
            Button boton = entrada.getValue();

            boolean permitido = Permisos.puedeAcceder(usuario, entrada.getKey());
            boton.setVisible(permitido);
            boton.setManaged(permitido);

            if (entrada.getKey() == actual) {
                boton.getStyleClass().remove("nav-item");
                boton.getStyleClass().add("nav-item-active");
            }
        }

        if (usuario != null) {
            lblUsuarioActual.setText(usuario.getNombre() + "\n" + usuario.getRol());
        }
    }

    @FXML
    private void irAPanel(ActionEvent event) {
        Navegacion.cambiarEscena(event, "/fxml/panelPrincipal.fxml");
    }

    @FXML
    private void irAProductos(ActionEvent event) {
        Navegacion.cambiarEscena(event, "/fxml/productos.fxml");
    }



    @FXML
    private void irAVentas(ActionEvent event) {
        Navegacion.cambiarEscena(event, "/fxml/ventas.fxml");
    }

    @FXML
    private void irAReportes(ActionEvent event) {
        Navegacion.cambiarEscena(event, "/fxml/reportes.fxml");
    }

    @FXML
    private void irAUsuarios(ActionEvent event) {
        Navegacion.cambiarEscena(event, "/fxml/usuarios.fxml");
    }

    @FXML
    private void irAConfiguracion(ActionEvent event) {
        Navegacion.cambiarEscena(event, "/fxml/configuracion.fxml");
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {
        AuditoriaService.registrar(Sesion.getUsuarioActual(), "Cerró sesión", null);
        Sesion.cerrar();
        Navegacion.cambiarEscena(event, "/fxml/login.fxml");
    }
}
