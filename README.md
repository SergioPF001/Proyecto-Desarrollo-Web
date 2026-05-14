# Cine La Estación — Proyecto Web Spring Boot

Aplicación web para la gestión de un cine desarrollada con **Java 21**, **Spring Boot 3.2.5** y **Thymeleaf**. Permite administrar películas, clientes, reservas y snacks desde un panel de administración protegido por login.

---

## Tecnologías utilizadas

| Tecnología | Versión | Para qué se usa |
|---|---|---|
| Java | 21 | Lenguaje principal del backend |
| Spring Boot | 3.2.5 | Framework para levantar el servidor web |
| Thymeleaf | (incluido) | Motor de plantillas HTML del lado del servidor |
| Bootstrap | 5.3.3 | Estilo visual y diseño responsivo |
| Chart.js | 4.4.3 | Gráficos de barras, líneas y círculos |
| WebJars | — | Sirve Bootstrap y Chart.js desde el servidor sin CDN externo |

---

## Cumplimiento de la rúbrica

### 1. Clases Modelo — mínimo 3 clases ✅

Se crearon **4 clases modelo** en `src/main/java/com/amsuno/model/`:

**`Cliente.java`**
Representa a un cliente del cine. Atributos: `id`, `nombre`, `email`, `telefono`.

**`Pelicula.java`**
Representa una película en cartelera. Atributos: `id`, `titulo`, `genero`, `duracion`, `clasificacion`, `precio`, `esEstreno`, `horarios`.

**`Reserva.java`**
Representa una reserva de entradas. Atributos: `id`, `clienteNombre`, `pelicula`, `fecha`, `asientos`, `total`, `estado` (Pendiente / Confirmada / Cancelada).

**`Snack.java`**
Representa un producto del snack bar. Atributos: `id`, `nombre`, `categoria`, `precio`, `stock`, `descripcion`.

> Todas las clases usan getters y setters, constructor vacío y constructor completo. No usan base de datos — los datos viven en memoria (`ArrayList`) mientras el servidor esté corriendo.

---

### 2. Clases de Servicio con operaciones CRUD ✅

Se crearon **4 servicios** en `src/main/java/com/amsuno/service/`. Cada uno tiene:

- `listar()` — devuelve la lista completa en memoria
- `agregar(objeto)` — asigna un ID automático con `AtomicLong` y lo agrega a la lista
- `buscar(id)` — recorre la lista con stream y retorna el objeto que coincide con el ID
- `eliminar(id)` — remueve el objeto de la lista cuyo ID coincide

**`ClienteService.java`** — gestiona la lista de clientes. Incluye 3 clientes de ejemplo precargados al iniciar.

**`PeliculaService.java`** — gestiona la cartelera. Incluye 4 películas de ejemplo precargadas.

**`ReservaService.java`** — gestiona las reservas. Además del CRUD tiene `confirmar(id)` que cambia el estado de una reserva de "Pendiente" a "Confirmada". Incluye 3 reservas de ejemplo.

**`SnackService.java`** — gestiona los productos del snack bar. Incluye 3 snacks de ejemplo precargados.

**¿Por qué `AtomicLong`?**
Es un contador que se incrementa de forma segura. Cada vez que se agrega un objeto nuevo, el contador sube en 1 y ese número se usa como ID único del objeto.

**¿Por qué `ArrayList`?**
Es la estructura más simple de Java para guardar listas de objetos en memoria. No se necesita base de datos porque el objetivo es demostrar la lógica CRUD de forma básica.

---

### 3. Controladores conectados con páginas HTML ✅

Se crearon **3 controladores** en `src/main/java/com/amsuno/controller/`:

**`HomeController.java`**
Maneja la ruta `/` y retorna la página de inicio (`index.html`). Es el controlador más simple del proyecto.

```
GET /  →  index.html
```

**`LoginController.java`**
Maneja el acceso al panel de administración.
- `GET /login` — muestra el formulario de login
- `POST /login` — recibe usuario y contraseña, los compara con las credenciales definidas como constantes (`admin_cine` / `LaEstacion2026!`). Si son correctas, guarda `loggedIn = true` en la sesión HTTP y redirige al dashboard.

```
GET  /login  →  login.html
POST /login  →  valida credenciales → redirect:/admin/dashboard o error
```

