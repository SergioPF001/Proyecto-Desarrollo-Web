# Explicación Técnica Detallada — Cine La Estación

Este documento explica cada clase Java del proyecto, su propósito, por qué existe, qué hace internamente y por qué el código está escrito de esa manera.

---

## 1. PUNTO DE ENTRADA — `CineApp.java`

Es la clase más corta del proyecto (12 líneas) y la más importante: es el punto de entrada de toda la aplicación.

**¿Qué hace `@SpringBootApplication`?**
Esta anotación agrupa tres anotaciones en una:
- `@SpringBootConfiguration` — declara esta clase como fuente de configuración.
- `@EnableAutoConfiguration` — le dice a Spring Boot que configure automáticamente todo lo que encuentre en el classpath (detecta MySQL, JPA, Thymeleaf y los configura sin código manual).
- `@ComponentScan` — escanea todos los paquetes bajo `com.amsuno` buscando `@Controller`, `@Service`, `@Repository`, `@Component`, etc., y los registra como beans gestionados.

**¿Por qué solo tiene `main`?**
Spring Boot sigue el principio de "convención sobre configuración". Con una sola anotación el framework hace todo el trabajo de arranque: crea el servidor Tomcat embebido, conecta con MySQL, inicializa Hibernate (JPA), registra los controladores y levanta el sistema en el puerto 8080.

---

## 2. INICIALIZACIÓN DE DATOS — `DataLoader.java`

Implementa `CommandLineRunner`, una interfaz de Spring Boot que ejecuta automáticamente el método `run()` justo después de que la aplicación arranca.

**¿Por qué existe esta clase?** La base de datos empieza vacía. Sin datos de demostración sería imposible probar el sistema. Esta clase crea salas, asientos, películas, clientes y reservas de ejemplo automáticamente.

**Flujo de ejecución del método `run()`:**
1. Siempre llama a `inicializarSalas()`
2. Si ya hay clientes: llama a `repararAsientosDemoData()` y termina
3. Si no hay clientes: llama a `inicializarDemostracion()` (carga completa)

**`inicializarSalas()`** — Crea dos salas si no existen:
- Sala Principal: 5 filas × 8 columnas = 40 asientos. Filas C y D "preferencial", el resto "normal".
- Sala VIP: 4 filas × 6 columnas = 24 asientos. Todos "vip".

**`inicializarDemostracion()`** — Solo se ejecuta una vez (base vacía). Crea 3 clientes, 4 películas y 3 reservas completas con sus registros en `reserva_asiento`.

**`repararAsientosDemoData()`** — Si el sistema ya tiene datos pero alguna reserva le falta registros en `reserva_asiento` (migración incompleta), los regenera automáticamente.

---

## 3. PROCEDIMIENTOS ALMACENADOS — `ProcedimientosLoader.java`

`@Component` con `@EventListener(ApplicationReadyEvent.class)`. Su método `crearProcedimientos()` se ejecuta cuando Spring Boot termina de arrancar completamente.

**¿Por qué se re-crean los procedimientos en cada arranque?**
Los procedimientos almacenados viven en MySQL, no en Java. Si se modifica la lógica en el código Java y no se re-crea en la base de datos, el sistema usaría la versión antigua. Con `DROP PROCEDURE IF EXISTS` + `CREATE PROCEDURE` en cada arranque, MySQL siempre tiene la versión actualizada sin scripts SQL manuales.

### `sp_AsientosDisponibles(p_pelicula_id, p_fecha)`

Devuelve todos los asientos de la sala de una película indicando si están ocupados o disponibles para esa fecha.

**¿Por qué como procedimiento y no como consulta Java?** La consulta necesita 4 tablas: pelicula → sala → asiento → reserva_asiento → reserva. El estado de cada asiento depende de una subconsulta `EXISTS` que verifica reservas activas. Esta lógica es más eficiente en el motor SQL que trayendo todos los datos a Java — MySQL tiene acceso directo a los índices y puede optimizar internamente.

**Lógica:** Para cada asiento de la sala, ¿existe alguna reserva activa (no 'Cancelada') que incluya este asiento para esta película y fecha? SÍ → 'ocupado', NO → 'disponible'.

### `sp_CrearReserva(..., OUT p_reserva_id)`

Inserta una fila en `reserva` y devuelve el ID generado.

**¿Por qué parámetro OUT?** Con el parámetro `OUT`, MySQL usa `LAST_INSERT_ID()` para capturar el ID de la fila recién insertada. Esto garantiza que incluso con múltiples usuarios creando reservas simultáneamente, cada uno recibe el ID correcto de su propia reserva. En Java se llama con `CallableStatement` registrando el parámetro de salida como `Types.BIGINT`.

### `sp_CancelarReserva(p_reserva_id)`

Elimina los registros de `reserva_asiento` y cambia el estado de la reserva a 'Cancelada'.

**¿Por qué dos operaciones en un procedimiento?** Si solo se actualiza el estado, los asientos seguirán apareciendo como ocupados. Si solo se eliminan los registros de `reserva_asiento`, la reserva queda inconsistente. El procedimiento garantiza que ambas operaciones ocurran juntas.

### `sp_ResumenSala(p_sala_id, p_fecha)`

Calcula cuántos asientos totales, ocupados y disponibles tiene una sala para una fecha dada. Creado para futuras métricas del dashboard.

---

## 4. LOS MODELOS (ENTIDADES) — 7 clases en `model/`

Los modelos son clases Java que representan tablas de la base de datos. Hibernate crea las tablas automáticamente gracias a `ddl-auto=update`.

### `Sala.java`
`Tabla: sala | Campos: id, nombre, filas, columnas`

Representa una sala física. No tiene referencia hacia sus asientos ni películas — esas entidades apuntan hacia Sala, no al revés. Sala es independiente. `GenerationType.IDENTITY` usa AUTO_INCREMENT de MySQL, delegando la generación del ID al motor de base de datos.

### `Asiento.java`
`Tabla: asiento | Campos: id, sala_id (FK), fila, numero, tipo`

Representa un asiento físico inamovible. **No tiene campo "estado"** — el estado de ocupación depende de la fecha y la película, no es un atributo fijo. Lo calcula `sp_AsientosDisponibles` dinámicamente. `@ManyToOne` + `@JoinColumn(name="sala_id")` crea la clave foránea hacia Sala.

