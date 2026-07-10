# Diagramas de Flujo - Sistema de Reservas de Asientos
## Cine La Estación — Spring Boot + Thymeleaf

---

## 1. Flujo Público — Reserva desde la Web (Cliente)

```mermaid
flowchart TD
    A([Cliente accede a /reservar]) --> B[GET /reservar\nsin parámetros]
    B --> C[Muestra grid de películas\ndisponibles]
    C --> D{¿Cliente selecciona película?}
    D --> |Sí| E[GET /reservar?peliculaId=X]
    E --> F[Muestra formulario de fecha\ny selector de horarios]
    F --> G{¿Selecciona fecha y horario?}
    G --> |Sí| H[GET /reservar?peliculaId=X\n&fecha=YYYY-MM-DD HH:mm]
    H --> I[AsientoService\n.asientosDisponibles\npeliculaId, fecha]
    I --> J[(sp_AsientosDisponibles\nMySQL Procedure)]
    J --> K{Consulta sala asignada\na la película}
    K --> L[Lista todos los asientos\nde la sala]
    L --> M{¿Existe ReservaAsiento\npara esa función\ncon estado != Cancelada?}
    M --> |Sí| N[Estado = 'ocupado']
    M --> |No| O[Estado = 'disponible']
    N --> P[Retorna mapa de asientos\ncon estados]
    O --> P
    P --> Q[Renderiza mapa visual\nde asientos en reservar.html]
    Q --> R{Cliente selecciona asientos\nen el mapa JS}
    R --> |Click en asiento disponible| S[toggleAsiento: marca seleccionado\ncss: asiento-seleccionado]
    S --> T[actualizarResumen:\ncalcula precio total]
    T --> U[Cliente llena datos:\nnombre, apellido, email, teléfono]
    U --> V{¿Envía el formulario?}
    V --> |POST /reservar| W[PublicController\n.procesarReserva]
    W --> X{¿Existe cliente\npor email?}
    X --> |No| Y[ClienteRepository\n.save - crea nuevo cliente]
    X --> |Sí| Z[Usa cliente existente]
    Y --> AA[AsientoService\n.crearReserva]
    Z --> AA
    AA --> AB[(sp_CrearReserva\nMySQL Procedure)]
    AB --> AC[INSERT INTO reserva\nRetorna reservaId]
    AC --> AD[AsientoService\n.guardarAsientosReserva\nreservaId, asientoIds]
    AD --> AE[Guarda ReservaAsiento\npor cada asiento seleccionado]
    AE --> AF[Redirect /reservar?exito=true]
    AF --> AG([Muestra mensaje de éxito\nal cliente])
```

---

## 2. Flujo Administrativo — Reserva desde el Panel Admin

```mermaid
flowchart TD
    A([Admin accede a /admin/reservas]) --> B{¿Tiene sesión\nválida?}
    B --> |No| C[Redirect /login]
    B --> |Sí| D[GET /admin/reservas\nListado de reservas]
    D --> E[Admin completa formulario:\n- Seleccionar cliente\n- Seleccionar película\n- Fecha y horario\n- Estado: Pendiente/Confirmada]
    E --> F[GET /admin/asientos\n?peliculaId=X&fecha=Y\n&clienteId=Z&estado=Pendiente]
    F --> G[AdminController\n.mostrarAsientos]
    G --> H[AsientoService\n.asientosDisponibles\npeliculaId, fecha]
    H --> I[(sp_AsientosDisponibles\nMySQL Procedure)]
    I --> J[Retorna mapa de asientos\ncon estado disponible/ocupado]
    J --> K[Renderiza admin/asientos.html\ncon panel de selección]
    K --> L{Admin selecciona asientos\nen el mapa]
    L --> M[POST /admin/reservas/nueva\ncon asientoIds como CSV]
    M --> N[parsearIds: convierte\nCSV a List de Long]
    N --> O[AsientoService\n.crearReserva]
    O --> P[(sp_CrearReserva\nMySQL Procedure)]
    P --> Q[INSERT INTO reserva\nRetorna reservaId]
    Q --> R[AsientoService\n.guardarAsientosReserva]
    R --> S[INSERT INTO reserva_asiento\npor cada asiento]
    S --> T[Redirect /admin/reservas]
    T --> U[Muestra tabla actualizada\nde reservas]
```

