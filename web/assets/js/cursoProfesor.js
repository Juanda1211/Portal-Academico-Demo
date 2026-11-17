let asignacionEditando = null;
let profesoresDisponibles = [];
let todosLosProfesores = [];
let cursosDisponibles = [];
let asignacionesActuales = [];

const $ = id => document.getElementById(id);
const etiquetaTipo = {
  PROFESOR_PRINCIPAL: 'Profesor principal',
  PROFESOR_ACOMPANANTE: 'Profesor acompañante',
  AUDITOR: 'Auditor'
};

function abrirModal(asignacion = null) {
  asignacionEditando = asignacion;
  $('modalTitulo').textContent = asignacion ? 'Modificar tipo de asignación' : 'Asignar profesor a curso';
  $('profesorCursoProfesor').disabled = Boolean(asignacion);
  $('cursoCursoProfesor').disabled = Boolean(asignacion);
  $('profesorCursoProfesor').value = asignacion?.profesor?.id ?? '';
  $('cursoCursoProfesor').value = asignacion?.cursoPeriodo?.id ?? '';
  $('tipoCursoProfesor').value = asignacion?.tipoAsignacion ?? 'PROFESOR_PRINCIPAL';
  $('mensajeError').textContent = '';
  $('modalCursoProfesor').classList.add('activo');
}
function cerrarModal() {
  $('modalCursoProfesor').classList.remove('activo');
  $('formCursoProfesor').reset();
  $('profesorCursoProfesor').disabled = false;
  $('cursoCursoProfesor').disabled = false;
  asignacionEditando = null;
  $('mensajeError').textContent = '';
}
$('btnRegistrar').addEventListener('click', () => abrirModal());
$('btnCerrarModal').addEventListener('click', cerrarModal);
$('btnCancelar').addEventListener('click', cerrarModal);

