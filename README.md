# Cine La Estación — Sistema Web de Gestión y Reservas

---

## PARTE 1 — ¿Qué es el sistema y qué problema resuelve?

Imaginen que tienen un cine y toda su gestión la hacen en papel o en hojas de Excel. Los clientes llaman para preguntar si hay asientos, el empleado tiene que revisar una lista manual, anotar el nombre, calcular el precio… Es lento, propenso a errores y muy difícil de escalar.

**Cine La Estación** es un sistema web completo que digitaliza toda esa operación.

El sistema tiene **dos caras**:

**La cara pública** — lo que cualquier cliente puede ver sin necesidad de registrarse:
- Una página de inicio con la cartelera actual, los servicios del cine, quiénes somos y contacto.
- Un módulo de reservas donde el cliente elige su película, selecciona la fecha y el horario, escoge sus asientos en un mapa visual interactivo, ingresa sus datos personales y confirma. Todo desde el navegador, sin llamar a nadie.

**La cara administrativa** — protegida por login, solo para el personal del cine:
- Un dashboard con métricas clave: ingresos totales, reservas pendientes, clientes registrados, películas en cartelera.
- Gestión completa de películas, clientes, reservas y snacks (agregar, consultar, eliminar).
- Estadísticas con gráficos de barras, líneas y circulares para analizar el negocio.
- Dos roles de usuario: Administrador (acceso total) y Cajero (acceso limitado).

El proyecto fue desarrollado con **Java 21**, **Spring Boot 3.2.5**, base de datos **MySQL** y vistas en **Thymeleaf** con estilos **Bootstrap 5**.

---

## PARTE 2 — ¿Cómo está construido? La arquitectura del sistema

El sistema sigue una arquitectura en capas, que es el estándar profesional para aplicaciones web en Java. Cada capa tiene una responsabilidad específica y no se mezcla con las demás.

```
[ Navegador del usuario ]
         ↓  petición HTTP
[ Controlador ]  ← recibe la petición y decide qué hacer
         ↓
[ Servicio ]     ← contiene la lógica del negocio
         ↓
[ Repositorio ]  ← habla directamente con la base de datos
         ↓
[ Base de datos MySQL ]
         ↑
[ Modelo / Entidad ]  ← representa las tablas de la base de datos
```

**¿Por qué esta separación?**

Si mañana queremos cambiar MySQL por otro motor de base de datos, solo tocamos los repositorios. Si queremos cambiar cómo se calcula el precio de una reserva, solo tocamos el servicio. Si queremos agregar una nueva pantalla, solo tocamos el controlador. Cada parte es independiente y reemplazable.

**Las tecnologías que se usaron y por qué:**

| Tecnología | Versión | Por qué se eligió |
|---|---|---|
| Java 21 | LTS | Última versión de soporte a largo plazo, estable y moderna |
| Spring Boot 3.2.5 | Estable | Levanta un servidor web completo con mínima configuración |
| Spring Data JPA | Incluido | Convierte objetos Java en tablas SQL automáticamente |
| Thymeleaf 3.1 | Incluido | Genera HTML dinámico en el servidor, simple y seguro |
| MySQL 8 | — | Base de datos relacional robusta y gratuita |
| Bootstrap 5.3.3 | WebJar | Diseño responsivo profesional sin escribir CSS desde cero |
| Chart.js 4.4.3 | WebJar | Gráficos interactivos en el navegador |

Un detalle importante: Bootstrap y Chart.js se sirven como **WebJars**, lo que significa que están empaquetados dentro del propio proyecto Java y no dependen de servidores externos (CDN). El sistema funciona aunque no haya internet.

---

## PARTE 3 — ¿Cómo se organizan los datos? El modelo de base de datos

Esta es la parte más importante de entender antes de hablar de cualquier funcionalidad. Los datos del sistema se organizan en **7 entidades** (tablas en la base de datos), y cada una representa algo del mundo real del cine.

**Sala** — Una sala física del cine. Tiene nombre, cantidad de filas y columnas.
*Ejemplo: "Sala Principal" con 5 filas y 8 columnas = 40 asientos.*

**Asiento** — Un asiento específico dentro de una sala. Tiene su fila (A, B, C…), número (1 al 8) y tipo (normal, preferencial o VIP). Un asiento pertenece a una sola sala.

**Pelicula** — Una película en cartelera. Tiene título, género, duración, clasificación (G, PG-13…), precio de la entrada, si es estreno, y los horarios disponibles separados por coma. Una película se proyecta en una sala específica.

**Cliente** — Una persona que hace una reserva. Solo se guarda nombre, email y teléfono. No necesita contraseña porque el sistema no requiere que los clientes se registren; se identifican por su email.

**Reserva** — El evento central del sistema. Vincula a un cliente con una película, en una fecha y hora específica, por una cantidad de asientos y un precio total. Tiene un estado: Pendiente, Confirmada o Cancelada.

**ReservaAsiento** — La tabla intermedia que dice exactamente qué asientos físicos se reservaron en cada reserva. Es necesaria porque una reserva puede ocupar varios asientos, y un asiento puede estar en muchas reservas distintas (en fechas distintas).

**Snack** — Un producto del snack bar. Tiene nombre, categoría, precio, stock y descripción.

**Relaciones entre las tablas:**

```
Sala ──── (tiene muchos) ──── Asiento
Sala ──── (asignada a) ──── Pelicula

Cliente ──── (hace) ──── Reserva
Pelicula ──── (es parte de) ──── Reserva

Reserva ──── (ocupa) ──── ReservaAsiento ──── (referencia a) ──── Asiento
```

