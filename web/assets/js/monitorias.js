let monitoriasActuales = [];
let monitoriaEvaluando = null;

const $ = id => document.getElementById(id);

const etiquetaEstado = {
  PENDIENTE: 'Pendiente',
  APROBADO: 'Aprobado',
  REPROBADO: 'Reprobado'
};

const claseBadge = {
  PENDIENTE: 'suspendido',
  APROBADO: 'activo',
  REPROBADO: 'suspendido'
};

function escapar(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function fechaCorta(fecha) {
  return fecha ? String(fecha).slice(0, 10) : 'Sin evaluar';
}

function abrirModal() {
  $('mensajeError').textContent = '';
  $('modalMonitoria').classList.add('activo');
}

function cerrarModal() {
  $('modalMonitoria').classList.remove('activo');
  $('formMonitoria').reset();
  $('mensajeError').textContent = '';
}

function abrirModalEvaluar(monitoria) {
  monitoriaEvaluando = monitoria;
  const curso = monitoria.cursoPeriodo ?? {};
  $('resumenEvaluar').textContent = `${monitoria.estudiante?.nombre ?? 'Monitor'} — ${curso.nombre ?? 'Curso'} (${curso.materia?.nombre ?? 'Materia'})`;
  $('calificacionMonitoria').value = monitoria.calificacion ?? '';
  $('comentarioMonitoria').value = '';
  $('mensajeErrorEvaluar').textContent = '';
  $('modalEvaluarMonitoria').classList.add('activo');
}

function cerrarModalEvaluar() {
  $('modalEvaluarMonitoria').classList.remove('activo');
  $('formEvaluarMonitoria').reset();
  $('mensajeErrorEvaluar').textContent = '';
  monitoriaEvaluando = null;
}

$('btnRegistrar').addEventListener('click', abrirModal);
$('btnCerrarModal').addEventListener('click', cerrarModal);
$('btnCancelar').addEventListener('click', cerrarModal);
$('btnCerrarModalEvaluar').addEventListener('click', cerrarModalEvaluar);
$('btnCancelarEvaluar').addEventListener('click', cerrarModalEvaluar);

async function cargarCatalogos() {
  const [cursos, estudiantes, profesores] = await Promise.all([
    peticionApi('/api/cursos-periodos'),
    peticionApi('/api/estudiantes'),
    peticionApi('/api/profesores')
  ]);

  const opcionesCursos = cursos.map(c =>
    `<option value="${c.id}">${escapar(c.nombre)} — ${escapar(c.materia?.nombre ?? 'Materia')} / ${escapar(c.periodo?.nombre ?? 'Periodo')}</option>`
  ).join('');
  $('cursoMonitoria').innerHTML = '<option value="">Seleccione un curso…</option>' + opcionesCursos;
  $('filtroCurso').innerHTML = '<option value="">Todos los cursos</option>' + opcionesCursos;

  const opcionesEstudiantes = estudiantes.map(e =>
    `<option value="${e.id}">${escapar(e.nombre)} (${escapar(e.codigo)})</option>`
  ).join('');
  $('estudianteMonitoria').innerHTML = '<option value="">Seleccione un estudiante…</option>' + opcionesEstudiantes;
  $('filtroEstudiante').innerHTML = '<option value="">Todos los estudiantes</option>' + opcionesEstudiantes;

  const opcionesProfesores = profesores.map(p =>
    `<option value="${p.id}">${escapar(p.nombre)} (${escapar(p.id)})</option>`
  ).join('');
  $('profesorMonitoria').innerHTML = '<option value="">Seleccione un profesor…</option>' + opcionesProfesores;
  $('filtroProfesor').innerHTML = '<option value="">Todos los profesores</option>' + opcionesProfesores;
}

async function cargarTabla() {
  const tbody = $('cuerpoTablaMonitorias');
  const curso = $('filtroCurso').value;
  const estudiante = $('filtroEstudiante').value;
  const profesor = $('filtroProfesor').value;

  const params = new URLSearchParams();
  if (curso) params.set('curso', curso);
  else if (estudiante) params.set('estudiante', estudiante);
  else if (profesor) params.set('profesor', profesor);

  const ruta = params.size ? '/api/monitorias?' + params.toString() : '/api/monitorias';
  tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px">Cargando monitorías…</td></tr>';

  try {
    const monitorias = await peticionApi(ruta);
    monitoriasActuales = monitorias;

    if (!monitorias.length) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px">No hay monitorías para los filtros seleccionados.</td></tr>';
      return;
    }

    tbody.innerHTML = monitorias.map((monitoria, indice) => {
      const cursoPeriodo = monitoria.cursoPeriodo ?? {};
      const materia = cursoPeriodo.materia ?? {};
      const periodo = cursoPeriodo.periodo ?? {};
      const estudiante = monitoria.estudiante ?? {};
      const profesor = monitoria.profesor ?? {};
      const estado = monitoria.estado ?? 'PENDIENTE';
      const comentario = monitoria.comentario;

      return `<tr>
        <td>${escapar(cursoPeriodo.nombre)}<small style="display:block">${escapar(materia.nombre)} / ${escapar(periodo.nombre)}</small></td>
        <td>${escapar(estudiante.nombre)}<small style="display:block">${escapar(estudiante.codigo)}</small></td>
        <td>${escapar(profesor.nombre)}</td>
        <td><span class="badge ${claseBadge[estado] ?? 'suspendido'}">${escapar(etiquetaEstado[estado] ?? estado)}</span></td>
        <td>${monitoria.calificacion == null ? 'Sin evaluar' : escapar(monitoria.calificacion)}</td>
        <td><div style="max-width:340px;white-space:normal" title="${escapar(comentario)}">${comentario ? escapar(comentario) : '—'}</div></td>
        <td>${fechaCorta(monitoria.fechaEvaluacion)}</td>
        <td><div style="display:flex;gap:6px;flex-wrap:wrap">
          <button type="button" class="btn-tabla" onclick="evaluarPorIndice(${indice})">Evaluar</button>
          <button type="button" class="btn-tabla btn-suspender" onclick="eliminarPorIndice(event,${indice})">Eliminar</button>
        </div></td>
      </tr>`;
    }).join('');
  } catch (error) {
    console.error(error);
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:#9b1c1c;padding:20px">${escapar(error.message || 'No se pudo cargar la información.')}</td></tr>`;
  }
}

