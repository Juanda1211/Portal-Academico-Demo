let idProgramaEditando = null;
let originalNombre = null;
let originalNivel = null;
let originalIdFacultad = null;
let programaSeleccionado = null;
let listaProgramasGlobal = []; // Memoria para la búsqueda rápida

// ==========================================
// CONTROL DEL PANEL LATERAL (MODAL DE EDICIÓN)
// ==========================================
function abrirModal() { document.getElementById('modalPrograma').classList.add('activo'); }
function cerrarModal() {
    document.getElementById('modalPrograma').classList.remove('activo');
    document.getElementById("nombrePrograma").value = "";
    document.getElementById("nivelPrograma").value = "";
    document.getElementById("facultadPrograma").value = "";
    if (document.getElementById("mensajeError")) document.getElementById("mensajeError").innerText = "";
    idProgramaEditando = null;
    programaSeleccionado = null;
    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = true;
        btnActualizar.classList.replace("btn-primario", "btn-secundario");
    }
    document.querySelectorAll("#cuerpoTablaPrograma tr").forEach(f => f.classList.remove("fila-seleccionada"));
}

document.getElementById('btnRegistrar').addEventListener('click', () => {
    document.getElementById('modalTitulo').innerText = "Registrar Nuevo Programa";
    abrirModal();
});

document.getElementById('btnActualizar').addEventListener('click', () => {
    if (programaSeleccionado !== null) prepararEdicion(programaSeleccionado.id, programaSeleccionado.nombre, programaSeleccionado.nivel, programaSeleccionado.idFacultad);
});

document.getElementById('filtroFacultad').addEventListener('change', cargarTabla);

// ==========================================
// CONTROL DEL MODAL CENTRADO (BÚSQUEDA)
// ==========================================
function cerrarModalDetalle() {
    document.getElementById('modalDetallePrograma').classList.remove('activo');
}

document.getElementById('btnBuscar').addEventListener('click', () => {
    let textoBusqueda = document.getElementById('buscarPrograma').value.trim().toLowerCase();
    
    if(textoBusqueda === "") {
        alert("Ingrese un nombre o un ID para buscar.");
        return;
    }

    // Buscamos si el texto coincide exactamente con el ID, o si es parte del nombre del programa
    let programaEncontrado = listaProgramasGlobal.find(p => 
        p.id.toLowerCase() === textoBusqueda || 
        p.nombre.toLowerCase().includes(textoBusqueda)
    );

    if(programaEncontrado) {
        mostrarDetallePrograma(programaEncontrado);
    } else {
        alert("No se encontró ningún programa con ese dato.");
    }
});

function mostrarDetallePrograma(p) {
    let claseEstado = p.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
    
    // Armamos el contenido inyectando los datos del programa y de la facultad anidada
    let htmlDetalle = `
        <p><strong>UUID:</strong> <span style="font-size: 0.85rem;">${p.id}</span></p>
        <p><strong>Programa:</strong> ${p.nombre}</p>
        <p><strong>Nivel Académico:</strong> ${p.nivelPrograma}</p>
        <p><strong>Estado:</strong> <span class="${claseEstado}">${p.estado}</span></p>

        <div class="tarjeta-info">
            <h4 style="margin-bottom: 12px; color: #002f6c;">Pertenece a:</h4>
            <p><strong>ID Facultad:</strong> ${p.facultad.id}</p>
            <p><strong>Nombre Facultad:</strong> ${p.facultad.nombre}</p>
            <p><strong>Estado Facultad:</strong> ${p.facultad.estado}</p>
        </div>
    `;

    document.getElementById('detalleContenido').innerHTML = htmlDetalle;
    document.getElementById('modalDetallePrograma').classList.add('activo');
}

// ==========================================
// LÓGICA DE SELECCIÓN DE FILA
// ==========================================
function seleccionarFila(filaHtml, id, nombre, nivel, idFacultad) {
    document.querySelectorAll("#cuerpoTablaPrograma tr").forEach(f => f.classList.remove("fila-seleccionada"));
    filaHtml.classList.add("fila-seleccionada");
    programaSeleccionado = { id, nombre, nivel, idFacultad };
    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = false;
        btnActualizar.classList.replace("btn-secundario", "btn-primario");
    }
}

