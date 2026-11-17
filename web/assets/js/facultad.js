let idFacultadEditando = null;
let originalNombre = null;
let facultadSeleccionada = null; 

// ==========================================
// CONTROL DEL PANEL LATERAL (MODAL)
// ==========================================
function abrirModal() {
    document.getElementById('modalFacultad').classList.add('activo');
}

function cerrarModal() {
    document.getElementById('modalFacultad').classList.remove('activo');
    
    document.getElementById("nombreFacultad").value = "";
    let divError = document.getElementById("mensajeError");
    if (divError) divError.innerText = "";
    
    idFacultadEditando = null;
    facultadSeleccionada = null;

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = true;
        btnActualizar.classList.remove("btn-primario");
        btnActualizar.classList.add("btn-secundario");
    }
    
    let todasLasFilas = document.querySelectorAll("#cuerpoTablaFacultad tr");
    todasLasFilas.forEach(f => f.classList.remove("fila-seleccionada"));
}

document.getElementById('btnRegistrar').addEventListener('click', () => {
    document.getElementById('modalTitulo').innerText = "Registrar Nueva Facultad";
    abrirModal();
});

document.getElementById('btnActualizar').addEventListener('click', () => {
    if (facultadSeleccionada !== null) {
        prepararEdicion(facultadSeleccionada.id, facultadSeleccionada.nombre);
    }
});

// ==========================================
// LÓGICA DE SELECCIÓN DE FILA
// ==========================================
function seleccionarFila(filaHtml, id, nombre) {
    let todasLasFilas = document.querySelectorAll("#cuerpoTablaFacultad tr");
    todasLasFilas.forEach(f => f.classList.remove("fila-seleccionada"));

    filaHtml.classList.add("fila-seleccionada");
    facultadSeleccionada = { id, nombre };

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
    let tbody = document.getElementById("cuerpoTablaFacultad");
    let filahtml = "";
    try {
        let facultades = await peticionApi('/api/facultades');

        facultades.forEach(function(f) {
            let claseEstado = f.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
            let botonAccion = f.estado === "ACTIVO" 
                ? `<button type="button" class="btn-tabla btn-suspender" onclick="cambiarEstado(event, ${f.id}, 'suspender')">Suspender</button>` 
                : `<button type="button" class="btn-tabla btn-reactivar" onclick="cambiarEstado(event, ${f.id}, 'reactivar')">Reactivar</button>`;
            
            filahtml += ` 
            <tr onclick="seleccionarFila(this, ${f.id}, '${f.nombre}')">
                <td class="col-codigo">${f.id}</td>
                <td class="col-nombre">${f.nombre}</td>
                <td><span class="${claseEstado}">${f.estado}</span></td>
                <td><div style="display:flex; gap:8px;">${botonAccion}</div></td>
            </tr>
            `;
        });
        tbody.innerHTML = filahtml;
    } catch(error) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:red; padding:20px;">Error: No se pudo conectar con el servidor</td></tr>`;
    }
}

async function registrarFacultad() {
    let nombre = document.getElementById("nombreFacultad").value;
    if (nombre === null || nombre.trim() === ""){
         alert("El nombre está vacío"); return;
    }

    try {
        if (idFacultadEditando === null){
            // POST
            await peticionApi('/api/facultades', 'POST', { nombre: nombre });
        } else {
            // PUT: se envía solo lo que cambió
            const cambios = {};
            if (nombre !== originalNombre) cambios.nuevoNombre = nombre;

            if (Object.keys(cambios).length > 0) {
                await peticionApi(`/api/facultades/${idFacultadEditando}`, 'PUT', cambios);
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
        await peticionApi(`/api/facultades/${id}/${accion}`, 'PUT');
        cargarTabla();
    } catch (error) {
        alert("Error: " + error.message); 
    }
}

function prepararEdicion(id, nombre) {
    idFacultadEditando = id;
    originalNombre = nombre;
    
    document.getElementById("nombreFacultad").value = nombre;
    document.getElementById("modalTitulo").innerText = "Editar Facultad";
    let divError = document.getElementById("mensajeError");
    if (divError) divError.innerHTML = "";
    
    abrirModal();
}

// Inicializamos la tabla
cargarTabla();