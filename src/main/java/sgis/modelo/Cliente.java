package sgis.modelo;

/**
 * Clase de modelo que representa un cliente que realiza una compra en caja.
 * Corresponde 1:1 con la tabla CLIENTE de la base de datos.
 */
public class Cliente {

    private int idCliente;
    private String nombre;
    private String correo;

    public Cliente() {
    }

    public Cliente(int idCliente, String nombre, String correo) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.correo = correo;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
