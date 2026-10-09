package sgis.controlador;

import sgis.db.ReporteDAO;
import sgis.modelo.ResumenInventario;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controlador del módulo de Reportes (KPIs del Panel Principal).
 */
public class ReporteController {

    private final ReporteDAO reporteDAO = new ReporteDAO();

    public ResumenInventario obtenerResumen() {
        try {
            return reporteDAO.obtenerResumen();
        } catch (SQLException e) {
            throw new RuntimeException("Error al calcular el resumen de inventario: " + e.getMessage(), e);
        }
    }

    public List<Map.Entry<String, Integer>> obtenerStockPorProducto() {
        try {
            return reporteDAO.obtenerStockPorProducto();
        } catch (SQLException e) {
            throw new RuntimeException("Error al calcular el stock por producto: " + e.getMessage(), e);
        }
    }
}
