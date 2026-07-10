# Documentación del Proyecto — Cine La Estación

Sistema web de gestión y reservas para un cine, desarrollado con Java 21, Spring Boot 3.2.5,
Spring Security, Spring Data JPA, Thymeleaf y MySQL 8.

---

## 1. Descripción

### 1.1 Problema

La gestión de un cine pequeño suele hacerse en papel o en hojas de cálculo. El cliente llama por teléfono
para preguntar si quedan asientos, un empleado revisa una lista manual, anota el nombre y calcula el precio.
Ese proceso es lento, permite vender el mismo asiento dos veces y no deja información confiable para tomar
decisiones de negocio.

### 1.2 Solución

**Cine La Estación** digitaliza toda la operación en un solo sistema web con dos caras:

**Cara pública** (sin registro):
- Página de inicio con la cartelera, servicios, información del cine y contacto.
- Módulo de reservas en tres pasos: elegir película, elegir fecha y horario, seleccionar asientos en un
  mapa visual e ingresar los datos personales.

**Cara administrativa** (protegida por login):
- Dashboard con métricas: ingresos totales, reservas pendientes, clientes y películas.
- Mantenimiento de películas, clientes, reservas y snacks.
- Estadísticas y gráficos de barras, líneas y circulares.
- Herramientas conectadas a APIs externas (tipo de cambio y consulta de DNI).
- Dos roles con permisos distintos: **ADMIN** y **CAJERO**.

### 1.3 Alcance de esta entrega

Además del sistema base, esta entrega incorpora:

| Componente | Descripción |
|---|---|
| Seguridad con Spring Security | Usuarios en base de datos, contraseñas con BCrypt, roles y menús dinámicos |
| API REST propia | Endpoints de consulta y mantenimiento bajo `/api/**` |
| Cliente externo | Página HTML + JavaScript, fuera de Spring Boot, que consume la API |
| APIs externas | Tipo de cambio (cotización real) y consulta de DNI (RENIEC) |

---

## 2. Modelamiento

### 2.1 Arquitectura en capas

```
[ Navegador ]                        [ Cliente externo HTML + JS ]
      |                                          |
      | HTTP (Thymeleaf)                         | fetch() + HTTP Basic
      v                                          v
+-----------------------------+   +-----------------------------+
|  Controladores web          |   |  Controladores REST         |
|  HomeController             |   |  PeliculaApiController      |
|  PublicController           |   |  SnackApiController         |
|  AdminController            |   |  ReservaApiController       |
|  HerramientasController     |   |  ConsultaApiController      |
+-----------------------------+   +-----------------------------+
              |                                  |
              +----------------+-----------------+
                               v
                    +---------------------+          +--------------------+
                    |  Servicios          |--------->|  APIs externas     |
                    |  lógica de negocio  |          |  tipo de cambio    |
                    +---------------------+          |  consulta DNI      |
                               |                     +--------------------+
                               v
                    +---------------------+
                    |  Repositorios (JPA) |
                    +---------------------+
                               |
                               v
                    +---------------------+
                    |     MySQL 8         |
                    |  + 4 procedimientos |
                    |    almacenados      |
                    +---------------------+
```

Cada capa tiene una responsabilidad única. Si se cambia el motor de base de datos solo se tocan los
repositorios; si cambia la regla de cálculo del precio, solo el servicio; si se agrega una pantalla,
solo el controlador. Esto es lo que permite que el sistema sea escalable.

### 2.2 Modelo de datos

El sistema tiene **8 entidades**:

| Entidad | Descripción |
|---|---|
| `Sala` | Sala física del cine: nombre, filas y columnas |
| `Asiento` | Asiento dentro de una sala: fila, número y tipo (normal, preferencial, vip) |
| `Pelicula` | Película en cartelera: título, género, duración, clasificación, precio, estreno y horarios |
| `Cliente` | Persona que reserva: nombre, email y teléfono |
| `Reserva` | Vincula cliente + película en una fecha, con cantidad de asientos, total y estado |
| `ReservaAsiento` | Tabla intermedia: qué asientos físicos ocupa cada reserva |
| `Snack` | Producto del snack bar: nombre, categoría, precio, stock y descripción |
| `Usuario` | Credenciales del panel: usuario, contraseña cifrada y rol |