**`AdminController.java`**
Es el controlador principal. Maneja todas las rutas del panel de administración bajo `/admin/**`. **Cada método verifica primero si hay sesión activa** — si no hay login, redirige a `/login`.

Rutas disponibles:

| Método | Ruta | Acción |
|--------|------|--------|
| GET | `/admin/dashboard` | Muestra el dashboard con resumen general |
| GET | `/admin/peliculas` | Lista todas las películas |
| POST | `/admin/peliculas/agregar` | Agrega una película nueva |
| GET | `/admin/peliculas/eliminar/{id}` | Elimina una película por ID |
| GET | `/admin/clientes` | Lista todos los clientes |
| POST | `/admin/clientes/agregar` | Agrega un cliente nuevo |
| GET | `/admin/clientes/eliminar/{id}` | Elimina un cliente por ID |
| GET | `/admin/reservas` | Lista reservas (con filtro por estado y búsqueda) |
| POST | `/admin/reservas/nueva` | Crea una reserva nueva |
| GET | `/admin/reservas/confirmar/{id}` | Confirma una reserva pendiente |
| GET | `/admin/reservas/eliminar/{id}` | Elimina una reserva por ID |
| GET | `/admin/snacks` | Lista todos los snacks |
| POST | `/admin/snacks/agregar` | Agrega un snack nuevo |
| GET | `/admin/snacks/eliminar/{id}` | Elimina un snack por ID |
| GET | `/admin/estadisticas` | Página de estadísticas con gráficos |
| GET | `/admin/graficos` | Segunda página de gráficos |
| GET | `/admin/cerrar-sesion` | Cierra la sesión e invalida la cookie |

---

### 4. Páginas Thymeleaf con formularios, listado, consulta y eliminación ✅

Se crearon **más de 4 páginas** en `src/main/resources/templates/`:

#### Página pública

**`index.html`** — Página principal del sitio. Contiene secciones: hero con cartelera de la semana, servicios, quiénes somos, testimonios y formulario de contacto. El navbar enlaza a cada sección mediante anclas (`#inicio`, `#servicios`, etc.).

**`login.html`** — Formulario de acceso al panel admin. Tiene campos usuario y contraseña. Si los datos son incorrectos muestra un mensaje de error.

#### Panel de administración (`templates/admin/`)

**`dashboard.html`** — Vista general del sistema. Muestra tarjetas KPI con: total de ingresos, número de reservas, clientes registrados y películas en cartelera. También lista las últimas reservas con su estado.

**`peliculas.html`** — Formulario para agregar películas (título, género, duración, clasificación, precio, horarios, estreno) + listado en tarjetas con botón eliminar.

**`clientes.html`** — Formulario para agregar clientes (nombre, email, teléfono) + tabla con todos los clientes y botón eliminar.

**`reservas.html`** — Formulario para crear reservas (cliente, película, fecha, asientos, total, estado) + tabla de reservas con filtro por estado, búsqueda por texto, botón confirmar y botón eliminar.

**`snacks.html`** — Formulario para agregar snacks (nombre, categoría, precio, stock, descripción) + listado en tarjetas con stock visible y botón eliminar.

**`estadisticas.html`** — Primera página de gráficos. Contiene 3 KPIs y 3 gráficos: barras de precio por película, dona de estado de reservas, línea de reservas por mes.

**`graficos.html`** — Segunda página de gráficos. Contiene 3 KPIs y 3 gráficos: barras horizontales de duración por película, pie de distribución de reservas, línea de ingresos estimados por semana.

**`configuracion.html`** — Página informativa con los datos del cine (nombre, dirección, credenciales del admin).

#### Fragmento reutilizable

**`fragments/sidebar.html`** — Barra lateral de navegación del panel admin. Se incluye en todas las páginas admin con `th:replace`. Recibe un parámetro `activo` para resaltar el ítem del menú correspondiente. Contiene links a: Dashboard, Reservas, Películas, Clientes, Snacks, Estadísticas, Gráficos, Configuración y Cerrar Sesión.

---

### 5. Páginas de gráficos: barras, lineales y círculos ✅

**`admin/estadisticas.html`**
- Gráfico de **barras verticales**: precio de cada película en cartelera
- Gráfico de **dona (circular)**: proporción de reservas Confirmadas / Pendientes / Canceladas
- Gráfico **lineal**: tendencia de reservas por mes (12 meses)