---

## 3. Ciclo de Vida de una Reserva — Estados

```mermaid
stateDiagram-v2
    [*] --> Pendiente : POST /reservar o\nPOST /admin/reservas/nueva\nse inserta con estado inicial

    Pendiente --> Confirmada : GET /admin/reservas/confirmar/id\nReservaService.confirmar(id)\nUPDATE estado = Confirmada

    Pendiente --> Cancelada : GET /admin/reservas/cancelar/id\nsp_CancelarReserva\nELIMINA reserva_asiento\nUPDATE estado = Cancelada

    Confirmada --> Cancelada : GET /admin/reservas/cancelar/id\nsp_CancelarReserva\nlibera asientos

    Cancelada --> [*] : GET /admin/reservas/eliminar/id\nReservaService.eliminar(id)\nDELETE FROM reserva

    Pendiente --> [*] : GET /admin/reservas/eliminar/id\nDELETE FROM reserva
```

---

## 4. Diagrama de Entidades — Modelo de Datos

```mermaid
erDiagram
    SALA {
        Long id PK
        String nombre
        int filas
        int columnas
    }
    ASIENTO {
        Long id PK
        Long sala_id FK
        String fila
        int numero
        String tipo
    }
    CLIENTE {
        Long id PK
        String nombre
        String email
        String telefono
    }
    PELICULA {
        Long id PK
        String titulo
        String genero
        int duracion
        String clasificacion
        double precio
        boolean esEstreno
        String horarios
        Long sala_id FK
    }
    RESERVA {
        Long id PK
        Long cliente_id FK
        Long pelicula_id FK
        String fecha
        int asientos
        double total
        String estado
    }
    RESERVA_ASIENTO {
        Long id PK
        Long reserva_id FK
        Long asiento_id FK
    }

    SALA ||--o{ ASIENTO : "tiene"
    SALA ||--o{ PELICULA : "asignada a"
    CLIENTE ||--o{ RESERVA : "realiza"
    PELICULA ||--o{ RESERVA : "incluye"
    RESERVA ||--o{ RESERVA_ASIENTO : "ocupa"
    ASIENTO ||--o{ RESERVA_ASIENTO : "asignado en"
```

---

## 5. Flujo de Consulta de Disponibilidad — Procedimiento sp_AsientosDisponibles

```mermaid
flowchart TD
    A([Llamada AsientoService\n.asientosDisponibles\npeliculaId, fecha]) --> B[JdbcTemplate.call\nsp_AsientosDisponibles\np_pelicula_id, p_fecha]
    B --> C{Obtener sala\nasignada a la película}
    C --> D[SELECT sala_id FROM pelicula\nWHERE id = p_pelicula_id]
    D --> E[SELECT todos los asientos\nde esa sala\nORDER BY fila, numero]
    E --> F{Para cada asiento\n¿Existe en reserva_asiento\nunido a reserva activa?}
    F --> |JOIN reserva WHERE estado != Cancelada\ny pelicula_id = X y fecha = Y| G{¿Encontrado?}
    G --> |Sí| H[estado = 'ocupado'\nCSS: asiento-ocupado]
    G --> |No| I[estado = 'disponible'\nCSS: asiento-disponible]
    H --> J[Retorna List Map\nid, fila, numero, tipo, estado]
    I --> J
    J --> K[Controller renderiza\nmapa visual en HTML\ncon Thymeleaf th:each]
```

---

## 6. Flujo de Cancelación — sp_CancelarReserva

```mermaid
flowchart TD
    A([Admin hace click en\nCancelar Reserva]) --> B[GET /admin/reservas\n/cancelar/id]
    B --> C{¿Tiene sesión\nválida con rol?}
    C --> |No| D[Redirect /login]
    C --> |Sí| E[AsientoService\n.cancelarReserva\nreservaId]
    E --> F[JdbcTemplate.call\nsp_CancelarReserva\np_reserva_id]
    F --> G[DELETE FROM reserva_asiento\nWHERE reserva_id = p_reserva_id]
    G --> H[UPDATE reserva\nSET estado = Cancelada\nWHERE id = p_reserva_id]
    H --> I[Asientos quedan libres\npara nuevas reservas]
    I --> J[Redirect /admin/reservas]
    J --> K([Tabla muestra reserva\ncon badge Cancelada\nAsientos disponibles nuevamente])
```

