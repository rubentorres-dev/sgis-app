package sgis.db;

import sgis.modelo.RegistroAuditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla AUDITORIA (registro de quién hizo qué en el sistema).
 */
public class AuditoriaDAO {

    public void registrar(Integer idUsuario, String usuario, String accion, String detalle) throws SQLException {
        String sql = "INSERT INTO AUDITORIA (fecha, idUsuario, usuario, accion, detalle) VALUES (?, ?, ?, ?, ?)";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            if (idUsuario == null) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, idUsuario);
            }
            stmt.setString(3, recortar(usuario, 150));
            stmt.setString(4, recortar(accion, 60));
            stmt.setString(5, recortar(detalle, 255));
            stmt.executeUpdate();
        }
    }

    /** Los 5000 registros más recientes, del más nuevo al más antiguo. */
    public List<RegistroAuditoria> listar() throws SQLException {
        String sql = "SELECT fecha, usuario, accion, detalle FROM AUDITORIA ORDER BY fecha DESC, idAuditoria DESC LIMIT 5000";
        List<RegistroAuditoria> registros = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                registros.add(new RegistroAuditoria(
                        rs.getTimestamp("fecha").toLocalDateTime(),
                        rs.getString("usuario"),
                        rs.getString("accion"),
                        rs.getString("detalle")));
            }
        }
        return registros;
    }

    /**
     * Fechas de los registros de una acción (con ese detalle exacto) posteriores al registro número idMayorQue,
     * del más reciente al más antiguo (hasta el límite indicado). Se compara por número de registro y no por
     * fecha porque la fecha solo tiene precisión de segundos.
     */
    public List<LocalDateTime> ultimasFechas(String accion, String detalle, long idMayorQue, int limite) throws SQLException {
        String sql = "SELECT fecha FROM AUDITORIA WHERE accion = ? AND detalle = ? AND idAuditoria > ? "
                   + "ORDER BY idAuditoria DESC LIMIT ?";
        List<LocalDateTime> fechas = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, accion);
            stmt.setString(2, detalle);
            stmt.setLong(3, idMayorQue);
            stmt.setInt(4, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    fechas.add(rs.getTimestamp(1).toLocalDateTime());
                }
            }
        }
        return fechas;
    }

    /** Número del registro más reciente cuya acción empieza con el texto dado y que tiene ese detalle, o 0. */
    public long ultimoIdQueEmpiezaCon(String prefijo, String detalle) throws SQLException {
        String sql = "SELECT MAX(idAuditoria) FROM AUDITORIA WHERE accion LIKE ? AND detalle = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, prefijo + "%");
            stmt.setString(2, detalle);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0; // MAX sin filas devuelve NULL, que getLong lee como 0
            }
        }
    }

    /** Número del registro más reciente de una acción exacta, o 0 si no hay ninguno. */
    public long ultimoId(String accion) throws SQLException {
        String sql = "SELECT MAX(idAuditoria) FROM AUDITORIA WHERE accion = ?";
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, accion);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /** Fechas de los registros de una acción (cualquier detalle) posteriores al registro idMayorQue, del más reciente al más antiguo. */
    public List<LocalDateTime> ultimasFechasDeAccion(String accion, long idMayorQue, int limite) throws SQLException {
        String sql = "SELECT fecha FROM AUDITORIA WHERE accion = ? AND idAuditoria > ? ORDER BY idAuditoria DESC LIMIT ?";
        List<LocalDateTime> fechas = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conexion.prepareStatement(sql)) {
            stmt.setString(1, accion);
            stmt.setLong(2, idMayorQue);
            stmt.setInt(3, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    fechas.add(rs.getTimestamp(1).toLocalDateTime());
                }
            }
        }
        return fechas;
    }

    private String recortar(String texto, int maximo) {
        if (texto == null) return null;
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
