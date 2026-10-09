package sgis.controlador;

import sgis.db.CategoriaDAO;
import sgis.db.ProductoDAO;
import sgis.modelo.Categoria;
import sgis.modelo.Producto;
import sgis.util.Sesion;
import sgis.servicio.AuditoriaService;
import sgis.util.FormatoMoneda;

import java.sql.SQLException;
import java.util.List;

/**
 * Controlador del módulo de Productos.
 * Hace de intermediario entre la Vista (pantalla) y el DAO (base de datos):
 * la vista nunca llama directamente al DAO, siempre pasa por aquí.
 */
public class ProductoController {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    public List<Producto> obtenerProductos() {
        try {
            return productoDAO.listarProductos();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los productos: " + e.getMessage(), e);
        }
    }

    public List<Categoria> obtenerCategorias() {
        try {
            return categoriaDAO.listarCategorias();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar las categorías: " + e.getMessage(), e);
        }
    }

    /** @return null si todo salió bien, o un mensaje de error para mostrar en pantalla. */
    public String registrarProducto(Producto p) {
        try {
            productoDAO.registrarProducto(p, idUsuarioActual());
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Creó producto",
                    p.getNombre() + " · " + FormatoMoneda.formatear(p.getPrecio()) + " · " + p.getCantidadDisponible() + " unidades");
            return null;
        } catch (SQLException e) {
            return "Error al registrar el producto: " + e.getMessage();
        }
    }

    /** @return null si todo salió bien, o un mensaje de error para mostrar en pantalla. */
    public String actualizarProducto(Producto p) {
        try {
            productoDAO.actualizarProducto(p, idUsuarioActual());
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Editó producto",
                    p.getNombre() + " · " + FormatoMoneda.formatear(p.getPrecio()) + " · " + p.getCantidadDisponible()
                    + " unidades · " + p.getEstado());
            return null;
        } catch (SQLException e) {
            return "Error al actualizar el producto: " + e.getMessage();
        }
    }

    /** @return null si todo salió bien, o un mensaje de error para mostrar en pantalla. */
    public String desactivarProducto(int idProducto) {
        try {
            productoDAO.desactivarProducto(idProducto);
            return null;
        } catch (SQLException e) {
            return "Error al desactivar el producto: " + e.getMessage();
        }
    }

    /** Usuario que inició sesión (para registrar quién hizo cada cambio de existencias). */
    private Integer idUsuarioActual() {
        return Sesion.getUsuarioActual() == null ? null : Sesion.getUsuarioActual().getIdUsuario();
    }
}
