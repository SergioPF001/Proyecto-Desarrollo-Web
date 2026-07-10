# Cine La Estación — Sistema Web de Gestión y Reservas

Sistema web para un cine, hecho con **Java 21**, **Spring Boot 3.2.5**, **Spring Security**, **MySQL 8** y vistas en **Thymeleaf** con **Bootstrap 5**.

Tiene una cara pública, donde cualquier persona ve la cartelera y reserva sus asientos en un mapa visual, y un panel administrativo protegido por login, donde el personal del cine gestiona reservas, películas, clientes y snacks.

Este README explica **qué se implementó en esta entrega**, dividido en 5 partes. La documentación completa del proyecto —descripción, modelamiento, evidencias de ejecución, limitaciones, conclusiones y bibliografía— está en **[docs/DOCUMENTACION.md](docs/DOCUMENTACION.md)**.

---

## PARTE 1 — De dónde partimos y a dónde llegamos

La versión anterior del proyecto ya reservaba asientos y tenía un panel de administración, pero arrastraba tres problemas importantes:

**El login era casero.** Los usuarios y contraseñas estaban escritos dentro del código, en texto plano, dentro de un `Map`. Cualquiera que abriera el archivo los veía.

**La seguridad se repetía a mano.** Cada método del `AdminController` empezaba con la misma línea:

```java
if (sesion.getAttribute("loggedIn") == null) return "redirect:/login";
```

Eso se repetía más de veinte veces. Bastaba olvidar una sola línea en un método nuevo para dejar una página abierta al público sin darse cuenta.

**El sistema estaba encerrado en sí mismo.** Solo se podía usar desde sus propias páginas HTML. Ningún otro programa podía consultar la cartelera ni registrar nada.

En esta entrega se corrigieron los tres problemas y se agregaron dos funcionalidades nuevas:

| # | Qué se implementó | Dónde vive |
|---|---|---|
| 1 | Seguridad real con Spring Security, roles y menús dinámicos | `config/SecurityConfig.java` |
| 2 | Usuarios guardados en base de datos con contraseñas cifradas | `model/Usuario.java` |
| 3 | Una API REST propia que devuelve JSON | `controller/api/` |
| 4 | Un cliente externo en HTML + JavaScript, fuera de Spring Boot | `cliente-externo/` |
| 5 | Integración con dos APIs externas: tipo de cambio y consulta de DNI | `service/ConsultaExternaService.java` |

**Archivos nuevos que conviene revisar:**

```
src/main/java/com/amsuno/
├── config/
│   ├── SecurityConfig.java          ← toda la seguridad, en un solo archivo
│   └── WebConfig.java               ← permiso CORS y cliente HTTP
├── controller/
│   ├── HerramientasController.java  ← página que usa las APIs externas
│   └── api/                         ← los 4 controladores REST
├── model/Usuario.java               ← tabla de usuarios del panel
├── service/
│   ├── UsuarioDetailsService.java   ← le dice a Spring quién es cada usuario
│   └── ConsultaExternaService.java  ← habla con las APIs de terceros
└── exception/                       ← errores traducidos a mensajes claros

cliente-externo/                     ← página HTML + JS, sin nada de Java
docs/DOCUMENTACION.md                ← documentación completa
```

---

## PARTE 2 — La seguridad: Spring Security, roles y menús

### Los usuarios ahora viven en la base de datos

Se creó la entidad `Usuario` con tres campos: usuario, contraseña y rol. Las contraseñas se guardan cifradas con **BCrypt**, un algoritmo diseñado específicamente para contraseñas.

Cifrar no es lo mismo que ocultar: BCrypt es una función de **un solo sentido**. Se puede comprobar si una contraseña coincide, pero es imposible recuperarla desde lo guardado. Si alguien roba la base de datos, no obtiene ninguna contraseña.

La clase `UsuarioDetailsService` es el puente: cuando alguien intenta entrar, Spring Security le pregunta a esta clase quién es ese usuario, y ella lo busca en la tabla y devuelve su rol.

### Toda la seguridad, en un solo archivo

`SecurityConfig.java` reemplaza las veinte comprobaciones manuales. Las reglas se declaran una sola vez y se aplican a todas las rutas por igual:

```java
.requestMatchers("/", "/reservar", "/login").permitAll()
.requestMatchers("/admin/estadisticas", "/admin/graficos", "/admin/configuracion").hasRole("ADMIN")
.requestMatchers("/admin/*/eliminar/**").hasRole("ADMIN")
.requestMatchers("/admin/**").hasAnyRole("ADMIN", "CAJERO")
```