### `Pelicula.java`
`Tabla: pelicula | Campos: id, titulo, genero, duracion, clasificacion, precio, esEstreno, horarios, sala_id (FK)`

El campo `horarios` almacena todos los horarios como string separado por comas ("14:00,17:30,21:00"). Simplifica el modelo evitando una tabla separada. Cuando se necesitan los horarios individuales se hace `horarios.split(",")`.

### `Cliente.java`
`Tabla: cliente | Campos: id, nombre, email, telefono`

Sin contraseña. Los clientes se identifican por email. Si ya existe uno con ese email al hacer una reserva, se reutiliza; si no, se crea automáticamente.

### `Reserva.java`
`Tabla: reserva | Campos: id, cliente_id (FK), pelicula_id (FK), fecha, asientos, total, estado`

Entidad central del sistema. Une un cliente con una película en una fecha y hora.

`@OneToMany(cascade = CascadeType.REMOVE)` — Al eliminar una Reserva, JPA elimina automáticamente sus registros en `reserva_asiento`. Evita registros huérfanos.

El campo `asientos` (int) guarda cuántos asientos se reservaron (el detalle de cuáles está en `reserva_asiento`). Permite consultar el resumen rápido sin JOIN extra.

### `ReservaAsiento.java`
`Tabla: reserva_asiento | Campos: id, reserva_id (FK), asiento_id (FK)`

Tabla intermedia que resuelve la relación muchos-a-muchos entre Reserva y Asiento.

**¿Por qué no usar `@ManyToMany` directamente?** `@ManyToMany` crea la tabla intermedia automáticamente, pero esa tabla no tiene ID propio ni se puede consultar directamente con JPQL. Al crear `ReservaAsiento` como entidad explícita, se pueden escribir consultas propias sobre ella (como `findEtiquetasByReservaId`).

### `Snack.java`
`Tabla: snack | Campos: id, nombre, categoria, precio, stock, descripcion`

Entidad independiente sin relaciones con otras tablas. Gestión del snack bar completamente separada del flujo de reservas.

---

## 5. LOS REPOSITORIOS — 7 interfaces en `repository/`

Interfaces que extienden `JpaRepository`. Spring Data JPA genera la implementación en tiempo de ejecución. El desarrollador no escribe SQL ni código de conexión.

`JpaRepository` provee automáticamente: `findAll()`, `findById()`, `save()`, `deleteById()`, `count()`.

**¿Por qué 7 repositorios?** Uno por cada entidad. No se puede buscar un asiento usando el repositorio de reservas.

**`ClienteRepository`** — Añade `findByEmail(String email)`. Spring genera `SELECT * FROM cliente WHERE email = ?` solo por el nombre del método (query method). Se usa en `PublicController` para reutilizar clientes por email.

**`ReservaAsientoRepository`** — El más especializado. Añade:
- `findByReservaId` y `deleteByReservaId` — query methods automáticos
- `findEtiquetasByReservaId` con `@Query` JPQL — construye etiquetas "A1", "B5" concatenando fila+numero ordenadas, para mostrar "A1, A2, B5" en la tabla de reservas del admin

**`AsientoRepository`** — Añade `findBySalaId(Long salaId)`. Usado en `DataLoader`.

Los repositorios de `Pelicula`, `Sala`, `Reserva` y `Snack` solo extienden `JpaRepository` sin métodos adicionales.

---

## 6. LOS SERVICIOS — 6 clases en `service/`

Contienen la lógica del negocio. Los controladores siempre pasan por el servicio, nunca acceden directamente al repositorio.

**¿Por qué 6 servicios?** Uno por cada entidad principal. `AsientoService` está separado porque su lógica es la más compleja (procedimientos almacenados + múltiples repositorios).

**`ClienteService.java`** — CRUD estándar: listar, agregar, buscar, eliminar.

**`PeliculaService.java`** — CRUD más `listarGeneros()` que obtiene géneros distintos ordenados alfabéticamente. Usado en el filtro dropdown de reservas del admin.

**`ReservaService.java`** — CRUD más `confirmar(id)` que cambia estado de Pendiente a Confirmada. Al eliminar, `CascadeType.REMOVE` borra también los `ReservaAsiento`.

**`SalaService.java`** — Solo listar, buscar, agregar. Sin eliminar porque sería una operación destructiva en cascada (sala → asientos → películas → reservas).

**`SnackService.java`** — CRUD completo estándar.

**`AsientoService.java`** — El más complejo. Inyecta 4 dependencias:
- `JdbcTemplate`: para procedimientos almacenados con SQL puro
- `AsientoRepository`: buscar asientos por ID
- `ReservaAsientoRepository`: guardar y consultar asientos de reserva
- `ReservaRepository`: obtener objeto Reserva

`asientosDisponibles(peliculaId, fecha)` — Llama a `sp_AsientosDisponibles`, devuelve `List<Map<String, Object>>` donde cada mapa es una fila SQL (id, fila, numero, tipo, estado). Estructura flexible que se pasa directamente a Thymeleaf.

`crearReserva(...)` — Llama a `sp_CrearReserva` con `CallableStatement` de JDBC puro. Necesita `jdbc.execute((Connection con) -> {...})` para obtener la conexión raw y registrar el parámetro OUT.

`guardarAsientosReserva(reservaId, asientoIds)` — Crea un `ReservaAsiento` por cada asiento seleccionado. `@Transactional` garantiza atomicidad: si falla al guardar alguno, todos los anteriores se deshacen.

`etiquetasAsientos(reservaId)` — Obtiene etiquetas "A1, A2, B5" vía JPQL. Devuelve "-" si no hay asientos.

---

## 7. EL DTO — `ReservaDTO.java`

Data Transfer Object: clase que transporta datos entre capas sin ser una entidad de base de datos.

**¿Por qué existe si ya existe `Reserva`?** `Reserva` es una entidad JPA con lazy loading. Acceder a `reserva.cliente.nombre` puede disparar consultas SQL adicionales. Con 10 reservas en pantalla, podrían ejecutarse 30 consultas extras automáticamente.