### 2.3 Relaciones

```
Sala 1 ────── N Asiento
Sala 1 ────── N Pelicula

Cliente 1 ──── N Reserva
Pelicula 1 ─── N Reserva

Reserva 1 ──── N ReservaAsiento N ──── 1 Asiento

Usuario  (independiente, solo para autenticación)
```

La tabla `reserva_asiento` es la clave del sistema: una reserva puede ocupar varios asientos, y un mismo
asiento puede aparecer en muchas reservas siempre que sean de fechas distintas. Al cancelar una reserva,
esas filas se eliminan y los asientos quedan libres nuevamente.

### 2.4 Procedimientos almacenados

Se implementaron 4 procedimientos que viven dentro de MySQL y se recrean al arrancar la aplicación
(`ProcedimientosLoader.java`):

| Procedimiento | Para qué sirve | Por qué es procedimiento |
|---|---|---|
| `sp_AsientosDisponibles` | Devuelve los asientos de una sala marcando cuáles están ocupados en una fecha | Requiere un `JOIN` entre 5 tablas; se resuelve más rápido en el motor de BD |
| `sp_CrearReserva` | Inserta la reserva y devuelve su ID generado | Necesita `LAST_INSERT_ID()` como parámetro `OUT` de forma atómica |
| `sp_CancelarReserva` | Libera los asientos y marca la reserva como cancelada | Las dos operaciones deben ocurrir juntas |
| `sp_ResumenSala` | Cuenta asientos totales, ocupados y disponibles de una sala | Métrica interna de ocupación |

### 2.5 Modelo de seguridad

La autenticación usa `UsuarioDetailsService`, que lee la tabla `usuario` y entrega el rol a Spring Security.
Las contraseñas se guardan cifradas con **BCrypt**; nunca en texto plano.

Toda la seguridad está declarada en **una sola cadena de filtros** (`SecurityConfig.java`), que habilita
dos formas de autenticarse porque un navegador y un programa no se identifican igual:

- **Formulario de login** (`/login`) para las páginas web, con sesión y protección CSRF.
- **HTTP Basic** para la API, porque un cliente JavaScript no puede llenar un formulario HTML.

Spring Security elige automáticamente cuál usar según la cabecera `Accept` de la petición: si el cliente
pide HTML lo manda al formulario; si pide JSON le responde `401`. El CSRF se desactiva únicamente en
`/api/**` con `csrf.ignoringRequestMatchers("/api/**")`, porque la API no usa cookies de sesión.

Reglas de autorización:

| Recurso | Público | CAJERO | ADMIN |
|---|:---:|:---:|:---:|
| `/`, `/reservar`, `/login` | Sí | Sí | Sí |
| `/admin/dashboard`, `/reservas`, `/peliculas`, `/clientes`, `/snacks`, `/herramientas` | No | Sí | Sí |
| `/admin/estadisticas`, `/admin/graficos`, `/admin/configuracion` | No | No | Sí |
| `/admin/*/eliminar/**` | No | No | Sí |
| `GET /api/peliculas`, `GET /api/snacks`, `GET /api/consultas/**` | Sí | Sí | Sí |
| `GET /api/reservas` | No | Sí | Sí |
| `POST`, `PUT`, `DELETE` en `/api/**` | No | No | Sí |

El menú lateral se construye con `sec:authorize`, por lo que el cajero **no ve** las opciones que no puede
usar, y los botones "Eliminar" tampoco aparecen para su rol. La protección está en dos niveles: la URL y la vista.

### 2.6 Integración con APIs externas

| API | Endpoint usado | Token | Uso en el proyecto |
|---|---|---|---|
| Exchange Rate API | `https://open.er-api.com/v6/latest/USD` | No requiere | Convierte el precio de las entradas de soles a dólares |
| APIs.net.pe (RENIEC) | `https://api.apis.net.pe/v2/reniec/dni` | Token gratuito | Autocompleta el nombre al registrar un cliente por su DNI |

Ambas se consumen desde `ConsultaExternaService` con `RestTemplate`, y se exponen también como endpoints
propios en `/api/consultas/**` para que el cliente externo pueda usarlas.

Para activar la consulta de DNI hay que registrar una cuenta gratuita en <https://apis.net.pe> y copiar el
token en `application.properties`:

