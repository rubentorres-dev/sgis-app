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

    private String recortar(String texto, int maximo) {
        if (texto == null) return null;
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