`ReservaDTO` aplana todo en una sola clase construida de una vez:
- `clienteNombre`: extraído de la entidad Cliente
- `peliculaTitulo`: extraído de la entidad Pelicula
- `asientosDetalle`: "A1, A2, B5" — no existe en ninguna entidad, se calcula combinando `ReservaAsiento` + `Asiento`

---

## 8. LOS CONTROLADORES — 4 clases en `controller/`

**`HomeController.java`** — Maneja `/`. Pasa a `index.html` la lista de películas y URLs de imágenes. Solo inyecta `PeliculaService` porque es el único dato dinámico de la página de inicio.

**`LoginController.java`** — Credenciales hardcoded en un `Map` con dos usuarios fijos (admin y cajero). Al login exitoso guarda 4 atributos en la sesión HTTP: `loggedIn`, `userRole`, `userName`, `userNombre`. `sesion.invalidate()` elimina completamente la sesión al cerrar.

**¿Por qué no Spring Security?** Añadiría complejidad (BCrypt, configuración de roles, CSRF) que va más allá del alcance del proyecto. La protección manual con sesiones HTTP es completamente funcional.

**`AdminController.java`** — 18 métodos bajo `/admin/**`. Patrón de seguridad en todos:
```
if (sinSesion(sesion))  return "redirect:/login"       // todas las rutas
if (sinPermiso(sesion)) return "redirect:/admin/dashboard" // solo rutas ADMIN
```

Métodos privados de utilidad: `crearDTO(r)` evita repetir la conversión Reserva→ReservaDTO. `parsearIds(ids)` convierte "12,15,18" en `List<Long>` para procesar la selección del mapa visual.

**Tabla de rutas del AdminController:**

| Ruta | Acceso | Acción |
|---|---|---|
| GET /admin/dashboard | Todos | KPIs y últimas reservas |
| GET /admin/reservas | Todos | Lista con búsqueda y filtro |
| GET /admin/asientos | Todos | Mapa visual de asientos |
| POST /admin/reservas/nueva | Todos | Crea reserva con asientos |
| GET /admin/reservas/confirmar/{id} | Todos | Cambia estado a Confirmada |
| GET /admin/reservas/cancelar/{id} | Todos | Cancela reserva (libera asientos) |
| GET /admin/reservas/eliminar/{id} | Todos | Elimina reserva definitivamente |
| GET /admin/peliculas | Todos | Lista películas |
| POST /admin/peliculas/agregar | Todos | Crea película nueva |
| GET /admin/peliculas/eliminar/{id} | Todos | Elimina película |
| GET /admin/clientes | Todos | Lista clientes con estadísticas |
| POST /admin/clientes/agregar | Todos | Crea cliente nuevo |
| GET /admin/clientes/eliminar/{id} | Todos | Elimina cliente |
| GET /admin/snacks | Todos | Lista snacks |
| POST /admin/snacks/agregar | Todos | Crea snack nuevo |
| GET /admin/snacks/eliminar/{id} | Todos | Elimina snack |
| GET /admin/estadisticas | Solo ADMIN | Estadísticas y gráficos |
| GET /admin/graficos | Solo ADMIN | Gráficos adicionales |
| GET /admin/configuracion | Solo ADMIN | Datos del cine |
| GET /admin/cerrar-sesion | Todos | Invalida sesión |

**`PublicController.java`** — 3 estados en una sola URL `/reservar` según parámetros:
1. Sin params → grilla de películas
2. `peliculaId` → detalles + selector fecha/horario
3. `peliculaId` + `fecha` → datos personales + mapa de asientos

POST: usa `clienteRepo.findByEmail(email).orElseGet(...)` para reutilizar o crear cliente automáticamente por email, sin que el usuario tenga que registrarse explícitamente.

---

## 9. RESTRICCIÓN DE THYMELEAF 3.1

`th:onclick` con expresiones de string está bloqueado por seguridad (previene XSS). Solución usada en los mapas de asientos:

```html
<!-- INCORRECTO — lanza error en Thymeleaf 3.1 -->
<div th:onclick="'toggleAsiento(' + ${asiento.id} + ')'">

<!-- CORRECTO — data attributes + handler estático -->
<div th:data-id="${asiento.id}"
     th:data-precio="${pelicula.precio}"
     onclick="handleSeat(this)">
```

JavaScript lee con `btn.dataset.id` y `btn.dataset.precio`. Lo mismo aplica para botones de horario: `th:data-horario` + `onclick="seleccionarHorario(this.dataset.horario)"`.

---

## 10. CONFIGURACIÓN — `application.properties`

| Propiedad | Valor | Por qué |
|---|---|---|
| `server.port` | 8080 | Puerto estándar de desarrollo |
| `ddl-auto` | update | Hibernate crea/actualiza tablas al arrancar sin borrar datos |
| `show-sql` | false | El log era muy verboso con procedimientos almacenados |
| `open-in-view` | false | Cierra la conexión BD antes de renderizar. Buena práctica de producción |
| `allowPublicKeyRetrieval` | true | Necesario para autenticación `caching_sha2_password` de MySQL 8 |

---

## 11. SISTEMA DE ROLES — 3 niveles de seguridad

**Nivel 1 — Al hacer login** (`LoginController`): Se guarda el rol en la sesión del navegador.

**Nivel 2 — En el menú lateral** (`sidebar.html`): Los ítems de Estadísticas, Gráficos y Configuración solo se renderizan si `session.userRole == 'ADMIN'`. El Cajero directamente no los ve.

**Nivel 3 — En las rutas del servidor** (`AdminController`): Si el Cajero intenta acceder a la URL directamente, el controlador verifica el rol con `sinPermiso()` y lo redirige al dashboard con mensaje de acceso denegado.

| Sección | Administrador | Cajero |
|---|---|---|
| Dashboard, Reservas, Películas, Clientes, Snacks | Sí | Sí |
| Estadísticas, Gráficos, Configuración | Sí | No |

---

## 12. RESUMEN COMPLETO DE CONTEOS