window.evaluarPorIndice = indice => {
  const monitoria = monitoriasActuales[indice];
  if (monitoria) abrirModalEvaluar(monitoria);
};

window.eliminarPorIndice = async (event, indice) => {
  event?.stopPropagation();
  const monitoria = monitoriasActuales[indice];
  if (!monitoria) return;

  const idCursoPeriodo = monitoria.cursoPeriodo?.id;
  const idEstudiante = monitoria.estudiante?.id;
  const idProfesor = monitoria.profesor?.id;
  if (!idCursoPeriodo || !idEstudiante || idProfesor == null) return;

  if (!confirm(`¿Desea eliminar la monitoría de ${monitoria.estudiante?.nombre ?? 'el monitor'}?`)) return;
  try {
    await peticionApi(`/api/monitorias/${idCursoPeriodo}/${idEstudiante}/${idProfesor}`, 'DELETE');
    await cargarTabla();
  } catch (error) {
    alert(error.message || 'No se pudo eliminar la monitoría.');
  }
};

$('formMonitoria').addEventListener('submit', async event => {
  event.preventDefault();
  const idCursoPeriodo = $('cursoMonitoria').value;
  const idEstudiante = $('estudianteMonitoria').value;
  const idProfesor = Number($('profesorMonitoria').value);

  if (!idCursoPeriodo || !idEstudiante || !idProfesor) {
    $('mensajeError').textContent = 'Seleccione el curso, el estudiante y el profesor.';
    return;
  }

  try {
    await peticionApi('/api/monitorias', 'POST', { idCursoPeriodo, idEstudiante, idProfesor });
    cerrarModal();
    await cargarTabla();
  } catch (error) {
    $('mensajeError').textContent = error.message || 'No se pudo comunicar con el servidor.';
  }
});

$('formEvaluarMonitoria').addEventListener('submit', async event => {
  event.preventDefault();
  if (!monitoriaEvaluando) return;

  const calificacion = Number($('calificacionMonitoria').value);
  const comentario = $('comentarioMonitoria').value.trim();

  if (!Number.isInteger(calificacion) || calificacion < 0 || calificacion > 5) {
    $('mensajeErrorEvaluar').textContent = 'La calificación debe ser un entero entre 0 y 5.';
    return;
  }
  if (!comentario) {
    $('mensajeErrorEvaluar').textContent = 'El comentario es obligatorio.';
    return;
  }

  const idCursoPeriodo = monitoriaEvaluando.cursoPeriodo?.id;
  const idEstudiante = monitoriaEvaluando.estudiante?.id;
  const idProfesor = monitoriaEvaluando.profesor?.id;
  if (!idCursoPeriodo || !idEstudiante || idProfesor == null) {
    $('mensajeErrorEvaluar').textContent = 'La monitoría no tiene la clave completa.';
    return;
  }

  try {
    await peticionApi(
      `/api/monitorias/${idCursoPeriodo}/${idEstudiante}/${idProfesor}`,
      'PUT',
      { calificacion, comentario }
    );
    cerrarModalEvaluar();
    await cargarTabla();
  } catch (error) {
    $('mensajeErrorEvaluar').textContent = error.message || 'No se pudo registrar la evaluación.';
  }
});

$('btnFiltrar').addEventListener('click', cargarTabla);
$('btnLimpiar').addEventListener('click', () => {
  $('filtroCurso').value = '';
  $('filtroEstudiante').value = '';
  $('filtroProfesor').value = '';
  cargarTabla();
});

(async function inicializar() {
  try {
    await cargarCatalogos();
    await cargarTabla();
  } catch (error) {
    console.error(error);
    $('cuerpoTablaMonitorias').innerHTML = '<tr><td colspan="8" style="text-align:center;color:#9b1c1c;padding:20px">Esperando conexión con el servidor…</td></tr>';
  }
})();
