package sgis.util;

/** Los datos del cliente de una venta no son consistentes (por ejemplo, la cédula ya pertenece a otra persona). */
public class DatosClienteException extends Exception {
    public DatosClienteException(String mensaje) {
        super(mensaje);
    }
}
