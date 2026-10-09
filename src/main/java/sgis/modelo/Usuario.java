package sgis.modelo;

/**
 * Clase de modelo que representa un usuario del sistema.
 * Corresponde 1:1 con la tabla USUARIO de la base de datos.
 */
public class Usuario {

    private int idUsuario;
    private String nombre;
    private String rol; // Administrador o Administrador de inventario
    private String usuarioLogin;
    private String contrasena; // guarda el HASH, nunca la contraseña real
    private String estado;
    private boolean protegido; // usuario "anclado": nadie puede editarlo, desactivarlo ni eliminarlo

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombre, String rol, String usuarioLogin,
                    String contrasena, String estado) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.rol = rol;
        this.usuarioLogin = usuarioLogin;
        this.contrasena = contrasena;
        this.estado = estado;
    }

    public boolean isProtegido() {
        return protegido;
    }

    public void setProtegido(boolean protegido) {
        this.protegido = protegido;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getUsuarioLogin() {
        return usuarioLogin;
    }

    public void setUsuarioLogin(String usuarioLogin) {
        this.usuarioLogin = usuarioLogin;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
