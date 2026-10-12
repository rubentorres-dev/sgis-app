# Administrador de ventas (SGIS)

Aplicación de escritorio para la gestión de inventario y ventas del **Supermercado Abasto La Barata**.
Desarrollada por **Atlas Corporation** (Medellín, Colombia) como proyecto de Ingeniería de Software.

- **Cliente:** Supermercado Abasto La Barata
- **Equipo:** Rubén Torres (líder) y Daniel Miguel Pardo Gómez

## Qué hace

- **Productos** con categorías y niveles de stock (agotado, bajo, medio y suficiente).
- **Ventas** con carrito, método de pago, autocompletado de clientes, comprobante y factura en PDF
  (documento informativo, **sin validez fiscal ante la DIAN**).
- **Comprobantes** consultables en cualquier momento, con estado: pendiente, enviado, fallido o completado.
- **Reportes:** indicadores, historial de movimientos (ventas e inventario), ventas de la semana,
  informe de ventas en PDF y respaldo de la base de datos.
- **Configuración** (solo Administrador): usuarios del sistema, roles, clientes, productos, categorías
  y auditoría de quién tocó el sistema.
- **Roles configurables:** el Administrador crea roles y elige a qué pantallas puede entrar cada uno y
  si puede editar y borrar.
- **Seguridad:** contraseñas con SHA-256, bloqueo tras 5 intentos fallidos, registro de auditoría y un
  usuario propietario protegido que nadie puede editar, desactivar ni eliminar.
- **Respaldo automático** de la base de datos cada hora.

## Tecnologías

Java 17+ · JavaFX 21 · MySQL (XAMPP) · Maven · OpenPDF · Jakarta Mail

## Estructura del código

| Paquete | Contenido |
|---|---|
| `sgis` | Arranque de la aplicación (`Main`, `App`) |
| `sgis.controlador` | Lógica de cada módulo |
| `sgis.db` | Acceso a la base de datos (conexión y consultas) |
| `sgis.modelo` | Clases de datos (Producto, Venta, Usuario, Rol...) |
| `sgis.servicio` | Comprobantes, facturas, correo, respaldos y auditoría |
| `sgis.util` | Permisos, formatos, seguridad y utilidades |
| `sgis.vista` | Pantallas y su lógica |

Las pantallas (FXML), estilos (CSS) e imágenes están en `src/main/resources`.

## Cómo ejecutarlo

### 1. Requisitos
- JDK 17 o superior (se probó con JDK 19)
- Maven (el que trae NetBeans sirve)
- XAMPP, solo con **MySQL** encendido (Apache únicamente se necesita para phpMyAdmin)

### 2. Base de datos
En phpMyAdmin (pestaña **SQL**), ejecuta los scripts de la carpeta `base_de_datos/` **en este orden**:

1. `sgis_schema.sql` (crea la base `sgis_db`, las tablas y datos de prueba)
2. `actualizar_password.sql` (deja la contraseña inicial del usuario `admin`)
3. `actualizar_metodo_pago.sql`
4. `actualizar_usuario_en_ventas.sql`
5. `actualizar_roles.sql`
6. `actualizar_estado_comprobante.sql`
7. `actualizar_contrasena_visible.sql`
8. `crear_auditoria.sql`
9. `crear_roles.sql`
10. `proteger_usuario_admin.sql` (deja protegido al usuario `admin`: nadie puede editarlo, desactivarlo ni eliminarlo)
11. `agregar_codigo_recuperacion.sql` (agrega la columna del código de recuperación de 6 dígitos)
12. `agregar_codigo_visible.sql` (copia cifrada del código, para que los Administradores lo vean en Configuración)

Script de EMERGENCIA (no forma parte de la instalación): `desbloquear_sistema.sql` desbloquea el sistema si queda bloqueado por un ataque de fuerza bruta y ningún administrador puede hacerlo desde la pantalla del candado.

Usuario inicial: `admin`. **Cambia su contraseña** en cuanto entres (Configuración > Usuarios del sistema).

La conexión usa `root` sin contraseña, que es lo habitual en XAMPP. Si la tuya es distinta,
ajústala en `src/main/java/sgis/db/ConexionBD.java`.

### 3. Abrir en NetBeans
Abre la carpeta como proyecto Maven y ejecuta **Clean and Build**; la clase principal es `sgis.Main`.

### 4. Instalar como programa de escritorio (Windows)
Con la carpeta del proyecto completa, ejecuta `empaquetar.bat`. Compila, instala la aplicación y deja un
acceso directo en el Escritorio. Para usarla: enciende MySQL en XAMPP y abre **Administrador de ventas**.
Los detalles y la solución de problemas están en `LEEME_empaquetado.txt`.

## Configuración pendiente

- **Correo de comprobantes:** las credenciales SMTP de `EmailService.java` son de ejemplo. Antes de
  usar el envío real hay que configurarlas. **Nunca subas credenciales reales al repositorio.**

## Notas

- Los comprobantes y facturas generados son informativos y **no reemplazan** una factura electrónica
  con validez ante la DIAN.
- Proyecto académico desarrollado bajo contrato de servicios con el cliente.