```properties
api.dni.token=TU_TOKEN_AQUI
```

Si el token está vacío, la aplicación **sigue funcionando** y la sección de DNI muestra un mensaje
explicativo en lugar de romperse.

---

## 3. Evidencias de la ejecución

Todas las pruebas se hicieron con la aplicación corriendo en `http://localhost:8080` y el cliente externo
en `http://localhost:5500`.

### 3.1 Arranque de la aplicación

```
Tomcat started on port 8080 (http) with context path ''
Started CineApp in 4.1 seconds (process running for 4.397)
```

### 3.2 API pública — consulta de cartelera

`GET /api/peliculas` → `200 OK`

```json
[{"id":1,"titulo":"Avatar 3: El Semillero","genero":"Ciencia Ficción","duracion":180,
  "clasificacion":"PG-13","precio":2500.0,"esEstreno":true,
  "horarios":["14:00","17:30","21:00"],"sala":"Sala Principal"}]
```

`GET /api/peliculas/generos` → `["Acción","Animación","Ciencia Ficción"]`

### 3.3 API externa — tipo de cambio real

`GET /api/consultas/tipo-cambio` → `200 OK`

```json
{"monedaBase":"USD","monedaDestino":"PEN","valor":3.402424,
 "actualizado":"Fri, 10 Jul 2026 00:02:31 +0000"}
```

La página `/admin/herramientas` usa este valor para mostrar el precio de cada película en soles y en dólares.

### 3.4 Seguridad de la API por roles

| Petición | Usuario | Resultado |
|---|---|---|
| `GET /api/reservas` | (sin autenticar) | `401 Unauthorized` |
| `GET /api/reservas` | `cajero_cine` | `200 OK` |
| `GET /api/reservas` | `admin_cine` | `200 OK` |
| `POST /api/snacks` | `cajero_cine` | `403 Forbidden` |
| `POST /api/snacks` | `admin_cine` | `201 Created` |
| `PUT /api/snacks/{id}` | `admin_cine` | `200 OK` |
| `DELETE /api/snacks/{id}` | `admin_cine` | `204 No Content` |
| `PUT /api/reservas/{id}/confirmar` | `cajero_cine` | `403 Forbidden` |
| `PUT /api/reservas/{id}/confirmar` | `admin_cine` | `200 OK`, estado `Confirmada` |
| `PUT /api/reservas/{id}/cancelar` | `admin_cine` | `200 OK`, asientos liberados |

Nótese que la misma petición sin autenticar devuelve `401` (falta identificarse) y autenticada con el rol
equivocado devuelve `403` (identificado, pero sin permiso). Son dos situaciones distintas y la API las
distingue correctamente.

Alta de un snack desde la API como administrador:

```json
{"id":4,"nombre":"Nachos API","categoria":"Snacks","precio":1200.0,
 "stock":30,"descripcion":"Creado desde la API"}
```

### 3.5 Manejo de errores de la API

| Petición | Respuesta |
|---|---|
| `GET /api/snacks/999` | `404` — `{"mensaje":"No existe el snack con id 999"}` |
| `GET /api/consultas/dni/123` | `400` — `{"mensaje":"El DNI debe tener exactamente 8 dígitos."}` |
| `GET /api/consultas/dni/12345678` sin token | `400` — `{"mensaje":"Falta configurar 'api.dni.token' en application.properties."}` |

### 3.6 Seguridad web por roles

| Prueba | Resultado |
|---|---|
| `/admin/dashboard` sin sesión | `302` → redirige a `/login` |
| Login con contraseña incorrecta | `302` → `/login?error` |
| Login correcto como cajero | `302` → `/admin/dashboard` |
| Cajero entra a `/admin/estadisticas` | `403` → página "Acceso denegado" |
| Cajero abre `/admin/peliculas/eliminar/1` | `403` |
| Botón "Eliminar" en la vista del cajero | 0 apariciones (oculto por `sec:authorize`) |
| Cerrar sesión (POST `/logout`) | `302` → `/` y el panel vuelve a pedir login |

### 3.7 Menús según el rol

**Menú del CAJERO** (6 opciones):
```
/admin/dashboard   /admin/reservas   /admin/peliculas
/admin/clientes    /admin/snacks     /admin/herramientas
```