| Capa | Cantidad | Clases / Archivos |
|---|---|---|
| Punto de entrada | 1 | CineApp |
| Inicialización | 2 | DataLoader, ProcedimientosLoader |
| Modelos / Entidades | 7 | Sala, Asiento, Pelicula, Cliente, Reserva, ReservaAsiento, Snack |
| Repositorios | 7 | SalaRepository, AsientoRepository, PeliculaRepository, ClienteRepository, ReservaRepository, ReservaAsientoRepository, SnackRepository |
| Servicios | 6 | SalaService, AsientoService, PeliculaService, ClienteService, ReservaService, SnackService |
| Controladores | 4 | HomeController, LoginController, AdminController, PublicController |
| DTOs | 1 | ReservaDTO |
| **Total clases Java** | **28** | |
| Procedimientos SQL | 4 | sp_AsientosDisponibles, sp_CrearReserva, sp_CancelarReserva, sp_ResumenSala |
| Templates Thymeleaf | 13 | index, login, reservar, sidebar, dashboard, peliculas, clientes, reservas, asientos, snacks, estadisticas, graficos, configuracion |
| Tablas en BD | 7 | sala, asiento, pelicula, cliente, reserva, reserva_asiento, snack |

- **Sitio público** (`/`): La página principal que ven los clientes con la cartelera, servicios y contacto.
- **Panel de administración** (`/admin/...`): Un panel interno al que solo pueden acceder usuarios con usuario y contraseña. Desde ahí se gestionan reservas, películas, clientes y snacks.

La aplicación fue construida con **Spring Boot** (Java) en el backend y **Thymeleaf + Bootstrap** en el frontend. No usa base de datos: todos los datos viven en memoria mientras el servidor está corriendo.

---

## Cómo está organizado el proyecto

```
src/
 └── main/
      ├── java/com/amsuno/
      │    ├── CineApp.java                 ← Punto de arranque
      │    ├── model/                       ← Las entidades (datos)
      │    │    ├── Pelicula.java
      │    │    ├── Cliente.java
      │    │    ├── Reserva.java
      │    │    └── Snack.java
      │    ├── service/                     ← La lógica del negocio
      │    │    ├── PeliculaService.java
      │    │    ├── ClienteService.java
      │    │    ├── ReservaService.java
      │    │    └── SnackService.java
      │    └── controller/                  ← Los que reciben peticiones web
      │         ├── HomeController.java
      │         ├── LoginController.java
      │         └── AdminController.java
      └── resources/
           └── templates/                   ← Las páginas HTML
                ├── index.html
                ├── login.html
                ├── fragments/sidebar.html
                └── admin/
                     ├── dashboard.html
                     ├── reservas.html
                     ├── peliculas.html
                     ├── clientes.html
                     ├── snacks.html
                     ├── estadisticas.html
                     ├── graficos.html
                     └── configuracion.html
```

---

## Archivos Java

---

### `CineApp.java` — El punto de arranque

```java
@SpringBootApplication
public class CineApp {
    public static void main(String[] args) {
        SpringApplication.run(CineApp.class, args);
    }
}
```

Este es el archivo más simple pero el más importante: **es el que arranca todo el sistema**.

- `@SpringBootApplication`: Le dice a Spring que este es el punto de entrada de la aplicación. Activa el escaneo automático de todos los controladores y servicios.
- `main(...)`: Es el método que ejecuta Java cuando se corre el programa.
- `SpringApplication.run(...)`: Arranca el servidor web embebido (Tomcat) en el puerto 8080 y deja la app escuchando peticiones.

Sin este archivo, la aplicación no existiría.

---

### Modelos (`model/`) — Los datos del negocio

Los modelos son clases que representan las entidades reales del cine. Son simples contenedores de datos: tienen atributos (variables) y métodos para leer y escribir esos atributos (getters y setters).

---

#### `Pelicula.java`

Representa una película en cartelera.

```java
private Long id;           // Número identificador único
private String titulo;     // Nombre de la película
private String genero;     // Tipo: Acción, Drama, etc.
private int duracion;      // Duración en minutos
private String clasificacion; // PG-13, G, R, etc.
private double precio;     // Precio de la entrada en Nairas
private boolean esEstreno; // Si está marcada como estreno o no
private String horarios;   // Horarios separados por coma: "14:00,17:30,21:00"
```

Cada campo tiene un **getter** (leer) y un **setter** (escribir). Por ejemplo:
- `getTitulo()` devuelve el título de la película.
- `setTitulo("Avatar 3")` cambia el título a "Avatar 3".

El constructor con todos los parámetros permite crear una película completa de un solo golpe, como se usa en `PeliculaService` al cargar los datos iniciales.

---

#### `Cliente.java`

Representa un cliente registrado en el cine.

```java
private Long id;        // Número identificador único
private String nombre;  // Nombre completo del cliente
private String email;   // Correo electrónico
private String telefono;// Número de teléfono
```

Igual que `Pelicula`, tiene getters y setters para cada campo. Esta clase se usa para listar clientes, agregarlos y vincularlos con sus reservas.

---

#### `Reserva.java`

Representa una reserva de entradas hecha por un cliente.

```java
private Long id;              // Número identificador único
private String clienteNombre; // Nombre del cliente que reservó
private String pelicula;      // Título de la película reservada
private String fecha;         // Fecha y hora: "2026-04-18 19:00"
private int asientos;         // Cantidad de asientos reservados
private double total;         // Monto total en Nairas
private String estado;        // "Pendiente", "Confirmada" o "Cancelada"
```

El campo `estado` es clave para el flujo de trabajo: una reserva nace como "Pendiente" y el administrador la puede confirmar, cambiándola a "Confirmada".

---

#### `Snack.java`

Representa un producto del snack bar del cine.

```java
private Long id;           // Número identificador único
private String nombre;     // Nombre del producto
private String categoria;  // "Snacks", "Bebidas", "Combos", "Dulces"
private double precio;     // Precio en Nairas
private int stock;         // Cantidad disponible en inventario
private String descripcion;// Descripción breve del producto
```

---

### Servicios (`service/`) — La lógica del negocio

Los servicios son los que realmente **trabajan con los datos**. Almacenan las listas en memoria y ofrecen métodos para listar, agregar, buscar y eliminar. La anotación `@Service` le dice a Spring que los registre automáticamente para poder usarlos en los controladores.

Todos los servicios comparten el mismo patrón, por eso se explican juntos con sus diferencias.

---

#### `PeliculaService.java`

```java
private final List<Pelicula> peliculas = new ArrayList<>();
private final AtomicLong contador = new AtomicLong();
```

