let idMateriaEditando = null;
let originalNombre = null;
let originalCreditos = null;
let originalCupo = null;
let materiaSeleccionada = null; // Variable para guardar la fila tocada

// ==========================================
// CONTROL DEL PANEL LATERAL (MODAL)
// ==========================================
function abrirModal() {
    document.getElementById('modalMateria').classList.add('activo');
}

function cerrarModal() {
    document.getElementById('modalMateria').classList.remove('activo');
    
    // Limpiamos los campos del formulario al cerrar
    document.getElementById("nombreMateria").value = "";
    document.getElementById("creditoMateria").value = "";
    document.getElementById("cupoMateria").value = "";
    
    let divError = document.getElementById("mensajeError");
    if (divError) divError.innerText = "";
    
    idMateriaEditando = null;

    // Resetear la selección visual de la tabla y apagar el botón
    materiaSeleccionada = null;
    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = true;
        btnActualizar.classList.remove("btn-primario");
        btnActualizar.classList.add("btn-secundario");
    }
    
    let todasLasFilas = document.querySelectorAll("#cuerpoTablaMateria tr");
    todasLasFilas.forEach(f => f.classList.remove("fila-seleccionada"));
}

// Escuchar el clic del botón superior "+ Registrar Materia"
document.getElementById('btnRegistrar').addEventListener('click', () => {
    document.getElementById('modalTitulo').innerText = "Registrar Nueva Materia";
    abrirModal();
});

// Escuchar el clic del botón superior "Actualizar Selección"
document.getElementById('btnActualizar').addEventListener('click', () => {
    if (materiaSeleccionada !== null) {
        prepararEdicion(
            materiaSeleccionada.id, 
            materiaSeleccionada.nombre, 
            materiaSeleccionada.creditos, 
            materiaSeleccionada.cupoMaximo
        );
    }
});

// ==========================================
// LÓGICA DE SELECCIÓN DE FILA
// ==========================================
function seleccionarFila(filaHtml, id, nombre, creditos, cupoMaximo) {
    // 1. Limpiamos la selección anterior
    let todasLasFilas = document.querySelectorAll("#cuerpoTablaMateria tr");
    todasLasFilas.forEach(f => f.classList.remove("fila-seleccionada"));

    // 2. Pintamos la fila actual
    filaHtml.classList.add("fila-seleccionada");

    // 3. Guardamos los datos de la fila tocada
    materiaSeleccionada = { id, nombre, creditos, cupoMaximo };

    // 4. Encendemos el botón superior
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
    let tbody = document.getElementById("cuerpoTablaMateria");
    let filahtml = "";
    try {
        let materias = await peticionApi('/api/materias');

        materias.forEach(function(materia) {
            let claseEstado = materia.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
            let botonAccion = materia.estado === "ACTIVO" 
                ? `<button type="button" class="btn-tabla btn-suspender" onclick="cambiarEstado(event, ${materia.id}, 'suspender')">Suspender</button>` 
                : `<button type="button" class="btn-tabla btn-reactivar" onclick="cambiarEstado(event, ${materia.id}, 'reactivar')">Reactivar</button>`;
            
            filahtml += ` 
            <tr onclick="seleccionarFila(this, ${materia.id}, '${materia.nombre}', ${materia.creditos}, ${materia.cupoMaximo})">
                <td class="col-codigo">${materia.id}</td>
                <td class="col-nombre">${materia.nombre}</td>
                <td class="col-numero">${materia.creditos}</td>
                <td class="col-numero">${materia.cupoMaximo}</td>
                <td><span class="${claseEstado}">${materia.estado}</span></td>
                <td><div style="display:flex; gap:8px;">${botonAccion}</div></td>
            </tr>
            `;
        });
        tbody.innerHTML = filahtml;
    } catch(error) {
        console.error("Error al conectar con la API: ", error);
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:red; padding:20px;">Error: No se pudo conectar con el servidor</td></tr>`;
    }
}

function mostrarErrorMateria(mensaje, idCampo) {
    const divError = document.getElementById("mensajeError");
    if (divError) divError.innerText = mensaje;
    if (idCampo) {
        const campo = document.getElementById(idCampo);
        if (campo) campo.focus();
    }
}

