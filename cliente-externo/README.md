# Cliente Externo

Página web hecha solo con HTML, CSS y JavaScript. **No usa Spring Boot ni Java.** Se ejecuta en un servidor
distinto y consume la API REST de `Cine La Estación` mediante `fetch()`.

## Cómo ejecutarlo

1. Levantar la aplicación Spring Boot en `http://localhost:8080`.

2. Desde esta carpeta, levantar un servidor estático en otro puerto:

```bash
python -m http.server 5500
```

3. Abrir `http://localhost:5500` en el navegador.

Como el cliente corre en el puerto `5500` y la API en el `8080`, son **orígenes distintos**. Por eso la API
declara una política CORS en `WebConfig.java` que permite las peticiones.

## Qué demuestra

| Sección | Endpoint | Seguridad |
|---|---|---|
| Tipo de cambio | `GET /api/consultas/tipo-cambio` | Público |
| Cartelera | `GET /api/peliculas` | Público |
| Reservas | `GET /api/reservas` | ADMIN o CAJERO |
| Snacks (crear / eliminar) | `POST` y `DELETE /api/snacks` | Solo ADMIN |

La autenticación se hace con **HTTP Basic**: el usuario y la contraseña se codifican en base64 y viajan
en la cabecera `Authorization` de cada petición.

Para comprobar los roles, inicia sesión con `cajero_cine` / `Cajero2026!` e intenta agregar un snack:
la API responde `403` y el cliente muestra el mensaje de permisos.