- `peliculas`: La lista donde se guardan todas las películas en memoria.
- `AtomicLong contador`: Un generador de IDs. Cada vez que se agrega una película, el contador sube en 1 y ese número se convierte en el ID de la nueva película. `AtomicLong` es seguro para uso concurrente.

**En el constructor** se cargan 4 películas de ejemplo (Avatar 3, Guardianes de la Galaxia Vol. 4, Aventura en el Bosque Mágico, Dune: Parte Tres). Estos son los datos que aparecen desde el primer momento que arranca la app.

**Métodos:**

| Método | Qué hace |
|---|---|
| `listar()` | Devuelve la lista completa de películas |
| `agregar(Pelicula)` | Asigna un nuevo ID y agrega la película a la lista |
| `buscar(Long id)` | Busca y devuelve una película por su ID |
| `eliminar(Long id)` | Borra de la lista la película que tenga ese ID |

---

#### `ClienteService.java`

Mismo patrón que `PeliculaService`. Carga 3 clientes de ejemplo al arrancar: Carlos Mendoza, María García y José Rodríguez.

**Métodos:** `listar()`, `agregar()`, `buscar()`, `eliminar()` — misma lógica que películas.

---

#### `ReservaService.java`

Similar, pero tiene un método adicional:

```java
public void confirmar(Long id) {
    reservas.stream()
            .filter(r -> r.getId().equals(id))
            .findFirst()
            .ifPresent(r -> r.setEstado("Confirmada"));
}
```

- Busca la reserva con ese ID dentro de la lista.
- Si la encuentra, le cambia el estado a `"Confirmada"`.
- Si no la encuentra, no hace nada.

Carga 3 reservas de ejemplo al arrancar: una confirmada, una pendiente y una confirmada.

---

#### `SnackService.java`

Mismo patrón. Carga 3 snacks de ejemplo: Palomitas Grandes, Refresco y Combo Familiar.

**Métodos:** `listar()`, `agregar()`, `buscar()`, `eliminar()`.

---

### Controladores (`controller/`) — Los que manejan las peticiones web

Los controladores son el puente entre el navegador y la lógica del sistema. Cuando alguien visita una URL, un controlador la recibe, hace lo que tiene que hacer (consultar el servicio, procesar datos) y le dice qué página HTML mostrar.

**`@GetMapping`**: Responde a peticiones GET, que son las que ocurren cuando el usuario escribe una URL o hace clic en un enlace. Se usan para **mostrar páginas**.

**`@PostMapping`**: Responde a peticiones POST, que son las que ocurren cuando el usuario envía un formulario. Se usan para **guardar o procesar datos**.

---

#### `HomeController.java`

```java
@Controller
public class HomeController {

    @GetMapping("/")
    public String inicio() {
        return "index";
    }
}
```

El controlador más simple de todos. Solo tiene una responsabilidad:

- Cuando alguien entra a la raíz del sitio (`http://localhost:8080/`), devuelve la plantilla `index.html`.
- No necesita sesión ni servicios porque el sitio público es libre para cualquiera.

---

#### `LoginController.java`

Maneja todo el proceso de autenticación: mostrar el formulario de login y validar las credenciales.

```java
private static final Map<String, String[]> USUARIOS = Map.of(
    "admin_cine",  new String[]{"LaEstacion2026!", "ADMIN",  "Administrador"},
    "cajero_cine", new String[]{"Cajero2026!",     "CAJERO", "Cajero"}
);
```

Este `Map` es el "registro de usuarios" del sistema. La clave es el nombre de usuario, y el valor es un arreglo con tres datos: `[contraseña, rol, nombre para mostrar]`. Hay dos usuarios registrados: uno Administrador y uno Cajero.

**`@GetMapping("/login")` — Mostrar el formulario:**

```java
public String mostrarLogin(HttpSession sesion) {
    if (sesion.getAttribute("loggedIn") != null) {
        return "redirect:/admin/dashboard";
    }
    return "login";
}
```

- Si el usuario ya tiene sesión activa (ya inició sesión), lo redirige directo al dashboard.
- Si no tiene sesión, muestra la página `login.html`.

**`@PostMapping("/login")` — Procesar el formulario:**

```java
public String procesarLogin(@RequestParam String usuario,
                            @RequestParam String contrasena,
                            HttpSession sesion, Model modelo) {
    String[] datos = USUARIOS.get(usuario);
    if (datos != null && datos[0].equals(contrasena)) {
        sesion.setAttribute("loggedIn",   true);
        sesion.setAttribute("userRole",   datos[1]);
        sesion.setAttribute("userName",   usuario);
        sesion.setAttribute("userNombre", datos[2]);
        return "redirect:/admin/dashboard";
    }
    modelo.addAttribute("error", "Usuario o contraseña incorrectos.");
    return "login";
}
```

- `@RequestParam String usuario` y `@RequestParam String contrasena`: captura los valores del formulario HTML.
- Busca el usuario en el `Map`. Si existe y la contraseña coincide:
  - Guarda en la sesión que está logueado (`loggedIn = true`).
  - Guarda el rol (`ADMIN` o `CAJERO`), el nombre de usuario y el nombre legible.
  - Redirige al dashboard.
- Si las credenciales son incorrectas, manda el mensaje de error de vuelta al login.

**La sesión** (`HttpSession`) es como una "memoria temporal" que el servidor tiene de cada usuario mientras navega. Se crea al iniciar sesión y se destruye al cerrarla.

---

#### `AdminController.java`

Es el controlador más grande. Maneja todas las páginas y acciones del panel de administración.

```java
@Controller
@RequestMapping("/admin")
public class AdminController {
```

- `@RequestMapping("/admin")`: Indica que todas las rutas de este controlador empiezan con `/admin`. Así `/dashboard` aquí equivale a `/admin/dashboard`.

**Inyección de dependencias:**

```java
private final PeliculaService peliculaService;
private final ClienteService clienteService;
private final ReservaService reservaService;
private final SnackService snackService;
```

Spring automáticamente proporciona (inyecta) los servicios a través del constructor. El controlador no crea los servicios, los recibe listos para usar.

**Métodos de seguridad:**

```java
private boolean sinSesion(HttpSession sesion) {
    return sesion.getAttribute("loggedIn") == null;
}

private boolean sinPermiso(HttpSession sesion) {
    return !"ADMIN".equals(sesion.getAttribute("userRole"));
}
```

