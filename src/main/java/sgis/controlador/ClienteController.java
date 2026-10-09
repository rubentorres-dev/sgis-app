package sgis.controlador;

import sgis.db.ClienteDAO;
import sgis.modelo.Cliente;
import sgis.servicio.AuditoriaService;
import sgis.util.Sesion;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Controlador de clientes: personas que compran o necesitan facturación.
 * Los clientes se registran automáticamente al hacer una venta, y también
 * se pueden registrar y editar manualmente desde la pantalla "Usuarios".
 */
public class ClienteController {

    private static final Pattern CORREO_VALIDO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ClienteDAO clienteDAO = new ClienteDAO();

    public List<Cliente> obtenerClientes() {
        try {
            return clienteDAO.listarClientes();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los clientes: " + e.getMessage(), e);
        }
    }

    /** @return null si todo salió bien, o un mensaje de error para mostrar en pantalla. */
    public String registrarCliente(String nombre, String correo) {
        String error = validar(nombre, correo, -1);
        if (error != null) return error;

        Cliente c = new Cliente();
        c.setNombre(nombre.trim());
        c.setCorreo(correo.trim());
        try {
            clienteDAO.registrar(c);
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Registró cliente", c.getNombre() + " · " + c.getCorreo());
            return null;
        } catch (SQLException e) {
            return "Error al registrar el cliente: " + e.getMessage();
        }
    }

    /** @return null si todo salió bien, o un mensaje de error para mostrar en pantalla. */
    public String actualizarCliente(Cliente cliente, String nuevoNombre, String nuevoCorreo) {
        String error = validar(nuevoNombre, nuevoCorreo, cliente.getIdCliente());
        if (error != null) return error;

        Cliente actualizado = new Cliente(cliente.getIdCliente(), nuevoNombre.trim(), nuevoCorreo.trim());
        try {
            clienteDAO.actualizar(actualizado);
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Modificó cliente",
                    cliente.getNombre() + " → " + actualizado.getNombre() + " · " + actualizado.getCorreo());
            return null;
        } catch (SQLException e) {
            return "Error al actualizar el cliente: " + e.getMessage();
        }
    }

    /** @return null si todo salió bien, o un mensaje de error para mostrar en pantalla. */
    public String eliminarCliente(Cliente cliente) {
        try {
            int ventas = clienteDAO.contarVentas(cliente.getIdCliente());
            if (ventas > 0) {
                return "No se puede eliminar: este cliente tiene " + ventas
                        + " venta(s) registradas. Para conservar el historial de ventas, no se eliminan clientes que ya compraron.";
            }
            clienteDAO.eliminar(cliente.getIdCliente());
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Eliminó cliente", cliente.getNombre() + " · " + cliente.getCorreo());
            return null;
        } catch (SQLException e) {
            return "Error al eliminar el cliente: " + e.getMessage();
        }
    }

    private String validar(String nombre, String correo, int idClienteExcluido) {
        if (nombre == null || nombre.isBlank()) {
            return "El nombre es obligatorio.";
        }
        if (correo == null || correo.isBlank()) {
            return "El correo es obligatorio.";
        }
        if (!CORREO_VALIDO.matcher(correo.trim()).matches()) {
            return "Escribe un correo válido (ej. nombre@correo.com).";
        }
        try {
            if (clienteDAO.existeCorreo(correo.trim(), idClienteExcluido)) {
                return "Ya existe un cliente registrado con ese correo.";
            }
        } catch (SQLException e) {
            return "Error al verificar el correo: " + e.getMessage();
        }
        return null;
    }
}
