package sgis.db;

import sgis.modelo.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla CLIENTE.
 */
public class ClienteDAO {

    /**
     * Busca un cliente por su correo dentro de una transacción existente
     * (usa la MISMA conexión que le pasa VentaDAO).
     * @return el idCliente encontrado, o -1 si no existe todavía.
     */
    public int buscarIdPorCorreo(Connection conexion, String correo) throws SQLException {
        String sql = "SELECT idCliente FROM CLIENTE WHERE correo = ?";
        try (PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, correo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("idCliente") : -1;
            }
        }
    }

    /**
     * Registra un cliente nuevo dentro de una transacción existente.
     * @return el idCliente recién generado.
     */
    public int registrarCliente(Connection conexion, Cliente cliente) throws SQLException {
        String sql = "INSERT INTO CLIENTE (nombre, correo) VALUES (?, ?)";
        try (PreparedStatement stmt = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getCorreo());
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
     * Busca el cliente por correo; si no existe, lo crea.
     * Usa la MISMA conexión/transacción que VentaDAO le pase.
     */
    public int obtenerOCrearCliente(Connection conexion, String nombre, String correo) throws SQLException {
        int idExistente = buscarIdPorCorreo(conexion, correo);
        if (idExistente != -1) {
            return idExistente;
        }
        Cliente nuevo = new Cliente();
        nuevo.setNombre(nombre);
        nuevo.setCorreo(correo);
        return registrarCliente(conexion, nuevo);
    }

    // ---------------------------------------------------------------
    // Gestión de clientes desde la pantalla "Usuarios"
    // (cada método abre y cierra su propia conexión)
    // ---------------------------------------------------------------

    /** Devuelve todos los clientes registrados, ordenados por nombre. */
    public List<Cliente> listarClientes() throws SQLException {
        String sql = "SELECT idCliente, nombre, correo FROM CLIENTE ORDER BY nombre";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                clientes.add(new Cliente(rs.getInt("idCliente"), rs.getString("nombre"), rs.getString("correo")));
            }
        }
        return clientes;
    }

    /**
     * Indica si ya existe un cliente con ese correo (sin distinguir mayúsculas).
     * @param idClienteExcluido id del cliente que se está editando (para no compararlo
     *                          consigo mismo), o -1 si se está registrando uno nuevo.
     */
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

    /** Registra un cliente nuevo (desde el botón "Registrar usuario"). */
    public void registrar(Cliente cliente) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            registrarCliente(conexion, cliente);
        }
    }

    /** Actualiza el nombre y el correo de un cliente existente. */
    public void actualizar(Cliente cliente) throws SQLException {
        String sql = "UPDATE CLIENTE SET nombre = ?, correo = ? WHERE idCliente = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getCorreo());
            stmt.setInt(3, cliente.getIdCliente());
            stmt.executeUpdate();
        }
    }

    /** Cuántas ventas tiene registradas un cliente. */
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

    /** Elimina definitivamente un cliente (solo si no tiene ventas asociadas). */
    public void eliminar(int idCliente) throws SQLException {
        String sql = "DELETE FROM CLIENTE WHERE idCliente = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setInt(1, idCliente);
            stmt.executeUpdate();
        }
    }
}