// ==========================================
// FUNCIONES DE LA API
// ==========================================
async function cargarFacultades() {
    try {
        let facultades = await peticionApi('/api/facultades');
        let opcionesFormulario = '<option value="">Seleccione una facultad...</option>';
        let opcionesFiltro = '<option value="">Todas las facultades</option>';

        facultades.forEach(f => {
            opcionesFiltro += `<option value="${f.id}">${f.nombre}</option>`;
            if (f.estado === "ACTIVO") opcionesFormulario += `<option value="${f.id}">${f.nombre}</option>`;
        });
        document.getElementById('facultadPrograma').innerHTML = opcionesFormulario;
        document.getElementById('filtroFacultad').innerHTML = opcionesFiltro;
    } catch (error) {
        console.error("Error al cargar las facultades:", error);
    }
}

async function cargarTabla() {
    let tbody = document.getElementById("cuerpoTablaPrograma");
    let filtroIdFacultad = document.getElementById("filtroFacultad").value;
    let filahtml = "";
    
    let ruta = filtroIdFacultad ? `/api/programas/facultad/${filtroIdFacultad}` : '/api/programas';

    try {
        let programas = await peticionApi(ruta);
        listaProgramasGlobal = programas; // Guardamos en memoria para la barra de búsqueda

        programas.forEach(function(p) {
            let claseEstado = p.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
            let botonAccion = p.estado === "ACTIVO" 
                ? `<button type="button" class="btn-tabla btn-suspender" onclick="cambiarEstado(event, '${p.id}', 'suspender')">Suspender</button>` 
                : `<button type="button" class="btn-tabla btn-reactivar" onclick="cambiarEstado(event, '${p.id}', 'reactivar')">Reactivar</button>`;

            // Atributo title="${p.id}" es el truco para ver el UUID completo al pasar el mouse
            filahtml += ` 
            <tr onclick="seleccionarFila(this, '${p.id}', '${p.nombre}', '${p.nivelPrograma}', ${p.facultad.id})">
                <td class="col-codigo">${p.id}</td>
                <td class="col-nombre">${p.nombre}</td>
                <td>${p.nivelPrograma}</td>
                <td>${p.facultad.nombre}</td>
                <td><span class="${claseEstado}">${p.estado}</span></td>
                <td><div style="display:flex; gap:8px;">${botonAccion}</div></td>
            </tr>`;
        });
        tbody.innerHTML = filahtml;
    } catch(error) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:red; padding:20px;">Error: No se pudo conectar con el servidor</td></tr>`;
    }
}

async function registrarPrograma() {
    let nombre = document.getElementById("nombrePrograma").value;
    let nivel = document.getElementById("nivelPrograma").value;
    let idFacultad = document.getElementById("facultadPrograma").value;
    
    if (!nombre || !nivel || !idFacultad) { alert("Todos los campos son obligatorios."); return; }
    idFacultad = parseInt(idFacultad);

    try {
        if (idProgramaEditando === null) {
            await peticionApi('/api/programas', 'POST', { nombre: nombre, nivelPrograma: nivel, idFacultad: idFacultad });
        } else {
            // PUT: se envía solo lo que cambió
            const cambios = {};
            if (nombre !== originalNombre) cambios.nuevoNombre = nombre;
            if (nivel !== originalNivel) cambios.nuevoNivel = nivel;
            if (idFacultad !== originalIdFacultad) cambios.nuevoIdFacultad = idFacultad;

            if (Object.keys(cambios).length > 0) {
                await peticionApi(`/api/programas/${idProgramaEditando}`, 'PUT', cambios);
            }
        }
        cerrarModal();
        cargarTabla();
    } catch (error) {
        if(document.getElementById("mensajeError")) document.getElementById("mensajeError").innerText = error.message || "No se pudo comunicar con el servidor.";
    }
}

async function cambiarEstado(event, id, accion) {
    if(event) event.stopPropagation();
    try {
        await peticionApi(`/api/programas/${id}/${accion}`, 'PUT');
        cargarTabla();
    } catch (error) { alert("Error: " + error.message); }
}

function prepararEdicion(id, nombre, nivel, idFacultad) {
    idProgramaEditando = id; originalNombre = nombre; originalNivel = nivel; originalIdFacultad = idFacultad;
    document.getElementById("nombrePrograma").value = nombre;
    document.getElementById("nivelPrograma").value = nivel;
    document.getElementById("facultadPrograma").value = idFacultad;
    document.getElementById("modalTitulo").innerText = "Editar Programa";
    if (document.getElementById("mensajeError")) document.getElementById("mensajeError").innerHTML = "";
    abrirModal();
}

async function inicializarPagina() {
    await cargarFacultades();
    const facultadDesdeMenu = new URLSearchParams(window.location.search).get('facultad');
    if (facultadDesdeMenu) {
        const filtro = document.getElementById("filtroFacultad");
        if (filtro) filtro.value = facultadDesdeMenu;
    }
    cargarTabla();
}

inicializarPagina();