---

## 7. Arquitectura de Capas — Flujo de una Solicitud

```mermaid
flowchart LR
    subgraph Cliente
        A[Navegador\nHTML + JS]
    end

    subgraph Capa Presentación
        B[Thymeleaf\nreservar.html\nadmin/asientos.html\nadmin/reservas.html]
    end

    subgraph Capa Controladores
        C[PublicController\nGET/POST /reservar]
        D[AdminController\nGET/POST /admin/reservas\nGET /admin/asientos]
    end

    subgraph Capa Servicios
        E[ReservaService\nlistar, agregar\nbuscar, eliminar, confirmar]
        F[AsientoService\nasientosDisponibles\ncrearReserva\nguardarAsientosReserva\ncancelarReserva]
        G[ClienteService\nlistar, agregar\nbuscar, findByEmail]
    end

    subgraph Capa Repositorios
        H[ReservaRepository\nJpaRepository]
        I[ReservaAsientoRepository\nfindByReservaId\ndeleteByReservaId]
        J[AsientoRepository\nfindBySalaId]
        K[ClienteRepository\nfindByEmail]
    end

    subgraph Base de Datos
        L[(MySQL\ncine_db)]
        M[sp_AsientosDisponibles]
        N[sp_CrearReserva]
        O[sp_CancelarReserva]
    end

    A <--> B
    B <--> C
    B <--> D
    C --> E
    C --> F
    C --> G
    D --> E
    D --> F
    D --> G
    E --> H
    F --> I
    F --> J
    G --> K
    H --> L
    I --> L
    J --> L
    K --> L
    F --> M
    F --> N
    F --> O
    M --> L
    N --> L
    O --> L
```

---

## 8. Selección de Asientos — Lógica JavaScript en el Frontend

```mermaid
flowchart TD
    A([Usuario hace click\nen un asiento]) --> B{handleSeat\nasiento}
    B --> C{¿Tiene clase\nasiento-ocupado?}
    C --> |Sí| D[No hace nada\nAsiento no disponible]
    C --> |No| E[toggleAsiento\nasientoId, precio, btn]
    E --> F{¿Tiene clase\nasiento-seleccionado?}
    F --> |Sí, ya estaba seleccionado| G[Quitar clase\nasiento-seleccionado\nAgregar asiento-disponible]
    G --> H[Eliminar asientoId\nde array seleccionados]
    H --> I[actualizarResumen\nprecio negativo]
    F --> |No, no estaba seleccionado| J[Quitar clase\nasiento-disponible\nAgregar asiento-seleccionado]
    J --> K[Agregar asientoId\nal array seleccionados]
    K --> L[actualizarResumen\nprecio positivo]
    I --> M{¿Array seleccionados\ntiene elementos?}
    L --> M
    M --> |Sí| N[Habilitar botón\nContinuar / Reservar]
    M --> |No| O[Deshabilitar botón\nContinuar / Reservar]
    N --> P[Actualizar campo hidden\nasientoIds con CSV\neg: 1,4,7]
    O --> P
    P --> Q[Actualizar resumen visual:\n- Asientos seleccionados\n- Total a pagar]
```

---

## Notas Técnicas

| Componente | Tecnología | Detalle |
|---|---|---|
| Backend | Spring Boot 3.2.5 | Java, MVC, JPA/Hibernate |
| Base de Datos | MySQL (cine_db) | Procedimientos almacenados para operaciones críticas |
| Frontend | Thymeleaf + Bootstrap 5 | Renderizado server-side |
| Lógica JS | Vanilla JavaScript | Selección de asientos en cliente |
| Acceso a BD | JdbcTemplate + JPA | AsientoService usa JDBC para SP; demás usan JPA |
| Roles | ADMIN / CAJERO | Validados por HttpSession (hardcoded en LoginController) |