Los controladores quedaron limpios: ya no saben nada de sesiones ni de permisos, solo de su trabajo.

### Dos formas de entrar, porque hay dos tipos de cliente

Un navegador y un programa no se identifican igual, así que la configuración habilita ambas:

- **Formulario de login** para las páginas web (con sesión y protección CSRF).
- **HTTP Basic** para la API, porque un programa en JavaScript no puede llenar un formulario HTML.

Spring Security elige cuál usar según lo que el cliente pida: si pide HTML lo manda al formulario; si pide JSON le responde `401`.

### Los permisos de cada rol

| Recurso | Público | CAJERO | ADMIN |
|---|:---:|:---:|:---:|
| Inicio, reservas públicas, login | Sí | Sí | Sí |
| Dashboard, reservas, películas, clientes, snacks, herramientas | No | Sí | Sí |
| Estadísticas, gráficos, configuración | No | No | Sí |
| Cualquier acción de **eliminar** | No | No | Sí |

### El menú se adapta al rol

El sidebar usa `sec:authorize` para mostrar solo lo que el usuario puede usar. El cajero ve **6 opciones**; el administrador ve **9**. Los botones "Eliminar" tampoco aparecen para el cajero.

**Pero ocultar un botón no protege nada**, porque cualquiera puede escribir la URL a mano. Por eso cada restricción visual tiene su regla equivalente del lado del servidor. Si el cajero escribe `/admin/estadisticas` directamente en el navegador, recibe la página de acceso denegado.

Esa es la idea central: **la seguridad se aplica en dos niveles a la vez**, la vista y la URL. El menú es comodidad; la cadena de filtros es la protección.

---

## PARTE 3 — La API REST propia

El mismo backend que genera las páginas Thymeleaf ahora expone también una **API REST** bajo `/api/**`, que responde en JSON en lugar de HTML.

### Endpoints disponibles

| Método | Endpoint | Qué hace | Quién puede |
|---|---|---|---|
| GET | `/api/peliculas` | Lista la cartelera (filtro opcional `?genero=`) | Público |
| GET | `/api/peliculas/generos` | Lista los géneros disponibles | Público |
| GET | `/api/peliculas/{id}` | Devuelve una película | Público |
| GET | `/api/snacks` | Lista los snacks | Público |
| POST | `/api/snacks` | Crea un snack | ADMIN |
| PUT | `/api/snacks/{id}` | Actualiza un snack | ADMIN |
| DELETE | `/api/snacks/{id}` | Elimina un snack | ADMIN |
| GET | `/api/reservas` | Lista las reservas (filtro opcional `?estado=`) | ADMIN o CAJERO |
| PUT | `/api/reservas/{id}/confirmar` | Confirma una reserva | ADMIN |
| PUT | `/api/reservas/{id}/cancelar` | Cancela y libera los asientos | ADMIN |
| GET | `/api/consultas/tipo-cambio` | Cotización del dólar | Público |
| GET | `/api/consultas/dni/{numero}` | Consulta un DNI en RENIEC | Público |

Así, las **consultas** son abiertas (la cartelera es información pública) y los **procesos de mantenimiento** exigen ser administrador.

### La API no devuelve las entidades directamente

Devuelve **DTOs**: objetos hechos a medida para la respuesta. `PeliculaDTO` entrega los horarios ya separados en una lista y el nombre de la sala, en vez de arrastrar toda la entidad `Sala` con sus columnas internas.

Esto evita exponer la estructura de la base de datos y deja libertad para cambiarla mañana sin romper a quien consume la API.

### Los errores también responden en JSON

`ApiExceptionHandler` traduce los errores a mensajes entendibles con el código HTTP correcto:

```json
GET /api/snacks/999   →  404   {"mensaje":"No existe el snack con id 999"}
GET /api/consultas/dni/123  →  400   {"mensaje":"El DNI debe tener exactamente 8 dígitos."}
```

Y la API distingue dos situaciones que suelen confundirse:

- **`401`** — no te identificaste.
- **`403`** — te identificaste, pero tu rol no tiene permiso.

---

## PARTE 4 — El cliente externo, fuera de Spring Boot

En la carpeta [`cliente-externo/`](cliente-externo/) hay una página hecha **solo con HTML, CSS y JavaScript**. No tiene ni una línea de Java. No usa Spring Boot. Se sirve desde otro puerto y llama a la API con `fetch()`.

