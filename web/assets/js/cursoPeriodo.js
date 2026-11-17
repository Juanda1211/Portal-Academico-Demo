let idCursoEditando = null;
let originalNombre = null;
let originalCupo = null;
let cursoSeleccionado = null;
// Materias cargadas desde la API, indexadas por id, para conocer su cupo máximo.
let materiasPorId = new Map();

function escaparHtml(texto) {
    return String(texto ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

function obtenerMateriaSeleccionada() {
    const select = document.getElementById("materiaCursoPeriodo");
    return select ? materiasPorId.get(select.value) || null : null;
}

function mostrarErrorCurso(mensaje, idCampo) {
    const divError = document.getElementById("mensajeError");
    if (divError) divError.innerText = mensaje;
    if (idCampo) {
        const campo = document.getElementById(idCampo);
        if (campo) campo.focus();
    }
}

// Refleja en el campo de cupo el máximo permitido por la materia elegida.
function actualizarLimiteCupo() {
    const materia = obtenerMateriaSeleccionada();
    const cupoInput = document.getElementById("cupoCursoPeriodo");
    const ayuda = document.getElementById("ayudaCupoCursoPeriodo");
    if (!cupoInput) return;

    if (materia && Number.isInteger(materia.cupoMaximo)) {
        cupoInput.max = String(materia.cupoMaximo);
        if (ayuda) {
            ayuda.dataset.estado = "normal";
            ayuda.textContent = `Máximo permitido para ${materia.nombre}: ${materia.cupoMaximo} cupos.`;
        }
    } else {
        cupoInput.removeAttribute("max");
        if (ayuda) {
            ayuda.dataset.estado = "normal";
            ayuda.textContent = "";
        }
    }
}

function avisarCupoExcedido() {
    const materia = obtenerMateriaSeleccionada();
    const ayuda = document.getElementById("ayudaCupoCursoPeriodo");
    if (!materia || !Number.isInteger(materia.cupoMaximo) || !ayuda) return;

    const cupo = Number(document.getElementById("cupoCursoPeriodo").value);
    const excedido = Number.isInteger(cupo) && cupo > materia.cupoMaximo;

    ayuda.dataset.estado = excedido ? "excedido" : "normal";
    ayuda.textContent = excedido
        ? `El cupo supera el máximo permitido para ${materia.nombre} (${materia.cupoMaximo}).`
        : `Máximo permitido para ${materia.nombre}: ${materia.cupoMaximo} cupos.`;
}

// ==========================================
// CONTROL DEL PANEL LATERAL (MODAL)
// ==========================================
function abrirModal() { document.getElementById('modalCursoPeriodo').classList.add('activo'); }

function cerrarModal() {
    document.getElementById('modalCursoPeriodo').classList.remove('activo');
    document.getElementById("nombreCursoPeriodo").value = "";
    document.getElementById("materiaCursoPeriodo").value = "";
    document.getElementById("periodoCursoPeriodo").value = "";
    document.getElementById("cupoCursoPeriodo").value = "";
    
    // Desbloqueamos los selectores por si vienen de una edición
    document.getElementById("materiaCursoPeriodo").disabled = false;
    document.getElementById("periodoCursoPeriodo").disabled = false;
    actualizarLimiteCupo();
    
    if (document.getElementById("mensajeError")) document.getElementById("mensajeError").innerText = "";
    idCursoEditando = null;
    cursoSeleccionado = null;

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = true;
        btnActualizar.classList.replace("btn-primario", "btn-secundario");
    }
    document.querySelectorAll("#cuerpoTablaCursoPeriodo tr").forEach(f => f.classList.remove("fila-seleccionada"));
}

document.getElementById('btnRegistrar').addEventListener('click', () => {
    document.getElementById('modalTitulo').innerText = "Registrar Nuevo Curso por Periodo";
    abrirModal();
});

document.getElementById('btnActualizar').addEventListener('click', () => {
    if (cursoSeleccionado !== null) {
        prepararEdicion(
            cursoSeleccionado.id, 
            cursoSeleccionado.nombre, 
            cursoSeleccionado.idMateria,
            cursoSeleccionado.idPeriodo,
            cursoSeleccionado.cupo
        );
    }
});

// Al cambiar la materia se actualiza el máximo de cupo y su texto de ayuda.
document.getElementById('materiaCursoPeriodo').addEventListener('change', actualizarLimiteCupo);
document.getElementById('cupoCursoPeriodo').addEventListener('input', avisarCupoExcedido);

// ==========================================
// LÓGICA DE SELECCIÓN DE FILA
// ==========================================
function seleccionarFila(filaHtml, id, nombre, idMateria, idPeriodo, cupo) {
    document.querySelectorAll("#cuerpoTablaCursoPeriodo tr").forEach(f => f.classList.remove("fila-seleccionada"));
    filaHtml.classList.add("fila-seleccionada");
    cursoSeleccionado = { id, nombre, idMateria, idPeriodo, cupo };

    let btnActualizar = document.getElementById("btnActualizar");
    if (btnActualizar) {
        btnActualizar.disabled = false;
        btnActualizar.classList.replace("btn-secundario", "btn-primario");
    }
}

// ==========================================
// FUNCIONES DE LA API
// ==========================================
async function cargarDesplegables() {
    try {
        // Pedimos ambas listas a Java al mismo tiempo para que cargue más rápido
        const [materias, periodos] = await Promise.all([
            peticionApi('/api/materias'),
            peticionApi('/api/periodos')
        ]);

        materiasPorId = new Map();
        let opcionesMateria = '<option value="">Seleccione una materia...</option>';
        materias.forEach(m => {
            const activa = m.estado === "ACTIVO";
            const maximo = Number.isInteger(m.cupoMaximo) ? ` (máx. ${m.cupoMaximo})` : "";
            const estado = activa ? "" : " (inactiva)";
            materiasPorId.set(String(m.id), m);
            // Las materias inactivas se muestran deshabilitadas para que un curso
            // existente que las use siga siendo editable.
            opcionesMateria += `<option value="${m.id}"${activa ? "" : " disabled"}>${escaparHtml(m.nombre)}${maximo}${estado}</option>`;
        });
        document.getElementById('materiaCursoPeriodo').innerHTML = opcionesMateria;

        let opcionesPeriodo = '<option value="">Seleccione un periodo...</option>';
        periodos.forEach(p => {
            const activo = p.estado === "ACTIVO";
            const estado = activo ? "" : " (inactivo)";
            opcionesPeriodo += `<option value="${p.id}"${activo ? "" : " disabled"}>${escaparHtml(p.nombre)}${estado}</option>`;
        });
        document.getElementById('periodoCursoPeriodo').innerHTML = opcionesPeriodo;

    } catch (error) {
        console.error("Error al cargar listas desplegables:", error);
    }
}

async function cargarTabla() {
    let tbody = document.getElementById("cuerpoTablaCursoPeriodo");
    let filahtml = "";
    
    try {
        let cursos = await peticionApi('/api/cursos-periodos');

        cursos.forEach(function(c) {
            let fechaCorta = c.fechaCreacion ? c.fechaCreacion.slice(0, 10) : "N/A";
            
            let botonAccion = `<button type="button" class="btn-tabla btn-suspender" onclick="eliminarCursoPeriodo(event, '${c.id}')">Eliminar</button>`;
            
            filahtml += ` 
            <tr onclick="seleccionarFila(this, '${c.id}', '${c.nombre}', ${c.materia.id}, ${c.periodo.id}, ${c.cupo})">
                <td class="col-codigo">${c.id}</td> <!-- Mostrando el ID completo como pediste -->
                <td class="col-nombre">${c.nombre}</td>
                <td>${c.materia.nombre}</td>
                <td>${c.periodo.nombre}</td>
                <td class="col-numero">${c.cupo}</td>
                <td>${fechaCorta}</td>
                <td><div style="display:flex; gap:8px;">${botonAccion}</div></td>
            </tr>`;
        });
        tbody.innerHTML = filahtml;
    } catch(error) {
        console.error("El error real es: ", error);
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:red; padding:20px;">Error: No se pudo conectar con el servidor</td></tr>`;
    }
}

async function registrarCursoPeriodo() {
    const nombre = document.getElementById("nombreCursoPeriodo").value.trim();
    const idMateria = document.getElementById("materiaCursoPeriodo").value;
    const idPeriodo = document.getElementById("periodoCursoPeriodo").value;
    const cupoTexto = document.getElementById("cupoCursoPeriodo").value;
    const divError = document.getElementById("mensajeError");

    if (divError) divError.innerText = "";

    if (!nombre || !idMateria || !idPeriodo || !cupoTexto) {
        mostrarErrorCurso("Todos los campos son obligatorios.");
        return;
    }

    const cupo = Number(cupoTexto);
    if (!Number.isInteger(cupo) || cupo < 1) {
        mostrarErrorCurso("El cupo debe ser un número entero mayor a 0.", "cupoCursoPeriodo");
        return;
    }

    // El cupo del curso nunca puede superar el cupo máximo de su materia.
    const materia = obtenerMateriaSeleccionada();
    if (materia && Number.isInteger(materia.cupoMaximo) && cupo > materia.cupoMaximo) {
        mostrarErrorCurso(
            `El cupo no puede superar el máximo de la materia "${materia.nombre}" (${materia.cupoMaximo}).`,
            "cupoCursoPeriodo"
        );
        return;
    }

    const idMateriaNumero = parseInt(idMateria, 10);
    const idPeriodoNumero = parseInt(idPeriodo, 10);

    try {
        if (idCursoEditando === null) {
            // MODO CREAR (POST)
            await peticionApi('/api/cursos-periodos', 'POST', {
                nombre,
                idMateria: idMateriaNumero,
                idPeriodoAcademico: idPeriodoNumero,
                cupo
            });
        } else {
            // MODO ACTUALIZAR (PUT): la API solo permite nombre y cupo, y solo lo que cambió
            const cambios = {};
            if (nombre !== originalNombre) cambios.nuevoNombre = nombre;
            if (cupo !== originalCupo) cambios.nuevoCupo = cupo;

            if (Object.keys(cambios).length > 0) {
                await peticionApi(`/api/cursos-periodos/${idCursoEditando}`, 'PUT', cambios);
            }
        }
        cerrarModal();
        cargarTabla();
    } catch (error) {
        if(document.getElementById("mensajeError")) document.getElementById("mensajeError").innerText = error.message || "No se pudo comunicar con el servidor.";
    }
}

async function eliminarCursoPeriodo(event, id) {
    if(event) event.stopPropagation();
    
    if(confirm("¿Está seguro de que desea eliminar permanentemente este curso?")) {
        try {
            await peticionApi(`/api/cursos-periodos/${id}`, 'DELETE');
            cargarTabla();
        } catch (error) { 
            alert("Error: " + error.message); 
        }
    }
}

function prepararEdicion(id, nombre, idMateria, idPeriodo, cupo) {
    idCursoEditando = id; 
    originalNombre = nombre; 
    originalCupo = cupo;
    
    document.getElementById("nombreCursoPeriodo").value = nombre;
    document.getElementById("materiaCursoPeriodo").value = idMateria;
    document.getElementById("periodoCursoPeriodo").value = idPeriodo;
    document.getElementById("cupoCursoPeriodo").value = cupo;
    
    // Bloqueamos los selectores porque la API no permite cambiar la materia o el periodo una vez creado
    document.getElementById("materiaCursoPeriodo").disabled = true;
    document.getElementById("periodoCursoPeriodo").disabled = true;
    actualizarLimiteCupo();
    
    document.getElementById("modalTitulo").innerText = "Editar Curso";
    if (document.getElementById("mensajeError")) document.getElementById("mensajeError").innerHTML = "";
    abrirModal();
}

async function inicializarPagina() {
    await cargarDesplegables();
    cargarTabla();
}

// ==========================================
// LÓGICA DE LA BARRA DE BÚSQUEDA
// ==========================================
document.getElementById('btnBuscar').addEventListener('click', async () => {
    let idBuscado = document.getElementById("buscarCursoPeriodo").value.trim();
    if (!idBuscado) {
        alert("Ingrese un UUID para buscar.");
        return;
    }

    try {
        let curso = await peticionApi(`/api/cursos-periodos/${idBuscado}`);
        if (curso) {
            mostrarDetalleCurso(curso);
        }
    } catch (error) {
        console.error("Error en búsqueda: ", error);
        alert("No se encontró ningún curso con el UUID: " + idBuscado);
    }
});

// ==========================================
// CONTROL DEL MODAL CENTRADO (DETALLES)
// ==========================================
function mostrarDetalleCurso(c) {
    let fechaCorta = c.fechaCreacion ? c.fechaCreacion.slice(0, 10) : "N/A";
    
    // Obtenemos los nombres de las relaciones
    let nombreMateria = c.materia ? c.materia.nombre : "N/A";
    let nombrePeriodo = c.periodo ? c.periodo.nombre : "N/A";
    let creditosMateria = c.materia ? c.materia.creditos : "N/A";

    // Si tu Materia o Periodo tienen estado, calculamos sus colores
    let claseMateria = (c.materia && c.materia.estado === "ACTIVO") ? "badge activo" : "badge suspendido";
    let clasePeriodo = (c.periodo && c.periodo.estado === "ACTIVO") ? "badge activo" : "badge suspendido";
    
    let htmlDetalle = `
        <p><strong>UUID:</strong> <span style="font-size: 0.85rem;">${c.id}</span></p>
        <p><strong>Grupo/Nombre:</strong> ${c.nombre}</p>
        <p><strong>Cupos:</strong> ${c.cupo}</p>
        <p><strong>Fecha de Creación:</strong> ${fechaCorta}</p>

        <div class="tarjeta-info" style="margin-top: 15px;">
            <h4 style="margin-bottom: 12px; color: #002f6c;">Datos de la Materia:</h4>
            <p><strong>Materia:</strong> ${nombreMateria}</p>
            <p><strong>Créditos:</strong> ${creditosMateria}</p>
            <p><strong>Estado:</strong> <span class="${claseMateria}">${c.materia ? c.materia.estado : 'N/A'}</span></p>
        </div>

        <div class="tarjeta-info" style="margin-top: 15px;">
            <h4 style="margin-bottom: 12px; color: #002f6c;">Datos del Periodo:</h4>
            <p><strong>Periodo Académico:</strong> ${nombrePeriodo}</p>
            <p><strong>Estado:</strong> <span class="${clasePeriodo}">${c.periodo ? c.periodo.estado : 'N/A'}</span></p>
        </div>
    `;

    document.getElementById('detalleContenido').innerHTML = htmlDetalle;
    document.getElementById('modalDetalleCurso').classList.add('activo');
}

function cerrarModalDetalle() {
    document.getElementById('modalDetalleCurso').classList.remove('activo');
}

inicializarPagina();