let idProfesorEditando = null;
let originalNombre = null;
let originalCelular = null;
let profesorSeleccionado = null; 

// ==========================================
// CONTROL DEL PANEL LATERAL (MODAL)
// ==========================================
function abrirModal() {
    document.getElementById('modalProfesor').classList.add('activo');
}

function cerrarModal() {
    document.getElementById('modalProfesor').classList.remove('activo');
    
    document.getElementById("nombreProfesor").value = "";
    document.getElementById("celularProfesor").value = "";
    let divError = document.getElementById("mensajeError");
    if (divError) divError.innerText = "";
    
    idProfesorEditando = null;
    profesorSeleccionado = null;

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = true;
        btnActualizar.classList.remove("btn-primario");
        btnActualizar.classList.add("btn-secundario");
    }
    
    let todasLasFilas = document.querySelectorAll("#cuerpoTablaProfesor tr");
    todasLasFilas.forEach(f => f.classList.remove("fila-seleccionada"));
}

document.getElementById('btnRegistrar').addEventListener('click', () => {
    document.getElementById('modalTitulo').innerText = "Registrar Nuevo Profesor";
    abrirModal();
});

document.getElementById('btnActualizar').addEventListener('click', () => {
    if (profesorSeleccionado !== null) {
        prepararEdicion(profesorSeleccionado.id, profesorSeleccionado.nombre, profesorSeleccionado.celular);
    }
});

// Escuchar cambios en el selector de filtros
document.getElementById('filtroEstado').addEventListener('change', () => {
    cargarTabla();
});

// ==========================================
// LÓGICA DE SELECCIÓN DE FILA
// ==========================================
function seleccionarFila(filaHtml, id, nombre, celular) {
    let todasLasFilas = document.querySelectorAll("#cuerpoTablaProfesor tr");
    todasLasFilas.forEach(f => f.classList.remove("fila-seleccionada"));

    filaHtml.classList.add("fila-seleccionada");
    profesorSeleccionado = { id, nombre, celular };

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
    let tbody = document.getElementById("cuerpoTablaProfesor");
    let filtro = document.getElementById("filtroEstado").value;
    let filahtml = "";
    
    // Determinamos a qué ruta consultar según el filtro
    let ruta = '/api/profesores';
    if (filtro === 'activos') ruta = '/api/profesores/activos';
    if (filtro === 'inactivos') ruta = '/api/profesores/inactivos';

    try {
        let profesores = await peticionApi(ruta);

        profesores.forEach(function(p) {
            let claseEstado = p.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
            
            // En vez de rutas /suspender, mandamos el número del nuevoEstado (1=ACTIVO, 2=INACTIVO)
            let botonAccion = p.estado === "ACTIVO" 
                ? `<button type="button" class="btn-tabla btn-suspender" onclick="cambiarEstado(event, ${p.id}, 2)">Inactivar</button>` 
                : `<button type="button" class="btn-tabla btn-reactivar" onclick="cambiarEstado(event, ${p.id}, 1)">Activar</button>`;
            
            filahtml += ` 
            <tr onclick="seleccionarFila(this, ${p.id}, '${p.nombre}', '${p.celular}')">
                <td class="col-codigo">${p.id}</td>
                <td class="col-nombre">${p.nombre}</td>
                <td class="col-numero">${p.celular}</td>
                <td><span class="${claseEstado}">${p.estado}</span></td>
                <td><div style="display:flex; gap:8px;">${botonAccion}</div></td>
            </tr>
            `;
        });
        tbody.innerHTML = filahtml;
    } catch(error) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:red; padding:20px;">Error: No se pudo conectar con el servidor</td></tr>`;
    }
}

async function registrarProfesor() {
    let nombre = document.getElementById("nombreProfesor").value;
    let celular = document.getElementById("celularProfesor").value;
    
    if (nombre === null || nombre.trim() === ""){ alert("El nombre está vacío"); return; }
    if (celular === null || celular.trim() === ""){ alert("El celular está vacío"); return; }

    try {
        if (idProfesorEditando === null){
            // POST
            await peticionApi('/api/profesores', 'POST', { nombre: nombre, celular: celular });
        } else {
            // PUT: se envía solo lo que cambió; el estado va por activar/inactivar
            const cambios = {};
            if (nombre !== originalNombre) cambios.nuevoNombre = nombre;
            if (celular !== originalCelular) cambios.nuevoCelular = celular;

            if (Object.keys(cambios).length > 0) {
                await peticionApi(`/api/profesores/${idProfesorEditando}`, 'PUT', cambios);
            }
        }
        
        cerrarModal();
        cargarTabla();

    } catch (error) {
        let divError = document.getElementById("mensajeError");
        if(divError) divError.innerText = error.message || "No se pudo comunicar con el servidor.";
    }
}

async function cambiarEstado(event, id, estadoNumerico) {
    if(event) event.stopPropagation();
    try {
        // Hacemos un PUT normal, enviando un null en nombre y celular, y enviando el número del nuevo estado
        const actualizacionDeEstado = {
            nuevoNombre: null,
            nuevoCelular: null,
            nuevoEstado: estadoNumerico
        };
        await peticionApi(`/api/profesores/${id}`, 'PUT', actualizacionDeEstado);
        cargarTabla();
    } catch (error) {
        alert("Error: " + error.message); 
    }
}

function prepararEdicion(id, nombre, celular) {
    idProfesorEditando = id;
    originalNombre = nombre;
    originalCelular = celular;
    
    document.getElementById("nombreProfesor").value = nombre;
    document.getElementById("celularProfesor").value = celular;
    
    document.getElementById("modalTitulo").innerText = "Editar Profesor";
    let divError = document.getElementById("mensajeError");
    if (divError) divError.innerHTML = "";
    
    abrirModal();
}

// ==========================================
// CONTROL DEL MODAL CENTRADO (BÚSQUEDA)
// ==========================================
function cerrarModalDetalle() {
    document.getElementById('modalDetalleProfesor').classList.remove('activo');
}

// Asegúrate de que el input en tu HTML de profesor tenga el id="buscarProfesor"
document.getElementById('btnBuscar').addEventListener('click', async () => {
    let idBuscado = document.getElementById('buscarProfesor').value.trim();
    
    if (idBuscado === "") {
        alert("Ingrese un ID para buscar.");
        return;
    }

    try {
        // Hacemos la petición GET directa a la ruta de profesores
        let profesor = await peticionApi(`/api/profesores/${idBuscado}`);
        if (profesor) {
            mostrarDetalleProfesor(profesor);
        }
    } catch (error) {
        console.error("Error en la búsqueda:", error);
        alert("No se encontró ningún profesor con el ID: " + idBuscado);
    }
});

function mostrarDetalleProfesor(p) {
    let claseEstado = p.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
    
    let htmlDetalle = `
        <p><strong>ID:</strong> <span style="font-size: 0.85rem;">${p.id}</span></p>
        <p><strong>Nombre:</strong> ${p.nombre}</p>
        <p><strong>Celular:</strong> ${p.celular}</p>
        <p><strong>Estado:</strong> <span class="${claseEstado}">${p.estado}</span></p>
    `;

    document.getElementById('detalleContenido').innerHTML = htmlDetalle;
    document.getElementById('modalDetalleProfesor').classList.add('activo');
}

// Inicializamos la tabla
cargarTabla();