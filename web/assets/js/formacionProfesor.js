let idFormacionEditando = null;
let originalTitulo = null;
let originalFecha = null;
let formacionSeleccionada = null;

// ==========================================
// CONTROL DEL PANEL LATERAL (MODAL)
// ==========================================
function abrirModal() { document.getElementById('modalFormacionProfesor').classList.add('activo'); }

function cerrarModal() {
    document.getElementById('modalFormacionProfesor').classList.remove('activo');
    document.getElementById("profesorFormacion").value = "";
    document.getElementById("tituloFormacion").value = "";
    document.getElementById("fechaFormacion").value = "";
    
    // Desbloqueamos el selector de profesor por si venía de una edición
    document.getElementById("profesorFormacion").disabled = false;
    
    if (document.getElementById("mensajeError")) document.getElementById("mensajeError").innerText = "";
    idFormacionEditando = null;
    formacionSeleccionada = null;

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = true;
        btnActualizar.classList.remove("btn-primario");
        btnActualizar.classList.add("btn-secundario");
    }
    document.querySelectorAll("#cuerpoTablaFormacionProfesor tr").forEach(f => f.classList.remove("fila-seleccionada"));
}

document.getElementById('btnRegistrar').addEventListener('click', () => {
    document.getElementById('modalTitulo').innerText = "Registrar Nueva Formación";
    abrirModal();
});

document.getElementById('btnActualizar').addEventListener('click', () => {
    if (formacionSeleccionada !== null) {
        prepararEdicion(
            formacionSeleccionada.id, 
            formacionSeleccionada.idProfesor, 
            formacionSeleccionada.titulo, 
            formacionSeleccionada.fecha
        );
    }
});

document.getElementById('filtroProfesor').addEventListener('change', cargarTabla);

// ==========================================
// LÓGICA DE SELECCIÓN DE FILA
// ==========================================
function seleccionarFila(filaHtml, id, idProfesor, titulo, fecha) {
    document.querySelectorAll("#cuerpoTablaFormacionProfesor tr").forEach(f => f.classList.remove("fila-seleccionada"));
    filaHtml.classList.add("fila-seleccionada");
    formacionSeleccionada = { id, idProfesor, titulo, fecha };

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
async function cargarProfesores() {
    try {
        let profesores = await peticionApi('/api/profesores');
        let opcionesFormulario = '<option value="">Seleccione un profesor...</option>';
        let opcionesFiltro = '<option value="">Todos los profesores</option>';

        profesores.forEach(p => {
            opcionesFiltro += `<option value="${p.id}">${p.nombre}</option>`;
            if (p.estado === "ACTIVO") opcionesFormulario += `<option value="${p.id}">${p.nombre}</option>`;
        });
        document.getElementById('profesorFormacion').innerHTML = opcionesFormulario;
        document.getElementById('filtroProfesor').innerHTML = opcionesFiltro;
    } catch (error) {
        console.error("Error al cargar los profesores:", error);
    }
}

async function cargarTabla() {
    let tbody = document.getElementById("cuerpoTablaFormacionProfesor");
    let filtroIdProfesor = document.getElementById("filtroProfesor").value;
    let filahtml = "";
    
    let ruta = filtroIdProfesor ? `/api/formaciones/profesor/${filtroIdProfesor}` : '/api/formaciones';

    try {
        let formaciones = await peticionApi(ruta);

        formaciones.forEach(function(f) {
            let fechaCorta = f.fecha.slice(0, 10); 
            let botonAccion = `<button type="button" class="btn-tabla btn-suspender" onclick="eliminarFormacion(event, ${f.id})">Eliminar</button>`;
            
            filahtml += ` 
            <tr onclick="seleccionarFila(this, ${f.id}, ${f.profesor.id}, '${f.titulo}', '${fechaCorta}')">
                <td class="col-codigo">${f.id}</td>
                <td class="col-nombre">${f.profesor.nombre}</td>
                <td>${f.titulo}</td>
                <td class="col-numero">${fechaCorta}</td>
                <td><div style="display:flex; gap:8px;">${botonAccion}</div></td>
            </tr>`;
        });
        tbody.innerHTML = filahtml;
    } catch(error) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:red; padding:20px;">Error: No se pudo conectar con el servidor</td></tr>`;
    }
}

async function registrarFormacionProfesor() {
    let idProfesor = document.getElementById("profesorFormacion").value;
    let titulo = document.getElementById("tituloFormacion").value;
    let fecha = document.getElementById("fechaFormacion").value;
    
    if (!idProfesor || !titulo || !fecha) { alert("Todos los campos son obligatorios."); return; }
    idProfesor = parseInt(idProfesor);

    try {
        if (idFormacionEditando === null) {
            await peticionApi('/api/formaciones', 'POST', { idProfesor, titulo, fecha });
        } else {
            // PUT: se envía solo lo que cambió
            const cambios = {};
            if (titulo !== originalTitulo) cambios.nuevoTitulo = titulo;
            if (fecha !== originalFecha) cambios.nuevaFecha = fecha;

            if (Object.keys(cambios).length > 0) {
                await peticionApi(`/api/formaciones/${idFormacionEditando}`, 'PUT', cambios);
            }
        }
        cerrarModal();
        cargarTabla();
    } catch (error) {
        if(document.getElementById("mensajeError")) document.getElementById("mensajeError").innerText = error.message || "No se pudo comunicar con el servidor.";
    }
}

async function eliminarFormacion(event, id) {
    if(event) event.stopPropagation();
    
    if(confirm("¿Está seguro de que desea eliminar permanentemente este título de formación?")) {
        try {
            await peticionApi(`/api/formaciones/${id}`, 'DELETE');
            cargarTabla();
        } catch (error) { 
            alert("Error: " + error.message); 
        }
    }
}

function prepararEdicion(id, idProfesor, titulo, fecha) {
    idFormacionEditando = id; originalTitulo = titulo; originalFecha = fecha;
    
    document.getElementById("profesorFormacion").value = idProfesor;
    document.getElementById("tituloFormacion").value = titulo;
    document.getElementById("fechaFormacion").value = fecha;
    document.getElementById("profesorFormacion").disabled = true;
    
    document.getElementById("modalTitulo").innerText = "Editar Formación";
    if (document.getElementById("mensajeError")) document.getElementById("mensajeError").innerHTML = "";
    abrirModal();
}

async function inicializarPagina() {
    await cargarProfesores();
    cargarTabla();
}

inicializarPagina();