- `sinSesion`: Devuelve `true` si el usuario NO está logueado. Se llama al inicio de cada ruta para protegerla.
- `sinPermiso`: Devuelve `true` si el usuario logueado NO es ADMIN. Se usa para las rutas exclusivas de administrador.

**Rutas del controlador:**

| Ruta | Tipo | Acceso | Qué hace |
|---|---|---|---|
| `/admin/dashboard` | GET | Todos | Carga y muestra el resumen general |
| `/admin/reservas` | GET | Todos | Lista reservas con búsqueda y filtro |
| `/admin/reservas/nueva` | POST | Todos | Guarda una nueva reserva |
| `/admin/reservas/confirmar/{id}` | GET | Todos | Cambia estado a "Confirmada" |
| `/admin/reservas/eliminar/{id}` | GET | Todos | Elimina la reserva |
| `/admin/peliculas` | GET | Todos | Lista las películas |
| `/admin/peliculas/agregar` | POST | Todos | Guarda una nueva película |
| `/admin/peliculas/eliminar/{id}` | GET | Todos | Elimina la película |
| `/admin/clientes` | GET | Todos | Lista clientes con sus estadísticas |
| `/admin/clientes/agregar` | POST | Todos | Guarda un nuevo cliente |
| `/admin/clientes/eliminar/{id}` | GET | Todos | Elimina el cliente |
| `/admin/snacks` | GET | Todos | Lista los snacks |
| `/admin/snacks/agregar` | POST | Todos | Guarda un nuevo snack |
| `/admin/snacks/eliminar/{id}` | GET | Todos | Elimina el snack |
| `/admin/estadisticas` | GET | Solo ADMIN | Muestra estadísticas y gráficos |
| `/admin/graficos` | GET | Solo ADMIN | Muestra gráficos alternativos |
| `/admin/configuracion` | GET | Solo ADMIN | Muestra configuración del cine |
| `/admin/cerrar-sesion` | GET | Todos | Destruye la sesión y redirige al inicio |

**Ejemplo detallado — `/admin/clientes`:**

```java
@GetMapping("/clientes")
public String clientes(HttpSession sesion, Model modelo) {
    if (sinSesion(sesion)) return "redirect:/login";

    List<Cliente> clientes = clienteService.listar();
    List<Reserva> reservas = reservaService.listar();

    Map<String, Integer> pedidosPorCliente = new HashMap<>();
    Map<String, Integer> gastadoPorCliente = new HashMap<>();

    for (Cliente c : clientes) {
        List<Reserva> misReservas = reservas.stream()
                .filter(r -> r.getClienteNombre().equals(c.getNombre()))
                .toList();

        pedidosPorCliente.put(c.getNombre(), misReservas.size());
        gastadoPorCliente.put(c.getNombre(),
            (int) misReservas.stream().mapToDouble(Reserva::getTotal).sum());
    }

    modelo.addAttribute("clientes",         clientes);
    modelo.addAttribute("cliente",           new Cliente());
    modelo.addAttribute("pedidosPorCliente", pedidosPorCliente);
    modelo.addAttribute("gastadoPorCliente", gastadoPorCliente);
    return "admin/clientes";
}
```

1. Verifica que haya sesión activa.
2. Obtiene la lista de todos los clientes y todas las reservas.
3. Para cada cliente, filtra las reservas que le pertenecen (por nombre).
4. Calcula cuántas reservas tiene y cuánto ha gastado en total.
5. Envía todos esos datos al HTML a través del `Model`.
6. Retorna la vista `admin/clientes.html`.

**El `Model`** es el mecanismo para pasar datos desde el controlador hacia el HTML. Todo lo que se agrega con `modelo.addAttribute("nombre", valor)` queda disponible en la plantilla Thymeleaf.

---

## Archivos HTML

Los archivos HTML usan **Thymeleaf**, un motor de plantillas que permite mezclar HTML normal con expresiones dinámicas del servidor. Se identifica por el prefijo `th:`.

**Conceptos clave de Thymeleaf:**
- `th:text="${variable}"` — Muestra el valor de una variable dentro de la etiqueta.
- `th:each="item : ${lista}"` — Repite el elemento HTML por cada ítem de la lista (como un for).
- `th:if="${condicion}"` — Muestra u oculta el elemento según la condición.
- `th:href="@{/ruta}"` — Genera una URL correcta para el enlace.
- `th:action="@{/ruta}"` — Define a dónde envía el formulario.
- `th:field="*{campo}"` — Vincula un campo del formulario con el campo de un objeto.
- `${session.variable}` — Lee un valor guardado en la sesión del usuario.

---

### `index.html` — Sitio público

Es la página que ven los visitantes antes de ingresar al panel. No tiene Thymeleaf dinámico porque sus datos son estáticos (la cartelera está escrita a mano en el HTML, no viene del servidor).

**Secciones que contiene:**
- **Navbar**: Barra de navegación con enlaces a las secciones y botón "Ingresar" que lleva a `/login`.
- **Ticker**: Banda animada con los nombres de las películas en cartelera, hecha con CSS.
- **Hero**: Sección principal con imagen, título y llamada a la acción.
- **Cartelera**: 4 tarjetas de películas con imagen, horarios y botón de compra.
- **Promo**: Sección destacando los martes de descuento.
- **Servicios**: 6 tarjetas describiendo los servicios del cine (3D, Snack Bar, Eventos, etc.).
- **Sobre nosotros**: Historia y estadísticas del cine.
- **Testimonios**: Opiniones de clientes.
- **Contacto**: Datos de contacto + formulario de mensaje.
- **Footer**: Pie de página con enlaces y redes sociales.

---

### `login.html` — Página de inicio de sesión

Formulario simple con dos campos (usuario y contraseña) para acceder al panel.

```html
<form th:action="@{/login}" method="post">
    <input type="text" name="usuario" ...>
    <input type="password" name="contrasena" ...>
    <button type="submit">Ingresar al Panel</button>
</form>
```

- `th:action="@{/login}"`: Al enviar el formulario, los datos van a `POST /login`, que maneja `LoginController`.
- `method="post"`: Los datos viajan en el cuerpo de la petición, no en la URL (por seguridad).
- `name="usuario"` y `name="contrasena"`: Estos nombres deben coincidir exactamente con los parámetros `@RequestParam` del controlador.

