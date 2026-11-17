let idEstudianteEditando = null;
let originalCorreo = null;
let originalCelular = null;
let originalDireccion = null;
let estudianteSeleccionado = null;

// ==========================================
// CONTROL DEL PANEL LATERAL (MODAL)
// ==========================================
function abrirModal() {
    document.getElementById('modalEstudiante').classList.add('activo');
}

function cerrarModal() {
    document.getElementById('modalEstudiante').classList.remove('activo');
    
    // Limpiamos los campos
    document.getElementById("codigoEstudiante").value = "";
    document.getElementById("nombreEstudiante").value = "";
    document.getElementById("correoEstudiante").value = "";
    document.getElementById("celularEstudiante").value = "";
    document.getElementById("direccionEstudiante").value = "";
    
    // Desbloqueamos código y nombre por si venían bloqueados de una edición anterior
    document.getElementById("codigoEstudiante").disabled = false;
    document.getElementById("nombreEstudiante").disabled = false;
    
    let divError = document.getElementById("mensajeError");
    if (divError) divError.innerText = "";
    
    idEstudianteEditando = null;
    estudianteSeleccionado = null;

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = true;
        btnActualizar.classList.remove("btn-primario");
        btnActualizar.classList.add("btn-secundario");
    }
    
    let todasLasFilas = document.querySelectorAll("#cuerpoTablaEstudiante tr");
    todasLasFilas.forEach(f => f.classList.remove("fila-seleccionada"));
}

document.getElementById('btnRegistrar').addEventListener('click', () => {
    document.getElementById('modalTitulo').innerText = "Registrar Nuevo Estudiante";
    abrirModal();
});

document.getElementById('btnActualizar').addEventListener('click', () => {
    if (estudianteSeleccionado !== null) {
        prepararEdicion(
            estudianteSeleccionado.id, 
            estudianteSeleccionado.codigo, 
            estudianteSeleccionado.nombre, 
            estudianteSeleccionado.correo, 
            estudianteSeleccionado.celular, 
            estudianteSeleccionado.direccion
        );
    }
});

document.getElementById('filtroEstado').addEventListener('change', () => {
    cargarTabla();
});

// ==========================================
// LÓGICA DE SELECCIÓN DE FILA Y DETALLES
// ==========================================
function seleccionarFila(filaHtml, id, codigo, nombre, correo, celular, direccion) {
    let todasLasFilas = document.querySelectorAll("#cuerpoTablaEstudiante tr");
    todasLasFilas.forEach(f => f.classList.remove("fila-seleccionada"));

    filaHtml.classList.add("fila-seleccionada");
    estudianteSeleccionado = { id, codigo, nombre, correo, celular, direccion };

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = false;
        btnActualizar.classList.remove("btn-secundario");
        btnActualizar.classList.add("btn-primario");
    }

    // NUEVO: Abrimos automáticamente el modal de detalles al seleccionar la fila
    mostrarDetalleEstudiante(estudianteSeleccionado);
}

// ==========================================
// CONTROL DEL MODAL CENTRADO (DETALLES)
// ==========================================
function mostrarDetalleEstudiante(e) {
    let htmlDetalle = `
        <p><strong>UUID:</strong> <span style="font-size: 0.85rem;">${e.id}</span></p>
        <p><strong>Código:</strong> ${e.codigo}</p>
        <p><strong>Nombre:</strong> ${e.nombre}</p>
        <p><strong>Correo:</strong> ${e.correo}</p>
        <p><strong>Celular:</strong> ${e.celular}</p>
        <p><strong>Dirección:</strong> ${e.direccion}</p>
    `;

    document.getElementById('detalleContenido').innerHTML = htmlDetalle;
    document.getElementById('modalDetalleEstudiante').classList.add('activo');
}

function cerrarModalDetalle() {
    // Al cerrar con la 'X', solo ocultamos el modal. 
    // La fila sigue seleccionada en la tabla por si desean "Actualizar".
    document.getElementById('modalDetalleEstudiante').classList.remove('activo');
}

