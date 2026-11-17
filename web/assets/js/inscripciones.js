let inscripcionesActuales = [];
let inscripcionEvaluando = null;

const $ = id => document.getElementById(id);

const etiquetaEstado = {
  PENDIENTE: 'Pendiente',
  ADMITIDA: 'Admitida',
  APROBADA: 'Aprobada',
  REPROBADA: 'Reprobada',
  RETIRADA: 'Retirada',
  RECHAZADA: 'Rechazada'
};

const claseBadge = {
  PENDIENTE: 'suspendido',
  ADMITIDA: 'activo',
  APROBADA: 'activo',
  REPROBADA: 'suspendido',
  RETIRADA: 'suspendido',
  RECHAZADA: 'suspendido'
};

function escapar(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function fechaCorta(fecha) {
  return fecha ? String(fecha).slice(0, 10) : 'N/A';
}

function abrirModal() {
  $('mensajeError').textContent = '';
  $('modalInscripcion').classList.add('activo');
}

function cerrarModal() {
  $('modalInscripcion').classList.remove('activo');
  $('formInscripcion').reset();
  $('mensajeError').textContent = '';
}

function abrirModalEvaluar(inscripcion) {
  inscripcionEvaluando = inscripcion;
  const curso = inscripcion.cursoPeriodo ?? {};
  $('resumenEvaluar').textContent = `${inscripcion.estudiante?.nombre ?? 'Estudiante'} — ${curso.nombre ?? 'Curso'} (${curso.materia?.nombre ?? 'Materia'})`;
  $('calificacionInscripcion').value = inscripcion.calificacion ?? '';
  $('mensajeErrorEvaluar').textContent = '';
  $('modalEvaluarInscripcion').classList.add('activo');
}

function cerrarModalEvaluar() {
  $('modalEvaluarInscripcion').classList.remove('activo');
  $('formEvaluarInscripcion').reset();
  $('mensajeErrorEvaluar').textContent = '';
  inscripcionEvaluando = null;
}

$('btnRegistrar').addEventListener('click', abrirModal);
$('btnCerrarModal').addEventListener('click', cerrarModal);
$('btnCancelar').addEventListener('click', cerrarModal);
$('btnCerrarModalEvaluar').addEventListener('click', cerrarModalEvaluar);
$('btnCancelarEvaluar').addEventListener('click', cerrarModalEvaluar);

async function cargarCatalogos() {
  const [cursos, estudiantes, materias, periodos] = await Promise.all([
    peticionApi('/api/cursos-periodos'),
    peticionApi('/api/estudiantes'),
    peticionApi('/api/materias'),
    peticionApi('/api/periodos')
  ]);

  $('cursoInscripcion').innerHTML = '<option value="">Seleccione un curso…</option>' +
    cursos.map(c => `<option value="${c.id}">${escapar(c.nombre)} — ${escapar(c.materia?.nombre ?? 'Materia')} / ${escapar(c.periodo?.nombre ?? 'Periodo')}</option>`).join('');
  $('estudianteInscripcion').innerHTML = '<option value="">Seleccione un estudiante…</option>' +
    estudiantes.map(e => `<option value="${e.id}">${escapar(e.nombre)} (${escapar(e.codigo)})</option>`).join('');

  $('filtroEstudiante').innerHTML = '<option value="">Todos los estudiantes</option>' +
    estudiantes.map(e => `<option value="${e.id}">${escapar(e.nombre)} (${escapar(e.codigo)})</option>`).join('');
  $('filtroCurso').innerHTML = '<option value="">Todos los cursos</option>' +
    cursos.map(c => `<option value="${c.id}">${escapar(c.nombre)} — ${escapar(c.materia?.nombre ?? 'Materia')}</option>`).join('');
  $('filtroMateria').innerHTML = '<option value="">Todas las materias</option>' +
    materias.map(m => `<option value="${m.id}">${escapar(m.nombre)}</option>`).join('');
  $('filtroPeriodo').innerHTML = '<option value="">Todos los periodos</option>' +
    periodos.map(p => `<option value="${p.id}">${escapar(p.nombre)}</option>`).join('');
}

// La consulta se arma con la misma precedencia que aplica la ruta: estudiante,
// curso, materia+periodo, materia, periodo. Sin filtros devuelve todo ('').
function consultaActual() {
  const estudiante = $('filtroEstudiante').value;
  const curso = $('filtroCurso').value;
  const materia = $('filtroMateria').value;
  const periodo = $('filtroPeriodo').value;

  if (estudiante) return 'estudiante=' + encodeURIComponent(estudiante);
  if (curso) return 'curso=' + encodeURIComponent(curso);
  if (materia && periodo) return `materia=${encodeURIComponent(materia)}&periodo=${encodeURIComponent(periodo)}`;
  if (materia) return 'materia=' + encodeURIComponent(materia);
  if (periodo) return 'periodo=' + encodeURIComponent(periodo);
  return '';
}

function botonesAccion(inscripcion, indice) {
  const curso = inscripcion.cursoPeriodo?.id ?? '';
  const estudiante = inscripcion.estudiante?.id ?? '';
  const estado = inscripcion.estado;
  const botones = [];

  if (estado === 'PENDIENTE') {
    botones.push(`<button type="button" class="btn-tabla btn-primario" onclick="cambiarEstado(event,'${curso}','${estudiante}','admitir')">Admitir</button>`);
    botones.push(`<button type="button" class="btn-tabla btn-suspender" onclick="cambiarEstado(event,'${curso}','${estudiante}','rechazar')">Rechazar</button>`);
  }
  if (estado === 'ADMITIDA') {
    botones.push(`<button type="button" class="btn-tabla" onclick="evaluarPorIndice(${indice})">Evaluar</button>`);
    botones.push(`<button type="button" class="btn-tabla btn-suspender" onclick="cambiarEstado(event,'${curso}','${estudiante}','retirar')">Retirar</button>`);
  }
  if (estado === 'APROBADA') {
    botones.push(`<button type="button" class="btn-tabla btn-suspender" onclick="cambiarEstado(event,'${curso}','${estudiante}','retirar')">Retirar</button>`);
  }

  return botones.length ? `<div style="display:flex;gap:6px;flex-wrap:wrap">${botones.join('')}</div>` : '—';
}

async function cargarTabla() {
  const tbody = $('cuerpoTablaInscripciones');
  const consulta = consultaActual();

  tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px">Cargando inscripciones…</td></tr>';
  try {
    const inscripciones = await peticionApi(consulta ? '/api/inscripciones?' + consulta : '/api/inscripciones');
    inscripcionesActuales = inscripciones;

    if (!inscripciones.length) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px">No hay inscripciones para mostrar.</td></tr>';
      return;
    }

    tbody.innerHTML = inscripciones.map((inscripcion, indice) => {
      const estudiante = inscripcion.estudiante ?? {};
      const curso = inscripcion.cursoPeriodo ?? {};
      const materia = curso.materia ?? {};
      const periodo = curso.periodo ?? {};
      const estado = inscripcion.estado ?? 'PENDIENTE';

      return `<tr>
        <td>${escapar(estudiante.nombre)}<small style="display:block">${escapar(estudiante.codigo)}</small></td>
        <td>${escapar(curso.nombre)}</td>
        <td>${escapar(materia.nombre)}</td>
        <td>${escapar(periodo.nombre)}</td>
        <td><span class="badge ${claseBadge[estado] ?? 'suspendido'}">${escapar(etiquetaEstado[estado] ?? estado)}</span></td>
        <td>${inscripcion.calificacion == null ? 'Sin evaluar' : escapar(inscripcion.calificacion)}</td>
        <td>${fechaCorta(inscripcion.fechaInscripcion)}</td>
        <td>${botonesAccion(inscripcion, indice)}</td>
      </tr>`;
    }).join('');
  } catch (error) {
    console.error(error);
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:#9b1c1c;padding:20px">${escapar(error.message || 'No se pudo cargar la información.')}</td></tr>`;
  }
}

