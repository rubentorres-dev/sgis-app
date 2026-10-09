package sgis;

import javafx.application.Application;
import sgis.util.Registro;

/**
 * Punto de entrada real de la aplicación.
 * Esta clase NO hereda de javafx.application.Application a propósito:
 * solo delega el arranque a App.java. Esto evita el error
 * "JavaFX runtime components are missing" cuando el proyecto se
 * ejecuta con java -classpath (como hace NetBeans con "Run File").
 */
public class Main {
    public static void main(String[] args) {
        Registro.iniciar(); // lo primero: así cualquier error queda guardado en el archivo de registro
        Application.launch(App.class, args);
    }
}


