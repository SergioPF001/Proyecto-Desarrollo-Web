const API = "http://localhost:8080/api";

let credenciales = null;

function encabezados() {
    const cabeceras = { "Content-Type": "application/json" };
    if (credenciales) {
        cabeceras["Authorization"] = "Basic " + credenciales;
    }
    return cabeceras;
}

async function pedir(ruta, opciones = {}) {
    const respuesta = await fetch(API + ruta, { ...opciones, headers: encabezados() });

    if (respuesta.status === 401) {
        throw new Error("Necesitas iniciar sesión para usar este recurso.");
    }
    if (respuesta.status === 403) {
        throw new Error("Tu rol no tiene permisos para esta operación.");
    }
    if (respuesta.status === 204) {
        return null;
    }

    const cuerpo = await respuesta.json();
    if (!respuesta.ok) {
        throw new Error(cuerpo.mensaje || "No se pudo completar la petición.");
    }
    return cuerpo;
}

function mostrarError(id, mensaje) {
    document.getElementById(id).innerHTML = `<p class="error">${mensaje}</p>`;
}

function elemento(id) {
    return document.getElementById(id);
}

async function cargarTipoCambio() {
    try {
        const cambio = await pedir("/consultas/tipo-cambio");
        elemento("tipoCambio").innerHTML = `
            <p><strong>1 ${cambio.monedaBase} = ${cambio.valor.toFixed(3)} ${cambio.monedaDestino}</strong></p>
            <p class="ayuda">Actualizado: ${cambio.actualizado}</p>`;
    } catch (error) {
        mostrarError("tipoCambio", error.message);
    }
}

async function cargarGeneros() {
    try {
        const generos = await pedir("/peliculas/generos");
        const selector = elemento("filtroGenero");
        generos.forEach(genero => {
            const opcion = document.createElement("option");
            opcion.value = genero;
            opcion.textContent = genero;
            selector.appendChild(opcion);
        });
    } catch (error) {
        mostrarError("cartelera", error.message);
    }
}

async function cargarCartelera() {
    const genero = elemento("filtroGenero").value;
    const ruta = genero ? `/peliculas?genero=${encodeURIComponent(genero)}` : "/peliculas";

    try {
        const peliculas = await pedir(ruta);
        if (peliculas.length === 0) {
            elemento("cartelera").innerHTML = `<p class="vacio">Sin películas para ese género.</p>`;
            return;
        }

        const filas = peliculas.map(p => `
            <tr>
                <td>${p.titulo}</td>
                <td>${p.genero}</td>
                <td>${p.duracion} min</td>
                <td>${p.sala}</td>
                <td>S/ ${p.precio.toFixed(2)}</td>
                <td>${p.horarios.join(", ")}</td>
            </tr>`).join("");

        elemento("cartelera").innerHTML = `
            <table>
                <thead>
                    <tr><th>Título</th><th>Género</th><th>Duración</th><th>Sala</th><th>Precio</th><th>Horarios</th></tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>`;
    } catch (error) {
        mostrarError("cartelera", error.message);
    }
}

function iniciarSesion() {
    const usuario = elemento("usuario").value;
    const contrasena = elemento("contrasena").value;

    if (!usuario || !contrasena) {
        mostrarError("estadoSesion", "Ingresa usuario y contraseña.");
        return;
    }

    credenciales = btoa(`${usuario}:${contrasena}`);
    elemento("estadoSesion").innerHTML = `<p class="exito">Credenciales guardadas para ${usuario}.</p>`;
}

function cerrarSesion() {
    credenciales = null;
    elemento("estadoSesion").innerHTML = `<p class="ayuda">Sesión cerrada.</p>`;
}

async function cargarSnacks() {
    try {
        const snacks = await pedir("/snacks");
        if (snacks.length === 0) {
            elemento("snacks").innerHTML = `<p class="vacio">Sin snacks registrados.</p>`;
            return;
        }

        const filas = snacks.map(s => `
            <tr>
                <td>${s.nombre}</td>
                <td>${s.categoria}</td>
                <td>S/ ${s.precio.toFixed(2)}</td>
                <td>${s.stock}</td>
                <td><button class="peligro" data-id="${s.id}">Eliminar</button></td>
            </tr>`).join("");

        elemento("snacks").innerHTML = `
            <table>
                <thead>
                    <tr><th>Nombre</th><th>Categoría</th><th>Precio</th><th>Stock</th><th></th></tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>`;

        document.querySelectorAll("#snacks button.peligro").forEach(boton => {
            boton.addEventListener("click", () => eliminarSnack(boton.dataset.id));
        });
    } catch (error) {
        mostrarError("snacks", error.message);
    }
}

async function crearSnack() {
    const snack = {
        nombre: elemento("snackNombre").value,
        categoria: elemento("snackCategoria").value,
        precio: Number(elemento("snackPrecio").value),
        stock: Number(elemento("snackStock").value),
        descripcion: ""
    };

    if (!snack.nombre || !snack.categoria) {
        mostrarError("snacks", "El nombre y la categoría son obligatorios.");
        return;
    }

    try {
        await pedir("/snacks", { method: "POST", body: JSON.stringify(snack) });
        elemento("snackNombre").value = "";
        elemento("snackCategoria").value = "";
        elemento("snackPrecio").value = "";
        elemento("snackStock").value = "";
        await cargarSnacks();
    } catch (error) {
        mostrarError("snacks", error.message);
    }
}

async function eliminarSnack(id) {
    try {
        await pedir(`/snacks/${id}`, { method: "DELETE" });
        await cargarSnacks();
    } catch (error) {
        mostrarError("snacks", error.message);
    }
}

async function cargarReservas() {
    try {
        const reservas = await pedir("/reservas");
        if (reservas.length === 0) {
            elemento("reservas").innerHTML = `<p class="vacio">Sin reservas registradas.</p>`;
            return;
        }

        const filas = reservas.map(r => `
            <tr>
                <td>${r.id}</td>
                <td>${r.clienteNombre}</td>
                <td>${r.peliculaTitulo}</td>
                <td>${r.fecha}</td>
                <td>${r.asientosDetalle}</td>
                <td>S/ ${r.total.toFixed(2)}</td>
                <td>${r.estado}</td>
            </tr>`).join("");

        elemento("reservas").innerHTML = `
            <table>
                <thead>
                    <tr><th>#</th><th>Cliente</th><th>Película</th><th>Fecha</th><th>Asientos</th><th>Total</th><th>Estado</th></tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>`;
    } catch (error) {
        mostrarError("reservas", error.message);
    }
}

elemento("btnTipoCambio").addEventListener("click", cargarTipoCambio);
elemento("btnCartelera").addEventListener("click", cargarCartelera);
elemento("btnIngresar").addEventListener("click", iniciarSesion);
elemento("btnSalir").addEventListener("click", cerrarSesion);
elemento("btnSnacks").addEventListener("click", cargarSnacks);
elemento("btnCrearSnack").addEventListener("click", crearSnack);
elemento("btnReservas").addEventListener("click", cargarReservas);

cargarGeneros();
cargarCartelera();