window.evaluarPorIndice = indice => {
  const inscripcion = inscripcionesActuales[indice];
  if (inscripcion) abrirModalEvaluar(inscripcion);
};

window.cambiarEstado = async (event, idCurso, idEstudiante, accion) => {
  event?.stopPropagation();
  const textos = {
    admitir: 'admitir esta inscripción',
    retirar: 'retirar esta inscripción',
    rechazar: 'rechazar esta inscripción'
  };
  if (!confirm(`¿Desea ${textos[accion] ?? accion}?`)) return;
  try {
    await peticionApi(`/api/inscripciones/${idCurso}/${idEstudiante}/${accion}`, 'POST');
    await cargarTabla();
  } catch (error) {
    alert(error.message || 'No se pudo actualizar el estado.');
  }
};

$('formInscripcion').addEventListener('submit', async event => {
  event.preventDefault();
  const idCursoPeriodo = $('cursoInscripcion').value;
  const idEstudiante = $('estudianteInscripcion').value;

  if (!idCursoPeriodo || !idEstudiante) {
    $('mensajeError').textContent = 'Seleccione el curso y el estudiante.';
    return;
  }

  try {
    await peticionApi('/api/inscripciones', 'POST', { idCursoPeriodo, idEstudiante });
    cerrarModal();
    await cargarTabla();
  } catch (error) {
    $('mensajeError').textContent = error.message || 'No se pudo comunicar con el servidor.';
  }
});

$('formEvaluarInscripcion').addEventListener('submit', async event => {
  event.preventDefault();
  if (!inscripcionEvaluando) return;

  const calificacion = Number($('calificacionInscripcion').value);
  if (!Number.isInteger(calificacion) || calificacion < 0 || calificacion > 500) {
    $('mensajeErrorEvaluar').textContent = 'La calificación debe ser un entero entre 0 y 500.';
    return;
  }

  const idCursoPeriodo = inscripcionEvaluando.cursoPeriodo?.id;
  const idEstudiante = inscripcionEvaluando.estudiante?.id;
  if (!idCursoPeriodo || !idEstudiante) {
    $('mensajeErrorEvaluar').textContent = 'La inscripción no tiene la clave completa.';
    return;
  }

  try {
    await peticionApi(`/api/inscripciones/${idCursoPeriodo}/${idEstudiante}/evaluar`, 'PUT', { calificacion });
    cerrarModalEvaluar();
    await cargarTabla();
  } catch (error) {
    $('mensajeErrorEvaluar').textContent = error.message || 'No se pudo registrar la evaluación.';
  }
});

$('btnFiltrar').addEventListener('click', cargarTabla);
$('btnLimpiar').addEventListener('click', () => {
  $('filtroEstudiante').value = '';
  $('filtroCurso').value = '';
  $('filtroMateria').value = '';
  $('filtroPeriodo').value = '';
  cargarTabla();
});

(async function inicializar() {
  try {
    await cargarCatalogos();
    await cargarTabla();
  } catch (error) {
    console.error(error);
    $('cuerpoTablaInscripciones').innerHTML = '<tr><td colspan="8" style="text-align:center;color:#9b1c1c;padding:20px">Esperando conexión con el servidor…</td></tr>';
  }
})();