// ==========================================
// FUNCIONES DE LA API
// ==========================================
async function cargarTabla() {
    let tbody = document.getElementById("cuerpoTablaEstudiante");
    let filtro = document.getElementById("filtroEstado").value;
    let filahtml = "";
    
    let ruta = '/api/estudiantes';
    if (filtro === 'activos') ruta = '/api/estudiantes/activos';
    if (filtro === 'inactivos') ruta = '/api/estudiantes/inactivos';

    try {
        let estudiantes = await peticionApi(ruta);

        estudiantes.forEach(function(e) {
            let claseEstado = e.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
            
            // OJO: Como el ID es UUID (texto), usamos comillas simples '${e.id}' en los parámetros
            let botonAccion = e.estado === "ACTIVO" 
                ? `<button type="button" class="btn-tabla btn-suspender" onclick="cambiarEstado(event, '${e.id}', 'inactivar')">Inactivar</button>` 
                : `<button type="button" class="btn-tabla btn-reactivar" onclick="cambiarEstado(event, '${e.id}', 'activar')">Activar</button>`;
            
            filahtml += ` 
            <tr onclick="seleccionarFila(this, '${e.id}', '${e.codigo}', '${e.nombre}', '${e.correo}', '${e.celular}', '${e.direccion}')">
                <td class="col-codigo">${e.codigo}</td>
                <td class="col-nombre">${e.nombre}</td>
                <td>${e.correo}</td>
                <td class="col-numero">${e.celular}</td>
                <td>${e.direccion}</td>
                <td><span class="${claseEstado}">${e.estado}</span></td>
                <td><div style="display:flex; gap:8px;">${botonAccion}</div></td>
            </tr>
            `;
        });
        tbody.innerHTML = filahtml;
    } catch(error) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:red; padding:20px;">Error: No se pudo conectar con el servidor</td></tr>`;
    }
}

async function registrarEstudiante() {
    let codigo = document.getElementById("codigoEstudiante").value;
    let nombre = document.getElementById("nombreEstudiante").value;
    let correo = document.getElementById("correoEstudiante").value;
    let celular = document.getElementById("celularEstudiante").value;
    let direccion = document.getElementById("direccionEstudiante").value;
    
    if (!codigo || !nombre || !correo || !celular || !direccion) {
        alert("Todos los campos son obligatorios.");
        return;
    }

    try {
        if (idEstudianteEditando === null) {
            // MODO CREAR (POST)
            const nuevoEstudiante = { codigo, nombre, correo, celular, direccion };
            await peticionApi('/api/estudiantes', 'POST', nuevoEstudiante);
        } else {
            // MODO ACTUALIZAR (PUT): se envía solo lo que cambió; el estado va por activar/inactivar
            const cambios = {};
            if (correo !== originalCorreo) cambios.correo = correo;
            if (celular !== originalCelular) cambios.celular = celular;
            if (direccion !== originalDireccion) cambios.direccion = direccion;

            if (Object.keys(cambios).length > 0) {
                await peticionApi(`/api/estudiantes/${idEstudianteEditando}`, 'PUT', cambios);
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
        await peticionApi(`/api/estudiantes/${id}/${accion}`, 'PUT');
        cargarTabla();
    } catch (error) {
        alert("Error: " + error.message); 
    }
}

function prepararEdicion(id, codigo, nombre, correo, celular, direccion) {
    idEstudianteEditando = id;
    originalCorreo = correo;
    originalCelular = celular;
    originalDireccion = direccion;
    
    document.getElementById("codigoEstudiante").value = codigo;
    document.getElementById("nombreEstudiante").value = nombre;
    document.getElementById("correoEstudiante").value = correo;
    document.getElementById("celularEstudiante").value = celular;
    document.getElementById("direccionEstudiante").value = direccion;
    
    // Bloqueamos los campos inmutables para que el usuario no pueda tocarlos
    document.getElementById("codigoEstudiante").disabled = true;
    document.getElementById("nombreEstudiante").disabled = true;
    
    document.getElementById("modalTitulo").innerText = "Editar Estudiante";
    let divError = document.getElementById("mensajeError");
    if (divError) divError.innerHTML = "";
    
    abrirModal();
}

// ==========================================
// LÓGICA DE LA BARRA DE BÚSQUEDA
// ==========================================
document.getElementById('btnBuscar').addEventListener('click', async () => {
    let idBuscado = document.getElementById('buscarEstudiante').value.trim();
    
    if (idBuscado === "") {
        alert("Ingrese un UUID para buscar.");
        return;
    }

    try {
        // Hacemos la petición GET directa al backend
        let estudiante = await peticionApi(`/api/estudiantes/${idBuscado}`);
        if (estudiante) {
            // Reutilizamos la función que creamos en el paso anterior
            mostrarDetalleEstudiante(estudiante);
        }
    } catch (error) {
        console.error("Error en la búsqueda:", error);
        alert("No se encontró ningún estudiante con el UUID: " + idBuscado);
    }
});

// Inicializamos
cargarTabla();