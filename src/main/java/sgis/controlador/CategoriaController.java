package sgis.controlador;

import sgis.db.CategoriaDAO;
import sgis.modelo.Categoria;
import sgis.servicio.AuditoriaService;
import sgis.util.Sesion;

import java.sql.SQLException;
import java.util.List;

/**
 * Controlador de la administración de categorías de productos.
 * Los métodos que modifican datos devuelven null si salió bien, o un mensaje de error.
 */
public class CategoriaController {

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    public List<Categoria> obtenerCategoriasConConteo() {
        try {
            return categoriaDAO.listarConConteo();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar las categorías: " + e.getMessage(), e);
        }
    }

    public String crear(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "El nombre de la categoría es obligatorio.";
        }
        try {
            if (categoriaDAO.existeNombre(nombre.trim(), -1)) {
                return "Ya existe una categoría con ese nombre.";
            }
            categoriaDAO.registrar(nombre.trim());
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Creó categoría", nombre.trim());
            return null;
        } catch (SQLException e) {
            return "Error al crear la categoría: " + e.getMessage();
        }
    }

    public String renombrar(Categoria categoria, String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            return "El nombre de la categoría es obligatorio.";
        }
        try {
            if (categoriaDAO.existeNombre(nuevoNombre.trim(), categoria.getIdCategoria())) {
                return "Ya existe una categoría con ese nombre.";
            }
            categoriaDAO.renombrar(categoria.getIdCategoria(), nuevoNombre.trim());
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Renombró categoría",
                    categoria.getNombre() + " → " + nuevoNombre.trim());
            return null;
        } catch (SQLException e) {
            return "Error al renombrar la categoría: " + e.getMessage();
        }
    }

    public String eliminar(Categoria categoria) {
        if (categoria.getCantidadProductos() > 0) {
            return "No se puede eliminar: la categoría tiene " + categoria.getCantidadProductos()
                    + " producto(s). Cámbialos de categoría primero.";
        }
        try {
            categoriaDAO.eliminar(categoria.getIdCategoria());
            AuditoriaService.registrar(Sesion.getUsuarioActual(), "Eliminó categoría", categoria.getNombre());
            return null;
        } catch (SQLException e) {
            return "Error al eliminar la categoría: " + e.getMessage();
        }
    }
}
