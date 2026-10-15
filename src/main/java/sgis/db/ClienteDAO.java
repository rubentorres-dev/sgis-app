package sgis.db;

import sgis.modelo.Cliente;
import sgis.util.DatosClienteException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Acceso a datos de los clientes (personas que compran o piden factura). */
public class ClienteDAO {

    public int buscarIdPorCorreo(Connection conexion, String correo) throws SQLException {
        String sql = "SELECT idCliente FROM CLIENTE WHERE correo = ?";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, correo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("idCliente") : -1;
            }
        }
    }

    /** El cliente con ese correo (con su cédula y teléfono, que pueden ser null), o null si no existe. */
    private Cliente buscarPorCorreo(Connection conexion, String correo) throws SQLException {
        String sql = "SELECT idCliente, nombre, correo, cedula, telefono FROM CLIENTE WHERE correo = ?";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, correo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next()
                        ? new Cliente(rs.getInt("idCliente"), rs.getString("nombre"), rs.getString("correo"),
                                rs.getString("cedula"), rs.getString("telefono"))
                        : null;
            }
        }
    }

    private int buscarIdPorCedula(Connection conexion, String cedula) throws SQLException {
        String sql = "SELECT idCliente FROM CLIENTE WHERE cedula = ?";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, cedula);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("idCliente") : -1;
            }
        }
    }

    public int registrarCliente(Connection conexion, Cliente cliente) throws SQLException {
        String sql = "INSERT INTO CLIENTE (nombre, correo, cedula, telefono) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getCorreo());
            stmt.setString(3, cliente.getCedula());
            stmt.setString(4, cliente.getTelefono());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("No se pudo obtener el id del cliente recién creado.");
            }
        }
    }

    /**
     * Devuelve el id del cliente de una venta: lo busca por correo y, si no existe, lo crea con su cédula y teléfono.
     * La cédula identifica a una sola persona: no puede quedar repetida ni cambiarse desde una venta. El teléfono
     * sí se actualiza con el de la venta (la gente cambia de número).
     */
    public int obtenerOCrearCliente(Connection conexion, String nombre, String correo, String cedula, String telefono)
            throws SQLException, DatosClienteException {
        Cliente porCorreo = buscarPorCorreo(conexion, correo);
        int idPorCedula = buscarIdPorCedula(conexion, cedula);

        if (porCorreo != null) {
            if (porCorreo.getCedula() != null && !porCorreo.getCedula().equals(cedula)) {
                throw new DatosClienteException("Ese correo ya está registrado con otra cédula. Revisa el número, "
                        + "o pide a un Administrador que corrija los datos del cliente en Configuración > Clientes.");
            }
            if (idPorCedula != -1 && idPorCedula != porCorreo.getIdCliente()) {
                throw new DatosClienteException("Esa cédula ya pertenece a otro cliente registrado.");
            }
            if (porCorreo.getCedula() == null) {
                // Cliente anterior a esta función: se completa con la cédula de esta venta.
                ejecutarUpdate(conexion, "UPDATE CLIENTE SET cedula = ? WHERE idCliente = ?", cedula, porCorreo.getIdCliente());
            }
            if (!telefono.equals(porCorreo.getTelefono())) {
                ejecutarUpdate(conexion, "UPDATE CLIENTE SET telefono = ? WHERE idCliente = ?", telefono, porCorreo.getIdCliente());
            }
            return porCorreo.getIdCliente();
        }
        if (idPorCedula != -1) {
            throw new DatosClienteException("Esa cédula ya está registrada con otro correo. Selecciona al cliente "
                    + "de la lista o revisa el número.");
        }
        Cliente nuevo = new Cliente();
        nuevo.setNombre(nombre);
        nuevo.setCorreo(correo);
        nuevo.setCedula(cedula);
        nuevo.setTelefono(telefono);
        return registrarCliente(conexion, nuevo);
    }

    private void ejecutarUpdate(Connection conexion, String sql, String valor, int id) throws SQLException {
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, valor);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public List<Cliente> listarClientes() throws SQLException {
        // Si los scripts de la cédula o del teléfono aún no se ejecutaron, la lista sigue funcionando sin esas columnas.
        try {
            return listar("SELECT idCliente, nombre, correo, cedula, telefono FROM CLIENTE ORDER BY nombre", true, true);
        } catch (SQLException e) {
            if (!"42S22".equals(e.getSQLState())) throw e;
        }
        try {
            return listar("SELECT idCliente, nombre, correo, cedula FROM CLIENTE ORDER BY nombre", true, false);
        } catch (SQLException e) {
            if (!"42S22".equals(e.getSQLState())) throw e;
        }
        return listar("SELECT idCliente, nombre, correo FROM CLIENTE ORDER BY nombre", false, false);
    }

    private List<Cliente> listar(String sql, boolean conCedula, boolean conTelefono) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                clientes.add(new Cliente(rs.getInt("idCliente"), rs.getString("nombre"), rs.getString("correo"),
                        conCedula ? rs.getString("cedula") : null, conTelefono ? rs.getString("telefono") : null));
            }
        }
        return clientes;
    }

    public boolean existeCorreo(String correo, int idClienteExcluido) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM CLIENTE WHERE LOWER(correo) = LOWER(?) AND idCliente <> ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, correo);
            stmt.setInt(2, idClienteExcluido);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        }
    }

    public boolean existeCedula(String cedula, int idClienteExcluido) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM CLIENTE WHERE cedula = ? AND idCliente <> ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, cedula);
            stmt.setInt(2, idClienteExcluido);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        }
    }

    /** Registra un cliente nuevo (desde el botón "Registrar cliente"). */
    public void registrar(Cliente cliente) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            registrarCliente(conexion, cliente);
        }
    }

    public void actualizar(Cliente cliente) throws SQLException {
        String sql = "UPDATE CLIENTE SET nombre = ?, correo = ?, cedula = ?, telefono = ? WHERE idCliente = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getCorreo());
            stmt.setString(3, cliente.getCedula());
            stmt.setString(4, cliente.getTelefono());
            stmt.setInt(5, cliente.getIdCliente());
            stmt.executeUpdate();
        }
    }

    public int contarVentas(int idCliente) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM VENTA WHERE idCliente = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    public void eliminar(int idCliente) throws SQLException {
        String sql = "DELETE FROM CLIENTE WHERE idCliente = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idCliente);
            stmt.executeUpdate();
        }
    }
}