async function cargarCatalogos() {
  const [profesores, cursos, materias, periodos] = await Promise.all([
    peticionApi('/api/profesores'), peticionApi('/api/cursos-periodos'),
    peticionApi('/api/materias'), peticionApi('/api/periodos')
  ]);
  todosLosProfesores = profesores;
  profesoresDisponibles = profesores.filter(p => !p.estado || p.estado === 'ACTIVO');
  cursosDisponibles = cursos;
  $('profesorCursoProfesor').innerHTML = '<option value="">Seleccione un profesor…</option>' +
    profesoresDisponibles.map(p => `<option value="${p.id}">${escapar(p.nombre)} (${p.id})</option>`).join('');
  $('filtroProfesor').innerHTML = '<option value="">Todos los profesores</option>' +
    profesores.map(p => `<option value="${p.id}">${escapar(p.nombre)}</option>`).join('');
  $('cursoCursoProfesor').innerHTML = '<option value="">Seleccione un curso…</option>' +
    cursos.map(c => `<option value="${c.id}">${escapar(c.nombre)} — ${escapar(c.materia?.nombre ?? 'Materia')} / ${escapar(c.periodo?.nombre ?? 'Periodo')}</option>`).join('');
  $('filtroMateria').innerHTML = '<option value="">Todas las materias</option>' +
    materias.map(m => `<option value="${m.id}">${escapar(m.nombre)}</option>`).join('');
  $('filtroPeriodo').innerHTML = '<option value="">Todos los periodos</option>' +
    periodos.map(p => `<option value="${p.id}">${escapar(p.nombre)}</option>`).join('');
}
function escapar(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
function fechaCorta(fecha) {
  return fecha ? String(fecha).slice(0, 10) : 'N/A';
}
async function cargarTabla() {
  const tbody = $('cuerpoTablaCursoProfesor');
  tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px">Cargando asignaciones…</td></tr>';
  const profesor = $('filtroProfesor').value;
  const materia = $('filtroMateria').value;
  const periodo = $('filtroPeriodo').value;
  const params = new URLSearchParams();
  if (profesor) params.set('profesor', profesor);
  if (materia) params.set('materia', materia);
  if (periodo) params.set('periodo', periodo);
  try {
    let asignaciones;
    if (params.size) {
      asignaciones = await peticionApi('/api/cursos-profesores?' + params.toString());
    } else {
      // CursoProfesor no expone listarTodos(): se consulta usando el filtro existente por profesor.
      const resultados = await Promise.all(todosLosProfesores.map(p =>
        peticionApi('/api/cursos-profesores?profesor=' + encodeURIComponent(p.id))
      ));
      const unicas = new Map();
      resultados.flat().forEach(a => unicas.set(`${a.cursoPeriodo?.id ?? a.cursoPeriodo?.idCursoPeriodo}-${a.profesor?.id ?? a.profesor?.idProfesor}`, a));
      asignaciones = [...unicas.values()].sort((a, b) => String(b.fechaAsignacion ?? '').localeCompare(String(a.fechaAsignacion ?? '')));
    }
    asignacionesActuales = asignaciones;
    if (!asignaciones.length) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px">No hay asignaciones para los filtros seleccionados.</td></tr>';
      return;
    }
    tbody.innerHTML = asignaciones.map((a, indice) => {
      const profesor = a.profesor ?? {};
      const curso = a.cursoPeriodo ?? {};
      const materia = curso.materia ?? {};
      const periodo = curso.periodo ?? {};
      const idProfesor = profesor.id ?? profesor.idProfesor;
      const idCurso = curso.id ?? curso.idCursoPeriodo;
      const retirado = a.estado === 'RETIRADO';
      const botonEstado = retirado
        ? `<button type="button" class="btn-tabla btn-primario" onclick="cambiarEstado(event,'${idCurso}','${idProfesor}','reactivar')">Reactivar</button>`
        : `<button type="button" class="btn-tabla btn-suspender" onclick="cambiarEstado(event,'${idCurso}','${idProfesor}','retirar')">Retirar</button>`;
      return `<tr>
        <td>${escapar(profesor.nombre)}<small style="display:block">ID: ${escapar(idProfesor)}</small></td>
        <td>${escapar(curso.nombre)}<small style="display:block">${escapar(idCurso)}</small></td>
        <td>${escapar(materia.nombre)}</td><td>${escapar(periodo.nombre)}</td>
        <td>${escapar(etiquetaTipo[a.tipoAsignacion] ?? a.tipoAsignacion)}</td>
        <td><span class="badge ${retirado ? 'suspendido' : 'activo'}">${retirado ? 'Retirado' : 'Asignado'}</span></td>
        <td>${fechaCorta(a.fechaAsignacion)}</td>
        <td><div style="display:flex;gap:6px;flex-wrap:wrap"><button type="button" class="btn-tabla" onclick="editarAsignacionPorIndice(${indice})">Editar</button>${botonEstado}</div></td>
      </tr>`;
    }).join('');
  } catch (error) {
    console.error(error);
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:#9b1c1c;padding:20px">${escapar(error.message || 'No se pudo cargar la información.')}</td></tr>`;
  }
}
window.editarAsignacionPorIndice = indice => {
  const asignacion = asignacionesActuales[indice];
  if (asignacion) abrirModal(asignacion);
};
window.cambiarEstado = async (event, idCurso, idProfesor, accion) => {
  event?.stopPropagation();
  const texto = accion === 'retirar' ? 'retirar esta asignación' : 'reactivar esta asignación';
  if (!confirm(`¿Desea ${texto}?`)) return;
  try {
    await peticionApi(`/api/cursos-profesores/${idCurso}/${idProfesor}/${accion}`, 'POST');
    await cargarTabla();
  } catch (error) { alert(error.message || 'No se pudo actualizar el estado.'); }
};
$('formCursoProfesor').addEventListener('submit', async event => {
  event.preventDefault();
  const idProfesor = $('profesorCursoProfesor').value;
  const idCursoPeriodo = $('cursoCursoProfesor').value;
  const tipoAsignacion = $('tipoCursoProfesor').value;
  if (!idProfesor || !idCursoPeriodo || !tipoAsignacion) {
    $('mensajeError').textContent = 'Seleccione profesor, curso y tipo de asignación.';
    return;
  }
  try {
    if (asignacionEditando) {
      await peticionApi(`/api/cursos-profesores/${idCursoPeriodo}/${idProfesor}`, 'PUT', { tipoAsignacion });
    } else {
      await peticionApi('/api/cursos-profesores', 'POST', {
        idCursoPeriodo, idProfesor: Number(idProfesor), tipoAsignacion
      });
    }
    cerrarModal();
    await cargarTabla();
  } catch (error) {
    $('mensajeError').textContent = error.message || 'No se pudo comunicar con el servidor.';
  }
});
$('btnFiltrar').addEventListener('click', cargarTabla);
$('btnLimpiar').addEventListener('click', () => {
  $('filtroProfesor').value = '';
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
    $('cuerpoTablaCursoProfesor').innerHTML = '<tr><td colspan="8" style="text-align:center;color:#9b1c1c;padding:20px">Esperando conexión con el servidor…</td></tr>';
  }
})();
