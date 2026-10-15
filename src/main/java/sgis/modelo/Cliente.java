package sgis.modelo;

/**
 * Clase de modelo que representa un cliente que realiza una compra en caja.
 * Corresponde 1:1 con la tabla CLIENTE de la base de datos.
 */
public class Cliente {

    private int idCliente;
    private String nombre;
    private String correo;
    private String cedula; // solo dígitos; puede ser null en clientes anteriores a esta función
    private String telefono; // solo dígitos; puede ser null en clientes anteriores a esta función

    public Cliente() {
    }

    public Cliente(int idCliente, String nombre, String correo) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.correo = correo;
    }

    public Cliente(int idCliente, String nombre, String correo, String cedula) {
        this(idCliente, nombre, correo);
        this.cedula = cedula;
    }

    public Cliente(int idCliente, String nombre, String correo, String cedula, String telefono) {
        this(idCliente, nombre, correo, cedula);
        this.telefono = telefono;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
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
