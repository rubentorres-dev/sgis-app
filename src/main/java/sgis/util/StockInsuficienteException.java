package sgis.util;

/**
 * Excepción de negocio: se lanza cuando se intenta registrar una salida
 * de producto por una cantidad mayor a la disponible en inventario.
 * Corresponde al "Flujo alternativo" del caso de uso CU-03
 * (Registrar salida de productos).
 */
public class StockInsuficienteException extends Exception {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