**Menú del ADMIN** (9 opciones):
```
/admin/dashboard   /admin/reservas      /admin/peliculas
/admin/clientes    /admin/snacks        /admin/herramientas
/admin/estadisticas  /admin/graficos    /admin/configuracion
```

### 3.8 Cliente externo (fuera de Spring Boot)

Servido con `python -m http.server 5500`, consumiendo la API del puerto `8080`:

| Petición cross-origin desde `http://localhost:5500` | Resultado |
|---|---|
| `GET /api/peliculas` | `200 OK` |
| `GET /api/consultas/tipo-cambio` | `200 OK` |
| `GET /api/reservas` con `cajero_cine` | `200 OK` |
| `POST /api/snacks` con `cajero_cine` | `403 Forbidden` |
| Preflight `OPTIONS /api/snacks` | `200 OK` |

La cabecera `Access-Control-Allow-Origin: *` confirma que la política CORS declarada en `WebConfig.java`
permite el consumo desde un origen distinto.

### 3.9 Protección CSRF

Los formularios generados por Thymeleaf incluyen el token automáticamente:

| Formulario | Tokens `_csrf` |
|---|---|
| `/login` | 1 |
| `/reservar` (reserva pública) | 1 |
| `/admin/asientos` (reserva administrativa) | 2 |

---

## 4. Limitaciones

1. **Los clientes no tienen cuenta propia.** Se identifican por su email y no pueden ver ni cancelar sus
   reservas después de crearlas. Solo el personal del cine puede modificarlas.

2. **Los roles y usuarios se crean por código.** `DataLoader` inserta `admin_cine` y `cajero_cine` la primera
   vez que arranca la aplicación. No existe una pantalla para crear, editar o dar de baja usuarios.

3. **La fecha y la hora se guardan como texto.** El campo `fecha` de `Reserva` es un `String` con formato
   `"2026-04-18 19:00"`. Esto simplifica el formulario, pero impide ordenar o filtrar por rango de fechas
   directamente en SQL.

4. **No hay control de concurrencia sobre los asientos.** Si dos personas seleccionan el mismo asiento al
   mismo tiempo y confirman en el mismo instante, ambas reservas se guardan. Se necesitaría un bloqueo o
   una restricción `UNIQUE (reserva_id, asiento_id)` sobre la fecha para evitarlo.

5. **Las APIs externas dependen de internet.** Si no hay conexión, la página de herramientas muestra un
   mensaje de error controlado, pero no puede mostrar la cotización ni consultar el DNI. Además, la consulta
   de DNI exige un token gratuito que no viene incluido en el repositorio.

6. **La API no tiene paginación.** `GET /api/peliculas` y `GET /api/reservas` devuelven todos los registros.
   Con miles de reservas la respuesta sería muy pesada.

7. **HTTP Basic viaja sin cifrar.** En un despliegue real sería obligatorio usar HTTPS, porque las
   credenciales viajan codificadas en base64, que no es cifrado.

8. **Los procedimientos almacenados atan el sistema a MySQL.** Migrar a PostgreSQL u otro motor obligaría a
   reescribirlos.

9. **No hay pruebas automatizadas.** La verificación se hizo de forma manual con peticiones HTTP, como
   consta en la sección de evidencias.

---

## 5. Conclusiones

1. **La separación en capas demostró su valor durante el propio desarrollo.** Al reemplazar el control de
   acceso manual basado en `HttpSession` por Spring Security, no fue necesario tocar ni un solo servicio ni
   repositorio: los cambios quedaron contenidos en la configuración y los controladores.

2. **Delegar la seguridad al framework redujo el código y eliminó errores.** La versión anterior repetía una
   comprobación de sesión en cada método del controlador —más de veinte veces— y bastaba olvidar una línea
   para dejar una página abierta. Ahora las reglas están declaradas en un solo archivo, `SecurityConfig`,
   y se aplican a todas las rutas por igual.

3. **La seguridad debe aplicarse en dos niveles a la vez.** Ocultar un botón del menú mejora la experiencia,
   pero no protege nada: cualquiera puede escribir la URL a mano. Por eso cada restricción visual con
   `sec:authorize` tiene su regla equivalente en la cadena de filtros, y las pruebas lo confirman
   (el cajero recibe `403` aunque el botón esté oculto).

