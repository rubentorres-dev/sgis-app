package sgis.modelo;

import java.time.LocalDateTime;

/**
 * Una línea del registro de auditoría: quién hizo qué y cuándo.
 * Corresponde a la tabla AUDITORIA.
 */
public class RegistroAuditoria {

    private final LocalDateTime fecha;
    private final String usuario;   // nombre y rol de quien lo hizo, tal como eran en ese momento
    private final String accion;
    private final String detalle;

    public RegistroAuditoria(LocalDateTime fecha, String usuario, String accion, String detalle) {
        this.fecha = fecha;
        this.usuario = usuario;
        this.accion = accion;
        this.detalle = detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getDetalle() {
        return detalle;
    }
}
