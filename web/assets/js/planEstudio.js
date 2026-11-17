let idPlanEditando = null;
let originalVersion = null;
let planSeleccionado = null;

// ==========================================
// CONTROL DEL PANEL LATERAL (MODAL)
// ==========================================
function abrirModal() { document.getElementById('modalPlanEstudio').classList.add('activo'); }

function cerrarModal() {
    document.getElementById('modalPlanEstudio').classList.remove('activo');
    document.getElementById("versionPlanEstudio").value = "";
    document.getElementById("programaPlanEstudio").value = "";
    
    // Desbloqueamos el selector de programa por si venía de una edición
    document.getElementById("programaPlanEstudio").disabled = false;
    
    if (document.getElementById("mensajeError")) document.getElementById("mensajeError").innerText = "";
    idPlanEditando = null;
    planSeleccionado = null;

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = true;
        btnActualizar.classList.replace("btn-primario", "btn-secundario");
    }
    document.querySelectorAll("#cuerpoTablaPlanEstudio tr").forEach(f => f.classList.remove("fila-seleccionada"));
}

document.getElementById('btnRegistrar').addEventListener('click', () => {
    document.getElementById('modalTitulo').innerText = "Registrar Nuevo Plan de Estudio";
    abrirModal();
});

document.getElementById('btnActualizar').addEventListener('click', () => {
    if (planSeleccionado !== null) {
        prepararEdicion(
            planSeleccionado.id, 
            planSeleccionado.version, 
            planSeleccionado.idPrograma
        );
    }
});

// ==========================================
// BÚSQUEDA Y FILTROS
// ==========================================
document.getElementById('filtroPrograma').addEventListener('change', cargarTabla);

document.getElementById('btnBuscar').addEventListener('click', async () => {
    let idBuscado = document.getElementById("buscarPlanEstudio").value.trim();
    if (!idBuscado) {
        alert("Ingrese el ID del plan para buscar.");
        return;
    }
    if (!/^\d+$/.test(idBuscado)) {
        alert("El ID del plan debe ser un número entero.");
        return;
    }

    try {
        // Hacemos la petición GET directa al backend
        let plan = await peticionApi(`/api/planes-estudio/${idBuscado}`);
        if (plan) {
            mostrarDetallePlan(plan);
        }
    } catch (error) {
        console.error("Error en búsqueda: ", error);
        alert("No se encontró ningún plan de estudio con el ID: " + idBuscado);
    }
});

// ==========================================
// LÓGICA DE SELECCIÓN DE FILA
// ==========================================
function seleccionarFila(filaHtml, id, version, idPrograma) {
    document.querySelectorAll("#cuerpoTablaPlanEstudio tr").forEach(f => f.classList.remove("fila-seleccionada"));
    filaHtml.classList.add("fila-seleccionada");
    planSeleccionado = { id, version, idPrograma };

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = false;
        btnActualizar.classList.replace("btn-secundario", "btn-primario");
    }
}

// ==========================================
// FUNCIONES DE LA API
// ==========================================
async function cargarProgramas() {
    try {
        let programas = await peticionApi('/api/programas');
        let opcionesFormulario = '<option value="">Seleccione un programa...</option>';
        let opcionesFiltro = '<option value="">Todos los programas</option>';

        programas.forEach(p => {
            opcionesFiltro += `<option value="${p.id}">${p.nombre}</option>`;
            // Solo dejamos seleccionar programas ACTIVOS para nuevos planes
            if (p.estado === "ACTIVO") opcionesFormulario += `<option value="${p.id}">${p.nombre}</option>`;
        });
        document.getElementById('programaPlanEstudio').innerHTML = opcionesFormulario;
        document.getElementById('filtroPrograma').innerHTML = opcionesFiltro;
    } catch (error) {
        console.error("Error al cargar los programas:", error);
    }
}

async function cargarTabla() {
    let filtroIdPrograma = document.getElementById("filtroPrograma").value;
    
    // Si hay un filtro seleccionado, usamos la ruta de búsqueda por programa
    let ruta = filtroIdPrograma ? `/api/planes-estudio/programa/${filtroIdPrograma}` : '/api/planes-estudio';

    try {
        let planes = await peticionApi(ruta);
        renderizarTabla(planes);
    } catch(error) {
        console.error("El error real es: ", error);
        document.getElementById("cuerpoTablaPlanEstudio").innerHTML = `<tr><td colspan="6" style="text-align:center; color:red; padding:20px;">Error: ${error.message}</td></tr>`;
    }
}

// Función separada para pintar la tabla (nos sirve para recargar todo o para la búsqueda de 1 ID)
function renderizarTabla(planes) {
    let tbody = document.getElementById("cuerpoTablaPlanEstudio");
    let filahtml = "";
    
    planes.forEach(function(p) {
        let fechaCorta = p.fechaCreacion ? p.fechaCreacion.slice(0, 10) : "N/A";
        
        let esActivo = p.estado === "ACTIVO";
        let claseEstado = esActivo ? "estado-activo" : "estado-inactivo";
        let textoBoton = esActivo ? "Inactivar" : "Activar";
        let claseBoton = esActivo ? "btn-suspender" : "btn-activar";
        
        // Aseguramos de que el programa existe antes de sacar su nombre
        let nombrePrograma = p.programa ? p.programa.nombre : "N/A";
        let idPrograma = p.programa ? p.programa.id : null;

        let botonAccion = `<button type="button" class="btn-tabla ${claseBoton}" onclick="cambiarEstadoPlan(event, '${p.id}', ${esActivo})">${textoBoton}</button>`;
        
        filahtml += ` 
        <tr onclick="seleccionarFila(this, '${p.id}', '${p.version}', '${idPrograma ?? ""}')">
            <td class="col-codigo">${p.id}</td> <!-- ID Completo -->
            <td class="col-nombre">${p.version}</td>
            <td>${nombrePrograma}</td>
            <td><span class="badge ${claseEstado}">${p.estado}</span></td>
            <td>${fechaCorta}</td>
            <td><div style="display:flex; gap:8px;">${botonAccion}</div></td>
        </tr>`;
    });
    tbody.innerHTML = filahtml;
}