4. **Exponer una API REST convirtió al sistema en una plataforma.** El mismo backend que sirve las páginas
   Thymeleaf alimenta a un cliente escrito solo con HTML y JavaScript, sin compartir una línea de código
   Java. Mañana podría ser una aplicación móvil o un panel de otro equipo, sin cambiar el servidor.

5. **CORS y los DTOs no son burocracia, resuelven problemas concretos.** CORS fue lo que permitió que el
   cliente del puerto `5500` hablara con la API del `8080`. Los DTOs evitaron enviar las entidades JPA
   directamente, lo que habría expuesto la estructura interna de la base de datos y arrastrado relaciones
   innecesarias en cada respuesta.

6. **Integrar servicios de terceros obliga a diseñar para el fallo.** Una API externa puede estar caída,
   lenta o exigir un token vencido. Por eso `ConsultaExternaService` traduce cualquier error a un mensaje
   entendible y la página sigue cargando: el sistema se degrada, no se cae.

7. **Los procedimientos almacenados tienen un costo real.** Ganan rendimiento en consultas con muchos `JOIN`
   y garantizan atomicidad en operaciones de varios pasos, pero reparten la lógica del negocio entre Java y
   SQL, y amarran el proyecto a MySQL. Es una decisión de ingeniería, no una mejora gratuita.

8. **Las limitaciones detectadas marcan el siguiente paso.** Convertir `fecha` a `LocalDateTime`, agregar la
   restricción `UNIQUE` que impide la doble reserva, paginar la API y publicar todo bajo HTTPS son mejoras
   concretas, identificadas y priorizadas gracias a las pruebas realizadas.

---

## 6. Bibliografía

1. Spring. *Spring Boot Reference Documentation, versión 3.2.5*.
   <https://docs.spring.io/spring-boot/docs/3.2.5/reference/html/>

2. Spring. *Spring Security Reference — Architecture, Authentication and Authorization*.
   <https://docs.spring.io/spring-security/reference/index.html>

3. Spring. *Spring Security — Cross Site Request Forgery (CSRF)*.
   <https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html>

4. Spring. *Spring Data JPA Reference Documentation*.
   <https://docs.spring.io/spring-data/jpa/reference/>

5. Spring. *Spring Framework — CORS Support*.
   <https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html>

6. Thymeleaf. *Tutorial: Thymeleaf + Spring* y *Thymeleaf Extras Spring Security*.
   <https://www.thymeleaf.org/documentation.html>

7. Oracle. *Java SE 21 Documentation*.
   <https://docs.oracle.com/en/java/javase/21/>

8. Oracle. *MySQL 8.0 Reference Manual — Chapter 25: Stored Objects*.
   <https://dev.mysql.com/doc/refman/8.0/en/stored-objects.html>

9. Mozilla. *MDN Web Docs — Using the Fetch API* y *Cross-Origin Resource Sharing (CORS)*.
   <https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API>

10. Fielding, R. T. (2000). *Architectural Styles and the Design of Network-based Software Architectures*.
    Disertación doctoral, University of California, Irvine. (Capítulo 5: REST).

11. Internet Engineering Task Force. *RFC 7617: The 'Basic' HTTP Authentication Scheme*.
    <https://datatracker.ietf.org/doc/html/rfc7617>

12. Provos, N. y Mazières, D. (1999). *A Future-Adaptable Password Scheme*. USENIX Annual Technical
    Conference. (Algoritmo BCrypt).

13. OWASP Foundation. *OWASP Top Ten* y *Password Storage Cheat Sheet*.
    <https://owasp.org/www-project-top-ten/>

14. Exchange Rate API. *Documentación del endpoint gratuito open.er-api.com*.
    <https://www.exchangerate-api.com/docs/free>

15. APIs.net.pe. *Documentación de la API de consulta RENIEC / DNI*.
    <https://apis.net.pe/api-consulta-dni>

16. Bootstrap. *Bootstrap 5.3 Documentation*.
    <https://getbootstrap.com/docs/5.3/>

17. Chart.js. *Chart.js 4.4 Documentation*.
    <https://www.chartjs.org/docs/latest/>
