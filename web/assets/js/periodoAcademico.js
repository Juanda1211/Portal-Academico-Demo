let idPeriodoEditando = null;
let originalNombre = null;
let periodoSeleccionado = null; 

// ==========================================
// CONTROL DEL PANEL LATERAL (MODAL)
// ==========================================
function abrirModal() {
    document.getElementById('modalPeriodoAcademico').classList.add('activo');
}

function cerrarModal() {
    document.getElementById('modalPeriodoAcademico').classList.remove('activo');
    
    document.getElementById("nombrePeriodoAcademico").value = "";
    let divError = document.getElementById("mensajeError");
    if (divError) divError.innerText = "";
    
    idPeriodoEditando = null;
    periodoSeleccionado = null;

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = true;
        btnActualizar.classList.remove("btn-primario");
        btnActualizar.classList.add("btn-secundario");
    }
    
    let todasLasFilas = document.querySelectorAll("#cuerpoTablaPeriodoAcademico tr");
    todasLasFilas.forEach(f => f.classList.remove("fila-seleccionada"));
}

document.getElementById('btnRegistrar').addEventListener('click', () => {
    document.getElementById('modalTitulo').innerText = "Registrar Nuevo Periodo Académico";
    abrirModal();
});

document.getElementById('btnActualizar').addEventListener('click', () => {
    if (periodoSeleccionado !== null) {
        prepararEdicion(periodoSeleccionado.id, periodoSeleccionado.nombre);
    }
});

// ==========================================
// LÓGICA DE SELECCIÓN DE FILA
// ==========================================
function seleccionarFila(filaHtml, id, nombre) {
    let todasLasFilas = document.querySelectorAll("#cuerpoTablaPeriodoAcademico tr");
    todasLasFilas.forEach(f => f.classList.remove("fila-seleccionada"));

    filaHtml.classList.add("fila-seleccionada");
    periodoSeleccionado = { id, nombre };

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = false;
        btnActualizar.classList.remove("btn-secundario");
        btnActualizar.classList.add("btn-primario");
    }
}

// ==========================================
// FUNCIONES DE LA API
// ==========================================
async function cargarTabla() {
    let tbody = document.getElementById("cuerpoTablaPeriodoAcademico");
    let filahtml = "";
    try {
        let periodos = await peticionApi('/api/periodos');

        periodos.forEach(function(p) {
            let claseEstado = p.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
            let botonAccion = p.estado === "ACTIVO" 
                ? `<button type="button" class="btn-tabla btn-suspender" onclick="cambiarEstado(event, ${p.id}, 'suspender')">Suspender</button>` 
                : `<button type="button" class="btn-tabla btn-reactivar" onclick="cambiarEstado(event, ${p.id}, 'reactivar')">Reactivar</button>`;
            
            filahtml += ` 
            <tr onclick="seleccionarFila(this, ${p.id}, '${p.nombre}')">
                <td class="col-codigo">${p.id}</td>
                <td class="col-nombre">${p.nombre}</td>
                <td><span class="${claseEstado}">${p.estado}</span></td>
                <td><div style="display:flex; gap:8px;">${botonAccion}</div></td>
            </tr>
            `;
        });
        tbody.innerHTML = filahtml;
    } catch(error) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:red; padding:20px;">Error: No se pudo conectar con el servidor</td></tr>`;
    }
}

async function registrarPeriodoAcademico() {
    let nombre = document.getElementById("nombrePeriodoAcademico").value;
    if (nombre === null || nombre.trim() === ""){
         alert("El nombre está vacío"); return;
    }

    try {
        if (idPeriodoEditando === null){
            // POST
            await peticionApi('/api/periodos', 'POST', { nombre: nombre });
        } else {
            // PUT: se envía solo lo que cambió
            const cambios = {};
            if (nombre !== originalNombre) cambios.nuevoNombre = nombre;

            if (Object.keys(cambios).length > 0) {
                await peticionApi(`/api/periodos/${idPeriodoEditando}`, 'PUT', cambios);
            }
        }
        
        cerrarModal();
        cargarTabla();

    } catch (error) {
        let divError = document.getElementById("mensajeError");
        if(divError) divError.innerText = error.message || "No se pudo comunicar con el servidor.";
    }
}

async function cambiarEstado(event, id, accion) {
    if(event) event.stopPropagation();
    try {
        await peticionApi(`/api/periodos/${id}/${accion}`, 'PUT');
        cargarTabla();
    } catch (error) {
        alert("Error: " + error.message); 
    }
}

function prepararEdicion(id, nombre) {
    idPeriodoEditando = id;
    originalNombre = nombre;
    
    document.getElementById("nombrePeriodoAcademico").value = nombre;
    document.getElementById("modalTitulo").innerText = "Editar Periodo Académico";
    let divError = document.getElementById("mensajeError");
    if (divError) divError.innerHTML = "";
    
    abrirModal();
}

// Inicializamos la tabla
cargarTabla();