```html
<div th:if="${error}" class="alert alert-danger" th:text="${error}"></div>
```

- Solo aparece si el controlador envió un atributo `error` al modelo.
- `th:text="${error}"`: Muestra el mensaje de error dentro del div.

Al pie muestra las credenciales de ambos roles (Administrador y Cajero) como referencia visual durante el desarrollo.

---

### `fragments/sidebar.html` — La barra lateral del panel

Es un **fragmento reutilizable**: en lugar de copiar el mismo menú lateral en cada página del panel, se define una vez aquí y las demás páginas lo incluyen con una sola línea.

```html
<div th:fragment="sidebar(activo)">
```

Recibe un parámetro `activo` que indica cuál sección está seleccionada, para resaltarla en el menú.

```html
<a href="/admin/dashboard"
   th:class="${activo == 'dashboard'} ? 'admin-nav-item activo' : 'admin-nav-item'">
    Dashboard
</a>
```

- Si `activo == 'dashboard'`, le agrega la clase CSS `activo` que lo resalta visualmente.

**Control de acceso por rol:**

```html
<th:block th:if="${session.userRole == 'ADMIN'}">
    <a href="/admin/estadisticas">Estadísticas</a>
    <a href="/admin/graficos">Gráficos</a>
    <a href="/admin/configuracion">Configuración</a>
</th:block>
```

- Lee el rol del usuario directamente desde la sesión (`session.userRole`).
- Si el rol NO es `ADMIN`, estos tres enlaces simplemente no aparecen en el menú.

**Usuario dinámico en el footer del sidebar:**

```html
<div class="admin-user-avatar" th:text="${#strings.substring(session.userNombre, 0, 1)}">A</div>
<div class="admin-user-name" th:text="${session.userNombre}">Administrador</div>
<div class="admin-user-role" th:text="${session.userName}">admin_cine</div>
```

- Muestra la primera letra del nombre como avatar.
- Muestra el nombre legible (`Administrador` o `Cajero`) y el nombre de usuario de la sesión.

**Cómo se incluye en otras páginas:**

```html
<div th:replace="~{fragments/sidebar :: sidebar(activo='dashboard')}"></div>
```

Esta línea reemplaza el div con el fragmento completo del sidebar, pasando el nombre de la sección activa.

---

### `admin/dashboard.html` — Panel principal

Es la primera pantalla que se ve al iniciar sesión. Muestra un resumen general del negocio.

```html
<div th:if="${param.accesoDenegado}" class="alert alert-warning ...">
    Acceso restringido. No tienes permisos para acceder a esa sección.
</div>
```

- `param.accesoDenegado`: Lee el parámetro de la URL. Si la URL es `/admin/dashboard?accesoDenegado=true`, aparece esta alerta. Esto ocurre cuando un Cajero intenta acceder a Estadísticas, Gráficos o Configuración.

**KPIs (indicadores clave):**

```html
<div class="kpi-admin-num">₦<span th:text="${#numbers.formatInteger(totalIngresos, 1, 'COMMA')}"></span></div>
```

- `${totalIngresos}`: Variable enviada desde el controlador con la suma de todos los totales de reservas.
- `#numbers.formatInteger(..., 'COMMA')`: Formatea el número con comas (ej: 22,500).

**Lista de reservas recientes:**

```html
<div th:each="r : ${reservas}" class="reserva-item">
    <div th:text="${r.clienteNombre}"></div>
    <div th:text="${r.pelicula}"></div>
    <span th:text="${r.estado}"
          th:class="${r.estado == 'Confirmada'} ? 'badge-confirmado' : ...">
    </span>
</div>
```

- `th:each` itera sobre la lista de reservas y dibuja una fila por cada una.
- La clase del badge cambia según el estado: verde para Confirmada, amarillo para Pendiente, rojo para Cancelada.

---

### `admin/reservas.html` — Gestión de Reservas

Tiene dos partes: un formulario para crear reservas y una tabla para verlas/gestionarlas.

**Formulario de nueva reserva:**

```html
<form th:action="@{/admin/reservas/nueva}" th:object="${reserva}" method="post">
    <input type="text" th:field="*{clienteNombre}" ...>
    <select th:field="*{pelicula}">
        <option th:each="p : ${peliculas}" th:value="${p.titulo}" th:text="${p.titulo}"></option>
    </select>
    ...
</form>
```

- `th:object="${reserva}"`: Vincula el formulario con el objeto `Reserva` que el controlador envió vacío.
- `th:field="*{clienteNombre}"`: Vincula el campo con el atributo `clienteNombre` del objeto.
- El `select` de película genera una opción por cada película en cartelera usando `th:each`.

**Búsqueda y filtro:**

```html
<form action="/admin/reservas" method="get">
    <input type="text" name="buscar" th:value="${buscar}">
    <select name="estado" onchange="this.form.submit()">
        <option value="Confirmada" th:selected="${estadoFiltro == 'Confirmada'}">Confirmada</option>
    </select>
</form>
```

- Es un GET porque solo filtra lo que se muestra, no modifica datos.
- `onchange="this.form.submit()"`: El filtro de estado se aplica automáticamente al cambiarlo.
- `th:selected`: Mantiene seleccionada la opción que el usuario eligió.

**Tabla con acciones:**

```html
<tr th:each="r : ${reservas}">
    <td th:text="${r.id}"></td>
    ...
    <td>
        <a th:if="${r.estado == 'Pendiente'}"
           th:href="@{/admin/reservas/confirmar/{id}(id=${r.id})}">Confirmar</a>
        <a th:href="@{/admin/reservas/eliminar/{id}(id=${r.id})}"
           onclick="return confirm('¿Eliminar?')">Eliminar</a>
    </td>
</tr>
```

- El botón "Confirmar" solo aparece si la reserva está Pendiente (`th:if`).
- `@{/admin/reservas/confirmar/{id}(id=${r.id})}`: Construye la URL con el ID dinámico de cada reserva.
- `onclick="return confirm(...)"`: Muestra un cuadro de diálogo antes de eliminar.

---

### `admin/peliculas.html` — Gestión de Películas

