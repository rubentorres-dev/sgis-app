package sgis.vista;

import sgis.modelo.Cliente;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Autocompletado de clientes registrados para el formulario de ventas.
 *
 * Mientras se escribe en el campo de nombre o de correo, muestra una lista con los clientes
 * registrados que coinciden. Al elegir uno, se rellenan los dos campos con sus datos, lo que
 * agiliza la facturación y evita duplicar clientes.
 */
public class AutocompletarCliente {

    private static final int MAX_SUGERENCIAS = 6;
    private static final int MIN_CARACTERES = 2;

    private final TextField campoNombre;
    private final TextField campoCorreo;
    private final ContextMenu menu = new ContextMenu();
    private List<Cliente> clientes = new ArrayList<>();
    private boolean rellenando = false;

    public AutocompletarCliente(TextField campoNombre, TextField campoCorreo) {
        this.campoNombre = campoNombre;
        this.campoCorreo = campoCorreo;
        escuchar(campoNombre);
        escuchar(campoCorreo);
    }

    /** Actualiza la lista de clientes registrados contra la que se compara lo que se escribe. */
    public void setClientes(List<Cliente> clientes) {
        this.clientes = new ArrayList<>(clientes);
    }

    private void escuchar(TextField campo) {
        campo.textProperty().addListener((obs, anterior, nuevo) -> {
            if (!rellenando && campo.isFocused()) {
                mostrarSugerencias(campo, nuevo);
            }
        });
        campo.focusedProperty().addListener((obs, estabaEnfocado, estaEnfocado) -> {
            if (!estaEnfocado) {
                menu.hide();
            }
        });
    }

    private void mostrarSugerencias(TextField campo, String texto) {
        menu.hide();

        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        if (buscado.length() < MIN_CARACTERES) {
            return;
        }

        List<Cliente> coincidencias = clientes.stream()
                .filter(c -> c.getNombre().toLowerCase().contains(buscado)
                          || c.getCorreo().toLowerCase().contains(buscado))
                .limit(MAX_SUGERENCIAS)
                .collect(Collectors.toList());
        if (coincidencias.isEmpty()) {
            return;
        }

        menu.getItems().clear();
        for (Cliente cliente : coincidencias) {
            MenuItem opcion = new MenuItem(cliente.getNombre() + "  —  " + cliente.getCorreo());
            opcion.setOnAction(e -> seleccionar(cliente));
            menu.getItems().add(opcion);
        }
        menu.show(campo, Side.BOTTOM, 0, 0);
    }

    private void seleccionar(Cliente cliente) {
        rellenando = true;
        campoNombre.setText(cliente.getNombre());
        campoCorreo.setText(cliente.getCorreo());
        rellenando = false;
        menu.hide();
    }
}
