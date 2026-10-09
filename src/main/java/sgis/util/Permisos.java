package sgis.util;

import sgis.modelo.Rol;
import sgis.modelo.Usuario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Define qué puede hacer cada rol.
 *
 * Los roles se guardan en la base de datos (tabla ROL) y el Administrador puede crear otros desde
 * Configuración > Roles. Cada rol indica a qué pantallas puede entrar y si puede editar y borrar.
 *
 * Reglas fijas, que ningún rol puede cambiar:
 * - El rol "Administrador" tiene acceso total, incluida Configuración, y es el único que la ve.
 * - Todos los roles pueden entrar al Panel Principal.
 *
 * Esta clase no conoce la base de datos: quien arranca la aplicación le entrega, con configurar(),
 * la forma de leer los roles. Si no se pueden leer (por ejemplo, falta ejecutar crear_roles.sql),
 * se usan los dos roles predeterminados para que la aplicación siga funcionando.
 */
public final class Permisos {

    public static final String ADMINISTRADOR = "Administrador";
    public static final String ADMIN_INVENTARIO = "Administrador de inventario";

    // Rol antiguo: solo se reconoce por si la base de datos todavía lo tiene.
    private static final String ANTIGUO_RESPONSABLE = "Responsable de la empresa";

    public enum Pantalla {
        PANEL, PRODUCTOS, VENTAS, REPORTES, USUARIOS, CONFIGURACION
    }

    private static Supplier<List<Rol>> proveedor;
    private static Map<String, Rol> cache;

    private Permisos() {
    }

    /** Indica cómo se leen los roles (se llama una vez al arrancar la aplicación). */
    public static synchronized void configurar(Supplier<List<Rol>> proveedorDeRoles) {
        proveedor = proveedorDeRoles;
        cache = null;
    }

    /** Vuelve a leer los roles la próxima vez que se necesiten (tras crear, editar o eliminar uno). */
    public static synchronized void recargar() {
        cache = null;
    }

    private static synchronized Map<String, Rol> mapaDeRoles() {
        if (cache == null) {
            Map<String, Rol> mapa = new LinkedHashMap<>();
            try {
                if (proveedor != null) {
                    for (Rol rol : proveedor.get()) {
                        mapa.put(rol.getNombre(), rol);
                    }
                }
            } catch (RuntimeException e) {
                System.out.println("No se pudieron leer los roles de la base de datos; se usan los predeterminados: "
                        + e.getMessage());
                mapa.clear();
            }
            if (mapa.isEmpty()) {
                for (Rol rol : rolesPredeterminados()) {
                    mapa.put(rol.getNombre(), rol);
                }
            }
            // El Administrador siempre existe, pase lo que pase con la base de datos.
            mapa.putIfAbsent(ADMINISTRADOR, rolesPredeterminados().get(0));
            cache = mapa;
        }
        return cache;
    }

    private static List<Rol> rolesPredeterminados() {
        List<Rol> roles = new ArrayList<>();
        roles.add(new Rol(0, ADMINISTRADOR,
                "Acceso total, incluida Configuración. Es el único que puede dar permisos y crear roles.",
                "PANEL,PRODUCTOS,VENTAS,REPORTES,USUARIOS,CONFIGURACION", true, true));
        roles.add(new Rol(0, ADMIN_INVENTARIO,
                "Atiende el día a día: productos, ventas y clientes, sin editar ni borrar.",
                "PANEL,PRODUCTOS,VENTAS,USUARIOS", false, false));
        return roles;
    }

    /**
     * Todos los roles. Si no se pueden leer de la base de datos (por ejemplo, falta la tabla ROL),
     * devuelve los dos roles predeterminados, así la lista nunca queda vacía.
     */
    public static List<Rol> listarRoles() {
        return new ArrayList<>(mapaDeRoles().values());
    }

    /** Nombres de los roles ACTIVOS (para las listas de selección: un rol desactivado no se puede asignar). */
    public static String[] roles() {
        List<String> activos = new ArrayList<>();
        for (Rol rol : mapaDeRoles().values()) {
            if (rol.isActivo()) {
                activos.add(rol.getNombre());
            }
        }
        return activos.toArray(new String[0]);
    }

    /** false solo si el rol existe y está desactivado. */
    public static boolean rolActivo(String nombre) {
        Rol rol = rolDe(nombre);
        return rol == null || rol.isActivo();
    }

    /** El rol con ese nombre, o null si no existe. */
    public static Rol rolDe(String nombre) {
        return mapaDeRoles().get(nombre);
    }

    /** true si el rol es el de Administrador (acceso total). */
    public static boolean esAdministrativo(String rol) {
        return ADMINISTRADOR.equals(rol) || ANTIGUO_RESPONSABLE.equals(rol);
    }

    public static boolean esAdministrativo(Usuario usuario) {
        return usuario != null && esAdministrativo(usuario.getRol());
    }

    /** El Administrador siempre puede; los demás roles, solo si su rol lo permite. */
    public static boolean puedeEditarYBorrar(Usuario usuario) {
        if (usuario == null) {
            return false;
        }
        if (esAdministrativo(usuario)) {
            return true;
        }
        Rol rol = rolDe(usuario.getRol());
        return rol != null && rol.isPuedeEditar();
    }

    public static boolean puedeAcceder(Usuario usuario, Pantalla pantalla) {
        if (usuario == null || usuario.getRol() == null) {
            return false;
        }
        if (esAdministrativo(usuario)) {
            return true;
        }
        if (pantalla == Pantalla.CONFIGURACION) {
            return false; // solo el Administrador
        }
        if (pantalla == Pantalla.PANEL) {
            return true;  // todos los roles entran al Panel Principal
        }
        Rol rol = rolDe(usuario.getRol());
        return rol != null && rol.permite(pantalla.name());
    }

    /** Nombre de una pantalla tal como se muestra en la aplicación. */
    public static String nombreAmigable(String pantalla) {
        switch (pantalla) {
            case "PANEL":
                return "Panel Principal";
            case "PRODUCTOS":
                return "Productos";
            case "VENTAS":
                return "Ventas";
            case "REPORTES":
                return "Reportes";
            case "USUARIOS":
                return "Usuarios (clientes)";
            case "CONFIGURACION":
                return "Configuración";
            default:
                return pantalla;
        }
    }

    /** Las pantallas de un rol con sus nombres amigables, separadas por comas. */
    public static String pantallasAmigables(Rol rol) {
        List<String> nombres = new ArrayList<>();
        for (String pantalla : rol.getPantallas()) {
            nombres.add(nombreAmigable(pantalla));
        }
        return String.join(", ", nombres);
    }
}
