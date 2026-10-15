package sgis.controlador;

import sgis.db.ClienteDAO;
import sgis.modelo.Cliente;
import sgis.modelo.Usuario;
import sgis.util.Permisos;
import sgis.util.Cedula;
import sgis.util.MensajesBD;
import sgis.util.Telefono;
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

    /** true si el texto tiene forma de correo (algo@dominio.ext). Lo usan Clientes y Ventas. */
    public static boolean correoValido(String correo) {
        return correo != null && CORREO_VALIDO.matcher(correo.trim()).matches();
    }

    private final ClienteDAO clienteDAO = new ClienteDAO();

    public List<Cliente> obtenerClientes() {
        try {
            return clienteDAO.listarClientes();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los clientes: " + e.getMessage(), e);
        }
    }

    /** @return null si todo salió bien, o un mensaje de error para mostrar en pantalla. */
    /** Mensaje para el usuario cuando falla la base de datos (con aviso claro si falta el script de la cédula). */
    private String mensajeDe(SQLException e, String prefijo) {
        String falta = MensajesBD.scriptFaltante(e);
        if (falta != null) {
            return falta;
        }
        if ("23000".equals(e.getSQLState())) {
            return "Ya existe un cliente con ese correo o esa cédula.";
        }
        return prefijo + e.getMessage();
    }

    public String registrarCliente(String nombre, String correo, String cedula, String telefono) {
        String error = validar(nombre, correo, cedula, telefono, -1);
        if (error != null) return error;

        Cliente c = new Cliente();
        c.setNombre(nombre.trim());
        c.setCorreo(correo.trim());
        c.setCedula(Cedula.normalizar(cedula));
        c.setTelefono(Telefono.normalizar(telefono));
        try {
            clienteDAO.registrar(c);
            // La cédula no se escribe en la auditoría.
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Registró cliente", c.getNombre() + " · " + c.getCorreo());
            return null;
        } catch (SQLException e) {
            return mensajeDe(e, "Error al registrar el cliente: ");
        }
    }

    public String actualizarCliente(Cliente cliente, String nuevoNombre, String nuevoCorreo, String nuevaCedula, String nuevoTelefono) {
        String error = validar(nuevoNombre, nuevoCorreo, nuevaCedula, nuevoTelefono, cliente.getIdCliente());
        if (error != null) return error;

        Cliente actualizado = new Cliente(cliente.getIdCliente(), nuevoNombre.trim(), nuevoCorreo.trim(),
                Cedula.normalizar(nuevaCedula), Telefono.normalizar(nuevoTelefono));
        try {
            clienteDAO.actualizar(actualizado);
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Modificó cliente",
                    cliente.getNombre() + " → " + actualizado.getNombre() + " · " + actualizado.getCorreo());
            return null;
        } catch (SQLException e) {
            return mensajeDe(e, "Error al actualizar el cliente: ");
        }
    }

    /**
     * La cédula de un cliente ya formateada (1.037.456.789) para mostrarla en Configuración. Solo un Administrador
     * puede verla, y cada consulta queda en la auditoría.
     */
    public String verCedula(Usuario quienPide, Cliente cliente) {
        // La puede ver quien tenga acceso a la pantalla Clientes (los Administradores siempre lo tienen).
        if (quienPide == null || !Permisos.puedeAcceder(quienPide, Permisos.Pantalla.USUARIOS)) {
            throw new SecurityException("Tu rol no tiene acceso a la pantalla Clientes.");
        }
        AuditoriaService.registrar(quienPide, "Vio la cédula de un cliente", cliente.getNombre() + " · " + cliente.getCorreo());
        return Cedula.formatear(cliente.getCedula());
    }

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

    private String validar(String nombre, String correo, String cedula, String telefono, int idClienteExcluido) {
        if (nombre == null || nombre.isBlank()) {
            return "El nombre es obligatorio.";
        }
        if (correo == null || correo.isBlank()) {
            return "El correo es obligatorio.";
        }
        if (!CORREO_VALIDO.matcher(correo.trim()).matches()) {
            return "Escribe un correo válido (ej. nombre@correo.com).";
        }
        String errorCedula = Cedula.validar(cedula);
        if (errorCedula != null) {
            return errorCedula;
        }
        String errorTelefono = Telefono.validar(telefono);
        if (errorTelefono != null) {
            return errorTelefono;
        }
        try {
            if (clienteDAO.existeCorreo(correo.trim(), idClienteExcluido)) {
                return "Ya existe un cliente registrado con ese correo.";
            }
            if (clienteDAO.existeCedula(Cedula.normalizar(cedula), idClienteExcluido)) {
                return "Ya existe un cliente registrado con esa cédula.";
            }
        } catch (SQLException e) {
            return mensajeDe(e, "Error al verificar los datos: ");
        }
        return null;
    }
}