```
localhost:5500                        localhost:8080
┌──────────────────────┐   fetch()   ┌──────────────────────┐
│  HTML + JavaScript   │ ──────────► │  API de Spring Boot  │
│  (sin Java)          │ ◄────────── │  responde JSON       │
└──────────────────────┘             └──────────────────────┘
```

### Por qué hizo falta configurar CORS

Como el cliente vive en el puerto `5500` y la API en el `8080`, para el navegador son **dos sitios distintos**. Por seguridad, el navegador bloquea por defecto ese tipo de llamadas.

CORS es el permiso que el servidor da para autorizarlas. Se declara en `WebConfig.java`:

```java
registro.addMapping("/api/**").allowedOrigins("*")
```

Sin esa línea, el cliente externo no funcionaría, aunque la API estuviera perfecta.

### Cómo se autentica

Para las operaciones protegidas, el cliente manda usuario y contraseña con **HTTP Basic**, en una cabecera de cada petición:

```javascript
credenciales = btoa(usuario + ":" + contrasena);
cabeceras["Authorization"] = "Basic " + credenciales;
```

La página permite comprobar los roles en vivo: si inicias sesión como `cajero_cine` e intentas agregar un snack, la API responde `403` y aparece el mensaje de permisos.

Esto demuestra que el backend dejó de ser una aplicación cerrada y se convirtió en una **plataforma reutilizable**. Mañana podría ser una app móvil consumiendo los mismos endpoints, sin cambiar una línea del servidor.

---

## PARTE 5 — Las APIs externas: tipo de cambio y DNI

La nueva página **Herramientas** del panel (`/admin/herramientas`) consume dos APIs de terceros y las conecta con el negocio del cine. No son adornos: cada una resuelve algo real.

### Tipo de cambio — gratuita, sin token

`https://open.er-api.com/v6/latest/USD`

Trae la cotización real del dólar y la usa para mostrar el precio de cada película en soles **y en dólares**. Sirve para atender turistas sin sacar la calculadora.

```json
{"monedaBase":"USD","monedaDestino":"PEN","valor":3.402424}
```

### Consulta de DNI — gratuita, requiere token

`https://api.apis.net.pe/v2/reniec/dni`

Busca los datos de una persona por su DNI y autocompleta el nombre al registrarla como cliente. Evita errores de tipeo y agiliza la atención en caja.

### Diseñadas para fallar bien

Una API externa puede estar caída, lenta o pedir un token vencido. Si eso pasa, `ConsultaExternaService` traduce el error a un mensaje claro y **la página sigue cargando**. Si no hay internet o falta el token, el resto del sistema funciona con total normalidad.

Programar contra un servicio ajeno obliga a asumir que algún día no va a responder. El sistema se degrada, no se cae.

---

## Cómo ejecutar el proyecto

**Requisitos previos:**
- Java 21 instalado
- MySQL 8 corriendo en `localhost:3306`
- Base de datos `cine_db` creada (vacía es suficiente)

```bash
mvn spring-boot:run
```

Al iniciar por primera vez, el sistema crea automáticamente las tablas, los usuarios, las salas, los asientos, películas, clientes y reservas de demostración. No hay que ejecutar ningún script SQL.

| URL | Descripción |
|---|---|
| `http://localhost:8080/` | Página pública del cine |
| `http://localhost:8080/reservar` | Módulo de reservas para clientes |
| `http://localhost:8080/login` | Acceso al panel de administración |
| `http://localhost:8080/api/peliculas` | API REST (consulta pública) |

**Credenciales del panel:**

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin_cine` | `LaEstacion2026!` | Administrador (acceso total) |
| `cajero_cine` | `Cajero2026!` | Cajero (acceso limitado) |

### Ejecutar el cliente externo

Con Spring Boot ya levantado, en **otra terminal**:

```bash
cd cliente-externo
python -m http.server 5500
```

Y abrir `http://localhost:5500`.

### Activar la consulta de DNI (opcional)

El tipo de cambio funciona sin configurar nada. La consulta de DNI necesita un token gratuito:

1. Crear una cuenta en <https://apis.net.pe> y copiar el token.
2. Pegarlo en `src/main/resources/application.properties`:

```properties
api.dni.token=TU_TOKEN_AQUI
```

Sin token, esa sección muestra un aviso y todo lo demás sigue funcionando.