Formulario para agregar películas + tarjetas visuales por cada película registrada.

```html
<div th:each="h : ${p.horarios.split(',')}" class="admin-horario-pill" th:text="${h.trim()}"></div>
```

- Los horarios se guardan como un texto con comas (`"14:00,17:30,21:00"`).
- `.split(',')` los divide en un arreglo y `th:each` dibuja una pastilla por cada horario.
- `.trim()` elimina espacios en blanco.

```html
<a th:href="@{/admin/peliculas/eliminar/{id}(id=${p.id})}"
   onclick="return confirm('¿Eliminar esta película?')">Eliminar</a>
```

El enlace de eliminar lleva al controlador el ID de la película a borrar.

---

### `admin/clientes.html` — Gestión de Clientes

Formulario para agregar clientes + tabla con sus estadísticas de consumo.

```html
<td th:text="${pedidosPorCliente[c.nombre]}"></td>
<td>₦<span th:text="${gastadoPorCliente[c.nombre]}"></span></td>
```

- `pedidosPorCliente` y `gastadoPorCliente` son dos `Map` calculados en el controlador.
- Aquí se accede al valor del mapa usando el nombre del cliente como clave: `mapa[clave]`.
- Muestra cuántas reservas tiene cada cliente y cuánto dinero ha gastado en total.

---

### `admin/snacks.html` — Gestión de Snacks

Mismo patrón que películas. Formulario con campos (nombre, categoría, precio, stock, descripción) y tarjetas por cada snack registrado.

```html
<span class="admin-stock-badge">Stock: <span th:text="${s.stock}"></span></span>
```

Muestra el stock disponible de cada producto en su tarjeta.

---

### `admin/estadisticas.html` — Estadísticas (solo Administrador)

Muestra 3 KPIs numéricos y 3 gráficos generados con **Chart.js** (librería de gráficos JavaScript).

**Cómo se pasan los datos de Java a JavaScript:**

```html
<script th:inline="javascript">
    const peliculas = /*[[${peliculas}]]*/ [];
    const reservas  = /*[[${reservas}]]*/ [];
</script>
```

- `th:inline="javascript"`: Activa el procesamiento Thymeleaf dentro de un bloque de JavaScript.
- `/*[[${peliculas}]]*/`: Thymeleaf reemplaza esto con los datos de Java convertidos a JSON. El `/**/` es sintaxis de comentario JS por si hay un error, el navegador no se rompe.

**Gráficos:**
- **Barras verticales**: Precio por película.
- **Dona (doughnut)**: Proporción de estados de reservas (Confirmada / Pendiente / Cancelada).
- **Línea**: Reservas por mes (datos simulados).

---

### `admin/graficos.html` — Gráficos (solo Administrador)

Misma estructura que Estadísticas pero con gráficos distintos:

- **Barras horizontales**: Duración por película en minutos.
- **Pastel (pie)**: Distribución de reservas.
- **Línea**: Ingresos estimados por semana (datos simulados).

La diferencia técnica del gráfico horizontal es:

```javascript
options: { indexAxis: 'y' }
```

Al cambiar el eje a `'y'`, las barras se dibujan de forma horizontal.

---

### `admin/configuracion.html` — Configuración (solo Administrador)

Dos formularios estáticos (sin lógica de backend activa):

1. **Información del Cine**: Nombre, teléfono, dirección, email, horario.
2. **Precios**: Entrada General, Entrada VIP, Combo Palomitas, Combo Familiar.

Los campos tienen valores precargados con los datos del cine. Los botones de guardar existen visualmente pero aún no tienen funcionalidad backend conectada.

---

## Sistema de Roles

El sistema de control de acceso funciona en tres niveles:

**1. Al hacer login** (`LoginController`): Se guarda el rol en la sesión del navegador.

**2. En el menú lateral** (`sidebar.html`): Los ítems de Estadísticas, Gráficos y Configuración solo se renderizan si `session.userRole == 'ADMIN'`. El Cajero directamente no los ve.

**3. En las rutas del servidor** (`AdminController`): Aunque el Cajero no vea los links, si intenta acceder a la URL directamente (por ejemplo escribiendo `/admin/estadisticas`), el controlador verifica el rol y lo redirige al dashboard con un mensaje de acceso denegado.

| Sección | Administrador | Cajero |
|---|---|---|
| Dashboard | Sí | Sí |
| Reservas | Sí | Sí |
| Películas | Sí | Sí |
| Clientes | Sí | Sí |
| Snacks | Sí | Sí |
| Estadísticas | Sí | No |
| Gráficos | Sí | No |
| Configuración | Sí | No |

---

## Tecnologías utilizadas

| Tecnología | Para qué sirve |
|---|---|
| **Spring Boot** | Framework Java que facilita crear aplicaciones web sin configuración compleja |
| **Thymeleaf** | Motor de plantillas que mezcla HTML con datos dinámicos del servidor |
| **Bootstrap 5** | Librería CSS/JS para diseño visual responsivo y componentes |
| **Chart.js** | Librería JavaScript para generar gráficos interactivos |
| **HttpSession** | Mecanismo de Spring para mantener al usuario logueado entre peticiones |
| **Maven** | Herramienta que gestiona las dependencias y compila el proyecto |

---

## Flujo completo de una acción típica

**Ejemplo: El Cajero crea una nueva reserva**

1. El Cajero ingresa a `/login` con `cajero_cine / Cajero2026!`.
2. `LoginController.procesarLogin()` valida las credenciales y guarda `rol=CAJERO` en sesión.
3. El Cajero es redirigido a `/admin/dashboard`.
4. Ve el menú sin Estadísticas, Gráficos ni Configuración.
5. Hace clic en "Reservas" → va a `/admin/reservas`.
6. `AdminController.reservas()` verifica sesión ✓, carga la lista, la manda al HTML.
7. El Cajero llena el formulario y hace clic en "Guardar Reserva".
8. El formulario hace `POST /admin/reservas/nueva`.
9. `AdminController.nuevaReserva()` recibe el objeto `Reserva`, llama a `reservaService.agregar()`.
10. `ReservaService.agregar()` asigna un ID y agrega la reserva a la lista en memoria.
11. El controlador redirige a `/admin/reservas` y el Cajero ve la nueva reserva en la tabla.
