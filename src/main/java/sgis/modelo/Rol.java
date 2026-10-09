package sgis.modelo;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Un rol de usuario: define a qué pantallas puede entrar y si puede editar y borrar.
 * Corresponde a la tabla ROL.
 */
public class Rol {

    private int idRol;
    private String nombre;
    private String descripcion;
    private final Set<String> pantallas = new LinkedHashSet<>(); // nombres de pantalla: PANEL, PRODUCTOS...
    private boolean puedeEditar;
    private boolean esSistema; // rol protegido: no se edita, ni se desactiva, ni se elimina
    private boolean activo = true; // un rol desactivado no se puede asignar y sus usuarios no pueden entrar

    public Rol() {
    }

    public Rol(int idRol, String nombre, String descripcion, String pantallasTexto,
               boolean puedeEditar, boolean esSistema) {
        this(idRol, nombre, descripcion, pantallasTexto, puedeEditar, esSistema, true);
    }

    public Rol(int idRol, String nombre, String descripcion, String pantallasTexto,
               boolean puedeEditar, boolean esSistema, boolean activo) {
        this.activo = activo;
        this.idRol = idRol;
        this.nombre = nombre;
        this.descripcion = descripcion;
        setPantallasTexto(pantallasTexto);
        this.puedeEditar = puedeEditar;
        this.esSistema = esSistema;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Set<String> getPantallas() {
        return pantallas;
    }

    public void setPantallas(Set<String> nuevas) {
        pantallas.clear();
        if (nuevas != null) {
            pantallas.addAll(nuevas);
        }
    }

    /** Las pantallas separadas por comas, tal como se guardan en la base de datos. */
    public String getPantallasTexto() {
        return String.join(",", pantallas);
    }

    public final void setPantallasTexto(String texto) {
        pantallas.clear();
        if (texto != null) {
            for (String pantalla : texto.split(",")) {
                String limpia = pantalla.trim();
                if (!limpia.isEmpty()) {
                    pantallas.add(limpia);
                }
            }
        }
    }

    public boolean permite(String pantalla) {
        return pantallas.contains(pantalla);
    }

    public boolean isPuedeEditar() {
        return puedeEditar;
    }

    public void setPuedeEditar(boolean puedeEditar) {
        this.puedeEditar = puedeEditar;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isEsSistema() {
        return esSistema;
    }

    public void setEsSistema(boolean esSistema) {
        this.esSistema = esSistema;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
