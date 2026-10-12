package sgis.controlador;

import sgis.db.MovimientoDAO;
import sgis.modelo.Actividad;
import sgis.modelo.Movimiento;
import sgis.util.StockInsuficienteException;

import java.sql.SQLException;
import java.util.List;

/**
 * Controlador del módulo de Entradas/Salidas (Movimientos).
 * Traduce los errores técnicos (SQLException) y de negocio
 * (StockInsuficienteException) en mensajes claros para la vista.
 */
public class MovimientoController {

    private final MovimientoDAO movimientoDAO = new MovimientoDAO();

    /**
     * Registra una entrada de producto.
     * @return null si todo salió bien, o un mensaje de error para mostrar en pantalla.
     */
    public String registrarEntrada(int idProducto, int cantidad, int idUsuario) {
        if (cantidad <= 0) {
            return "La cantidad debe ser mayor que 0.";
        }
        try {
            movimientoDAO.registrarEntrada(idProducto, cantidad, idUsuario);
            return null;
        } catch (SQLException e) {
            return "Error al registrar la entrada: " + e.getMessage();
        }
    }

    /**
     * Registra una salida de producto, validando que haya stock suficiente.
     * @return null si todo salió bien, o un mensaje de error para mostrar en pantalla.
     */
    public String registrarSalida(int idProducto, int cantidad, int idUsuario) {
        if (cantidad <= 0) {
            return "La cantidad debe ser mayor que 0.";
        }
        try {
            movimientoDAO.registrarSalida(idProducto, cantidad, idUsuario);
            return null;
        } catch (StockInsuficienteException e) {
            return e.getMessage(); // Mensaje de negocio, claro para el usuario final
        } catch (SQLException e) {
            return "Error al registrar la salida: " + e.getMessage();
        }
    }

    public List<Movimiento> obtenerMovimientos() {
        try {
            return movimientoDAO.listarMovimientos();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los movimientos: " + e.getMessage(), e);
        }
    }

    /** Historial completo: entradas, salidas y ventas (con quién vendió y quién compró). */
    public List<Actividad> obtenerHistorial() {
        try {
            return movimientoDAO.listarHistorial();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el historial de movimientos: " + e.getMessage(), e);
        }
    }
}