async function registrarPlanEstudio() {
    let version = document.getElementById("versionPlanEstudio").value;
    let idPrograma = document.getElementById("programaPlanEstudio").value;
    
    if (!version || !idPrograma) { alert("Todos los campos son obligatorios."); return; }
    
    try {
        if (idPlanEditando === null) {
            // CREAR (POST)
            await peticionApi('/api/planes-estudio', 'POST', { version, idPrograma });
        } else if (version === originalVersion) {
            // El backend solo actualiza cuando la versión cambia de verdad.
            let mensaje = document.getElementById("mensajeError");
            if (mensaje) mensaje.innerText = "La versión no cambió, no hay nada que actualizar.";
            return;
        } else {
            // ACTUALIZAR (PUT) - el backend solo permite cambiar la versión
            await peticionApi(`/api/planes-estudio/${idPlanEditando}`, 'PUT', {
                nuevaVersion: version
            });
        }
        cerrarModal();
        cargarTabla();
    } catch (error) {
        if(document.getElementById("mensajeError")) document.getElementById("mensajeError").innerText = error.message || "No se pudo comunicar con el servidor.";
    }
}

async function cambiarEstadoPlan(event, id, esActivo) {
    if(event) event.stopPropagation();
    
    let accion = esActivo ? "inactivar" : "activar";
    if(confirm(`¿Está seguro de que desea ${accion} este plan de estudio?`)) {
        try {
            await peticionApi(`/api/planes-estudio/${id}/${accion}`, 'PUT');
            cargarTabla();
        } catch (error) { 
            alert("Error: " + error.message); 
        }
    }
}

function prepararEdicion(id, version, idPrograma) {
    idPlanEditando = id; 
    originalVersion = version; 
    
    document.getElementById("versionPlanEstudio").value = version;
    document.getElementById("programaPlanEstudio").value = idPrograma;
    
    // Bloqueamos el selector de programa (normalmente no se cambia el programa de un plan ya creado)
    document.getElementById("programaPlanEstudio").disabled = true;
    
    document.getElementById("modalTitulo").innerText = "Editar Plan de Estudio";
    if (document.getElementById("mensajeError")) document.getElementById("mensajeError").innerHTML = "";
    abrirModal();
}

async function inicializarPagina() {
    await cargarProgramas();
    cargarTabla();
}

// ==========================================
// CONTROL DEL MODAL CENTRADO (DETALLES)
// ==========================================
function mostrarDetallePlan(p) {
    // 1. Calculamos el estado del Plan
    let claseEstadoPlan = p.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
    let nombrePrograma = p.programa ? p.programa.nombre : "N/A";
    let fechaCorta = p.fechaCreacion ? p.fechaCreacion.slice(0, 10) : "N/A";
    
    // 2. Calculamos los estados del Programa y la Facultad de forma independiente
    let claseEstadoPrograma = p.programa.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
    let claseEstadoFacultad = p.programa.facultad.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
    
    let htmlDetalle = `
        <p><strong>ID:</strong> <span style="font-size: 0.85rem;">${p.id}</span></p>
        <p><strong>Versión:</strong> ${p.version}</p>
        <p><strong>Programa:</strong> ${nombrePrograma}</p>
        <p><strong>Fecha de Creación:</strong> ${fechaCorta}</p>
        <p><strong>Estado:</strong> <span class="${claseEstadoPlan}">${p.estado}</span></p>

        <div class="tarjeta-info">
            <h4 style="margin-bottom: 12px; color: #002f6c;">Pertenece a:</h4>

            <p><strong>UUID (Programa):</strong> <span style="font-size: 0.85rem;">${p.programa.id}</span></p>
            <p><strong>Programa:</strong> ${p.programa.nombre}</p>
            <p><strong>Nivel Académico:</strong> ${p.programa.nivelPrograma}</p>
            <p><strong>Estado:</strong> <span class="${claseEstadoPrograma}">${p.programa.estado}</span></p>

            <p><strong>ID Facultad:</strong> ${p.programa.facultad.id}</p>
            <p><strong>Nombre Facultad:</strong> ${p.programa.facultad.nombre}</p>
            <p><strong>Estado Facultad:</strong> <span class="${claseEstadoFacultad}">${p.programa.facultad.estado}</span></p>
        </div>
    `;

    document.getElementById('detalleContenido').innerHTML = htmlDetalle;
    document.getElementById('modalDetallePlan').classList.add('activo');
}

function cerrarModalDetalle() {
    document.getElementById('modalDetallePlan').classList.remove('activo');
}

inicializarPagina();