La tabla `reserva_asiento` es la clave del sistema. Cuando alguien cancela una reserva, esa tabla se limpia y los asientos quedan libres para otra reserva en la misma fecha.

---

## PARTE 4 — ¿Qué puede hacer el sistema? Las funcionalidades principales

**Flujo de reserva pública (lo que ve el cliente):**

El proceso tiene tres pasos bien definidos, todos en la misma URL (`/reservar`), pero el sistema detecta en qué paso está según los parámetros de la URL:

```
Paso 1: /reservar
→ El cliente ve todas las películas disponibles en tarjetas.
→ Hace clic en la que quiere.

Paso 2: /reservar?peliculaId=1
→ Aparece la información de la película seleccionada.
→ El cliente elige la fecha en un calendario y el horario haciendo clic en un botón.

Paso 3: /reservar?peliculaId=1&fecha=2026-06-15 17:30
→ Aparece el mapa de asientos con dos secciones y pasillo central.
→ Los asientos ocupados aparecen en rojo y no se pueden seleccionar.
→ El cliente llena sus datos (nombre, apellido, email, teléfono).
→ Confirma la reserva y el sistema la guarda en la base de datos.
```

Si el cliente ya existe en el sistema (mismo email), se reutiliza su registro. Si es nuevo, se crea automáticamente. El cliente nunca necesita "registrarse" explícitamente.

**Panel de administración (lo que ve el personal):**

Acceso mediante `/login` con dos roles:
- `admin_cine` / `LaEstacion2026!` — Acceso completo a todo, incluyendo estadísticas y configuración.
- `cajero_cine` / `Cajero2026!` — Acceso a reservas, películas y clientes, pero no a estadísticas ni configuración.

Desde el panel, el administrador puede:
- Crear reservas manualmente eligiendo cliente, película, fecha, horario y asientos desde el mismo mapa visual que el cliente público.
- Confirmar o cancelar reservas existentes.
- Agregar y eliminar películas, clientes y snacks.
- Ver gráficos de rendimiento: ingresos por película, distribución de reservas por estado, tendencia mensual.

---

## PARTE 5 — La parte técnica avanzada: Procedimientos Almacenados

Esta es la característica más avanzada del proyecto desde el punto de vista de bases de datos.

Un **procedimiento almacenado** es un bloque de código SQL que vive dentro del servidor de base de datos MySQL, no en el código Java. Se llama por nombre desde Java y MySQL lo ejecuta. Es como tener una función en la base de datos.

En este proyecto se implementaron **4 procedimientos almacenados**, y hay una razón específica para cada uno:

**`sp_AsientosDisponibles`** — El más importante. Recibe el ID de una película y una fecha, y devuelve todos los asientos de esa sala indicando si están ocupados o disponibles para ese día. Requería un `JOIN` complejo entre 4 tablas (pelicula → sala → asiento → reserva_asiento → reserva) con una condición de exclusión de reservas canceladas. Este tipo de consulta se beneficia enormemente de estar en el motor de base de datos porque es más rápida que traer todos los datos a Java y filtrarlos ahí.

**`sp_CrearReserva`** — Inserta una nueva reserva y devuelve el ID generado automáticamente usando `LAST_INSERT_ID()`. Se usó procedimiento porque necesitamos ese ID de vuelta (parámetro OUT) de forma atómica — si alguien más inserta al mismo tiempo, `LAST_INSERT_ID()` garantiza que cada conexión obtiene su propio ID.

**`sp_CancelarReserva`** — Realiza dos operaciones en secuencia: primero elimina los registros de `reserva_asiento` para liberar los asientos, y luego actualiza el estado de la reserva a 'Cancelada'. Se usa procedimiento porque estas dos operaciones deben ocurrir juntas como una unidad. Si fallara entre medias, los asientos quedarían huérfanos.

**`sp_ResumenSala`** — Calcula cuántos asientos totales, ocupados y disponibles tiene una sala para una fecha dada. Útil para métricas internas.

Los procedimientos se crean automáticamente cada vez que arranca la aplicación, gracias a la clase `ProcedimientosLoader.java` que los registra en MySQL antes de que el sistema empiece a recibir peticiones.

```
Spring Boot arranca
        ↓
ProcedimientosLoader se ejecuta (evento ApplicationReady)
        ↓
Elimina los procedimientos viejos (DROP PROCEDURE IF EXISTS)
        ↓
Los recrea con la versión actualizada
        ↓
El sistema ya puede recibir peticiones
```

Esto garantiza que los procedimientos en MySQL siempre estén sincronizados con el código Java, sin necesidad de ejecutar scripts SQL manualmente.

---

## Cómo ejecutar el proyecto

**Requisitos previos:**
- Java 21 instalado
- MySQL 8 corriendo en localhost:3306
- Base de datos `cine_db` creada (vacía es suficiente)

```bash
# Desde la carpeta raíz del proyecto
mvn spring-boot:run
```

Luego abrir en el navegador:

| URL | Descripción |
|---|---|
| `http://localhost:8080/` | Página pública del cine |
| `http://localhost:8080/reservar` | Módulo de reservas para clientes |
| `http://localhost:8080/login` | Acceso al panel de administración |

**Credenciales del panel:**

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin_cine` | `LaEstacion2026!` | Administrador (acceso total) |
| `cajero_cine` | `Cajero2026!` | Cajero (acceso limitado) |

Al iniciar por primera vez, el sistema crea automáticamente las tablas, las salas, los asientos, películas, clientes y reservas de demostración. No es necesario ejecutar ningún script SQL.
