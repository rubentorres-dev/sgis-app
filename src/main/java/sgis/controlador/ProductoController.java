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
        String invalido = validar(p);
        if (invalido != null) {
            return invalido;
        }
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
        String invalido = validar(p);
        if (invalido != null) {
            return invalido;
        }
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
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Desactivó producto", "Producto n.º " + idProducto);
            return null;
        } catch (SQLException e) {
            return "Error al desactivar el producto: " + e.getMessage();
        }
    }

    private static final int LARGO_MAXIMO_NOMBRE = 150;
    private static final int LARGO_MAXIMO_DESCRIPCION = 255;
    private static final double PRECIO_MAXIMO = 99_999_999.99; // el límite de la columna DECIMAL(10,2)

    /** Reglas del producto (la pantalla las comparte): nombre, precio y cantidad deben tener sentido. */
    private String validar(Producto p) {
        if (p == null) {
            return "Faltan los datos del producto.";
        }
        String nombre = p.getNombre() == null ? "" : p.getNombre().trim();
        if (nombre.isEmpty()) {
            return "El nombre es obligatorio.";
        }
        if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
            return "El nombre no puede tener más de " + LARGO_MAXIMO_NOMBRE + " caracteres.";
        }
        if (p.getDescripcion() != null && p.getDescripcion().length() > LARGO_MAXIMO_DESCRIPCION) {
            return "La descripción no puede tener más de " + LARGO_MAXIMO_DESCRIPCION + " caracteres.";
        }
        if (Double.isNaN(p.getPrecio()) || Double.isInfinite(p.getPrecio()) || p.getPrecio() <= 0) {
            return "El precio debe ser un número mayor que 0.";
        }
        if (p.getPrecio() > PRECIO_MAXIMO) {
            return "El precio no puede superar " + FormatoMoneda.formatear(PRECIO_MAXIMO) + ".";
        }
        if (p.getCantidadDisponible() < 0) {
            return "La cantidad no puede ser negativa.";
        }
        p.setNombre(nombre); // se guarda sin espacios de más
        return null;
    }

    /** Usuario que inició sesión (para registrar quién hizo cada cambio de existencias). */
    private Integer idUsuarioActual() {
        return Sesion.getUsuarioActual() == null ? null : Sesion.getUsuarioActual().getIdUsuario();
    }
}