**`admin/graficos.html`**
- Gráfico de **barras horizontales**: duración en minutos de cada película
- Gráfico **circular (pie)**: distribución de reservas por estado
- Gráfico **lineal**: ingresos estimados por semana (8 semanas)

Los datos de películas y reservas se pasan desde el servidor mediante Thymeleaf (`th:inline="javascript"`) y Chart.js los procesa para dibujar los gráficos.

---

### 6. Menú con Bootstrap que enlaza las páginas ✅

**Navbar pública** (`index.html`): barra de navegación Bootstrap con enlaces de scroll suave a las secciones de la misma página (`#inicio`, `#servicios`, `#quienes-somos`, `#contactenos`) y botón "Ingresar" que lleva al login.

**Sidebar del panel admin** (`fragments/sidebar.html`): menú lateral Bootstrap con enlaces a todas las páginas del panel. El ítem activo se resalta automáticamente con una clase CSS distinta usando Thymeleaf.

---

## Estructura del proyecto

```
src/main/java/com/amsuno/
│
├── CineApp.java                        ← Punto de entrada, arranca el servidor
│
├── model/
│   ├── Cliente.java                    ← Clase modelo: cliente
│   ├── Pelicula.java                   ← Clase modelo: película
│   ├── Reserva.java                    ← Clase modelo: reserva
│   └── Snack.java                      ← Clase modelo: snack
│
├── service/
│   ├── ClienteService.java             ← CRUD de clientes en memoria
│   ├── PeliculaService.java            ← CRUD de películas en memoria
│   ├── ReservaService.java             ← CRUD + confirmar reservas en memoria
│   └── SnackService.java               ← CRUD de snacks en memoria
│
└── controller/
    ├── HomeController.java             ← Ruta pública: /
    ├── LoginController.java            ← Rutas: /login (GET y POST)
    └── AdminController.java            ← Rutas: /admin/** (protegidas por sesión)

src/main/resources/
│
├── templates/
│   ├── index.html                      ← Página principal pública
│   ├── login.html                      ← Formulario de acceso admin
│   ├── fragments/
│   │   └── sidebar.html               ← Menú lateral reutilizable (admin)
│   └── admin/
│       ├── dashboard.html              ← Resumen general con KPIs
│       ├── peliculas.html              ← CRUD de películas
│       ├── clientes.html               ← CRUD de clientes
│       ├── reservas.html               ← CRUD de reservas con filtros
│       ├── snacks.html                 ← CRUD de snacks
│       ├── estadisticas.html           ← Gráficos 1: barras, dona, línea
│       ├── graficos.html               ← Gráficos 2: barras horiz., pie, línea
│       └── configuracion.html          ← Datos del sistema
│
└── static/
    └── css/
        └── amsuno.css                  ← Estilos personalizados del proyecto
```

---

## Cómo funciona el flujo de datos

```
Usuario llena formulario HTML
        ↓
Thymeleaf envía POST al controlador
        ↓
AdminController recibe los datos como objeto Java (@ModelAttribute)
        ↓
Llama al Service correspondiente → agregar(objeto)
        ↓
El Service asigna un ID y lo guarda en el ArrayList en memoria
        ↓
Controlador redirige → la página recarga y muestra el dato nuevo
```

---

## Cómo funciona la protección del panel admin

```
Accede a cualquier ruta /admin/**
        ↓
AdminController llama a sinSesion(sesion)
        ↓
¿Existe sesion.getAttribute("loggedIn")?
   NO  →  redirect:/login
   SÍ  →  ejecuta la lógica normal y retorna la vista
```

El login guarda `loggedIn = true` en la sesión HTTP de Spring. Al cerrar sesión con `/admin/cerrar-sesion`, la sesión se invalida completamente.

---

## Cómo ejecutar el proyecto

```bash
# Desde la raíz del proyecto
mvn spring-boot:run
```

Luego abrir en el navegador:
- Sitio público: `http://localhost:8080/`
- Panel admin: `http://localhost:8080/login`
  - Usuario: `admin_cine`
  - Contraseña: `LaEstacion2026!`

> Los datos se guardan **en memoria**. Al reiniciar el servidor, los datos vuelven a los valores de ejemplo predefinidos en cada Service.