async function registrarMateria() {
    const nombre = document.getElementById("nombreMateria").value.trim();
    if (!nombre) {
        mostrarErrorMateria("El nombre de la materia es obligatorio.", "nombreMateria");
        return;
    }

    const credito = Number(document.getElementById("creditoMateria").value);
    if (!Number.isInteger(credito) || credito < 1 || credito > 6) {
        mostrarErrorMateria("Los créditos deben ser un número entero entre 1 y 6.", "creditoMateria");
        return;
    }

    const cupoMaximo = Number(document.getElementById("cupoMateria").value);
    if (!Number.isInteger(cupoMaximo) || cupoMaximo < 1 || cupoMaximo > 50) {
        mostrarErrorMateria("El cupo máximo debe ser un número entero entre 1 y 50.", "cupoMateria");
        return;
    }

    try {
        if (idMateriaEditando === null){
            // MODO CREAR (POST) 
            await peticionApi('/api/materias', 'POST', { 
                    nombre: nombre, 
                    creditos: credito, 
                    cupoMaximo: cupoMaximo 
                });
        } else {
            // MODO ACTUALIZAR (PUT): se envía solo lo que cambió
            const cambios = {};
            if (nombre !== originalNombre) cambios.nuevoNombre = nombre;
            if (credito !== originalCreditos) cambios.nuevosCreditos = credito;
            if (cupoMaximo !== originalCupo) cambios.nuevoCupo = cupoMaximo;

            if (Object.keys(cambios).length > 0) {
                await peticionApi(`/api/materias/${idMateriaEditando}`, 'PUT', cambios);
            }
        }
        
        // Si no hay errores, cerramos y recargamos
        cerrarModal();
        cargarTabla();

    } catch (error) {
        // Atrapamos el error que nos lanza comun.js
        console.error("Error al guardar: ", error);
        let divError = document.getElementById("mensajeError");
        if(divError) divError.innerText = error.message || "No se pudo comunicar con el servidor.";
    }
}

async function cambiarEstado(event, id, accion) {
    if(event) event.stopPropagation();
    try {
        // MODO SUSPENDER/REACTIVAR (PUT sin cuerpo)
        await peticionApi(`/api/materias/${id}/${accion}`, 'PUT');
        cargarTabla();
    } catch (error) {
        alert("Error: " + error.message); 
    }
}

function prepararEdicion(id, nombre, credito, cupoMaximo) {
    idMateriaEditando = id;
    originalNombre = nombre;
    originalCreditos = credito;
    originalCupo = cupoMaximo;
    
    document.getElementById("nombreMateria").value = nombre;
    document.getElementById("creditoMateria").value = credito;
    document.getElementById("cupoMateria").value = cupoMaximo;

    document.getElementById("modalTitulo").innerText = "Editar Materia";
    let divError = document.getElementById("mensajeError");
    if (divError) divError.innerHTML = "";
    
    abrirModal();
}

// ==========================================
// CONTROL DEL MODAL CENTRADO (BÚSQUEDA)
// ==========================================
function cerrarModalDetalle() {
    document.getElementById('modalDetalleMateria').classList.remove('activo');
}

document.getElementById('btnBuscar').addEventListener('click', async () => {
    let idBuscado = document.getElementById('buscarMateria').value.trim();
    
    if (idBuscado === "") {
        alert("Ingrese un ID para buscar.");
        return;
    }

    if (isNaN(idBuscado)) {
        alert("Por favor, ingrese un número válido para el ID de la materia.");
        return;
    }

    try {
        let materia = await peticionApi(`/api/materias/${idBuscado}`);
        if (materia) {
            mostrarDetalleMateria(materia);
        }
    } catch (error) {
        // Agregamos este console.error para ver la causa real si algo falla
        console.error("Error en la búsqueda:", error);
        alert("No se encontró ninguna materia con el ID: " + idBuscado);
    }
});

function mostrarDetalleMateria(m) {
    let claseEstado = m.estado === "ACTIVO" ? "badge activo" : "badge suspendido";
    
    let htmlDetalle = `
        <p><strong>ID:</strong> ${m.id}</p>
        <p><strong>Materia:</strong> ${m.nombre}</p>
        <p><strong>Créditos:</strong> ${m.creditos}</p>
        <p><strong>Cupo Máximo:</strong> ${m.cupoMaximo}</p>
        <p><strong>Estado:</strong> <span class="${claseEstado}">${m.estado}</span></p>
    `;

    document.getElementById('detalleContenido').innerHTML = htmlDetalle;
    document.getElementById('modalDetalleMateria').classList.add('activo');
}

// Inicializamos la tabla
cargarTabla();