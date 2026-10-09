package sgis.modelo;

/**
 * Clase de modelo que representa una categoría de productos.
 * Corresponde 1:1 con la tabla CATEGORIA de la base de datos.
 */
public class Categoria {

    private int idCategoria;
    private String nombre;
    private int cantidadProductos; // solo informativo (no es una columna de la tabla)

    public Categoria() {
    }

    public Categoria(int idCategoria, String nombre) {
        this.idCategoria = idCategoria;
        this.nombre = nombre;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCantidadProductos() {
        return cantidadProductos;
    }

    public void setCantidadProductos(int cantidadProductos) {
        this.cantidadProductos = cantidadProductos;
    }

    @Override
    public String toString() {
        return nombre; // para que se vea bien en un ComboBox
    }
}
