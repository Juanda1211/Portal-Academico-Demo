let asignacionEditando = null;
let asignacionesActuales = [];

const $ = id => document.getElementById(id);

function escapar(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function abrirModal(asignacion = null) {
  asignacionEditando = asignacion;
  $('modalTitulo').textContent = asignacion ? 'Actualizar ubicación de la materia' : 'Asignar materia a un plan';
  $('planPlanMateria').disabled = Boolean(asignacion);
  $('materiaPlanMateria').disabled = Boolean(asignacion);
  $('planPlanMateria').value = asignacion?.planEstudio?.id ?? '';
  $('materiaPlanMateria').value = asignacion?.materia?.id ?? '';
  $('semestrePlanMateria').value = asignacion?.semestre ?? '';
  $('ordenPlanMateria').value = asignacion?.orden ?? '';
  $('mensajeError').textContent = '';
  $('modalPlanMateria').classList.add('activo');
}

function cerrarModal() {
  $('modalPlanMateria').classList.remove('activo');
  $('formPlanMateria').reset();
  $('planPlanMateria').disabled = false;
  $('materiaPlanMateria').disabled = false;
  asignacionEditando = null;
  $('mensajeError').textContent = '';
}

$('btnRegistrar').addEventListener('click', () => abrirModal());
$('btnCerrarModal').addEventListener('click', cerrarModal);
$('btnCancelar').addEventListener('click', cerrarModal);

async function cargarCatalogos() {
  const [planes, materias] = await Promise.all([
    peticionApi('/api/planes-estudio'),
    peticionApi('/api/materias')
  ]);
  const opcionesPlanes = planes.map(p =>
    `<option value="${p.id}">${escapar(p.version)} — ${escapar(p.programa?.nombre ?? 'Programa')}</option>`
  ).join('');
  $('planPlanMateria').innerHTML = '<option value="">Seleccione un plan…</option>' + opcionesPlanes;
  $('filtroPlan').innerHTML = '<option value="">Todos los planes</option>' + opcionesPlanes;

  const opcionesMaterias = materias.map(m => `<option value="${m.id}">${escapar(m.nombre)}</option>`).join('');
  $('materiaPlanMateria').innerHTML = '<option value="">Seleccione una materia…</option>' + opcionesMaterias;
  $('filtroMateria').innerHTML = '<option value="">Todas las materias</option>' + opcionesMaterias;
}

// El backend exige plan o materia: no expone un listado sin filtro.
function consultaActual() {
  const plan = $('filtroPlan').value;
  const materia = $('filtroMateria').value;
  if (plan) return 'plan=' + encodeURIComponent(plan);
  if (materia) return 'materia=' + encodeURIComponent(materia);
  return null;
}

async function cargarTabla() {
  const tbody = $('cuerpoTablaPlanMateria');
  const consulta = consultaActual();

  if (!consulta) {
    asignacionesActuales = [];
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:20px">Seleccione un plan o una materia para consultar las asignaciones.</td></tr>';
    return;
  }

  tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:20px">Cargando asignaciones…</td></tr>';
  try {
    const asignaciones = await peticionApi('/api/plan-materias?' + consulta);
    asignacionesActuales = asignaciones;

    if (!asignaciones.length) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:20px">No hay asignaciones para los filtros seleccionados.</td></tr>';
      return;
    }

    tbody.innerHTML = asignaciones.map((asignacion, indice) => {
      const plan = asignacion.planEstudio ?? {};
      const programa = plan.programa ?? {};
      const materia = asignacion.materia ?? {};
      return `<tr>
        <td>${escapar(plan.version)}<small style="display:block">ID: ${escapar(plan.id)}</small></td>
        <td>${escapar(programa.nombre)}</td>
        <td>${escapar(materia.nombre)}<small style="display:block">${escapar(materia.creditos)} créditos</small></td>
        <td>${escapar(asignacion.semestre)}</td>
        <td>${escapar(asignacion.orden)}</td>
        <td><div style="display:flex;gap:6px;flex-wrap:wrap">
          <button type="button" class="btn-tabla" onclick="editarPorIndice(${indice})">Editar</button>
          <button type="button" class="btn-tabla btn-suspender" onclick="quitarPorIndice(event,${indice})">Quitar</button>
        </div></td>
      </tr>`;
    }).join('');
  } catch (error) {
    console.error(error);
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:#9b1c1c;padding:20px">${escapar(error.message || 'No se pudo cargar la información.')}</td></tr>`;
  }
}

window.editarPorIndice = indice => {
  const asignacion = asignacionesActuales[indice];
  if (asignacion) abrirModal(asignacion);
};

window.quitarPorIndice = async (event, indice) => {
  event?.stopPropagation();
  const asignacion = asignacionesActuales[indice];
  if (!asignacion) return;
  const idPlan = asignacion.planEstudio?.id;
  const idMateria = asignacion.materia?.id;
  if (idPlan == null || idMateria == null) return;

  if (!confirm(`¿Desea quitar ${asignacion.materia?.nombre ?? 'la materia'} del plan?`)) return;
  try {
    await peticionApi(`/api/plan-materias/${idPlan}/${idMateria}`, 'DELETE');
    await cargarTabla();
  } catch (error) {
    alert(error.message || 'No se pudo quitar la materia del plan.');
  }
};

$('formPlanMateria').addEventListener('submit', async event => {
  event.preventDefault();

  const idPlanEstudio = Number($('planPlanMateria').value);
  const idMateria = Number($('materiaPlanMateria').value);
  const semestre = Number($('semestrePlanMateria').value);
  const orden = Number($('ordenPlanMateria').value);

  if (!idPlanEstudio || !idMateria) {
    $('mensajeError').textContent = 'Seleccione el plan y la materia.';
    return;
  }
  if (!Number.isInteger(semestre) || semestre < 1 || !Number.isInteger(orden) || orden < 1) {
    $('mensajeError').textContent = 'El semestre y el orden deben ser enteros mayores o iguales a uno.';
    return;
  }

  try {
    if (asignacionEditando) {
      // El backend exige al menos un campo: enviamos solo lo que cambió.
      const cuerpo = {};
      if (semestre !== asignacionEditando.semestre) cuerpo.semestre = semestre;
      if (orden !== asignacionEditando.orden) cuerpo.orden = orden;

      if (!Object.keys(cuerpo).length) {
        $('mensajeError').textContent = 'Modifique el semestre o el orden para actualizar.';
        return;
      }
      await peticionApi(`/api/plan-materias/${idPlanEstudio}/${idMateria}`, 'PUT', cuerpo);
    } else {
      await peticionApi('/api/plan-materias', 'POST', { idPlanEstudio, idMateria, semestre, orden });
    }
    cerrarModal();
    if (!consultaActual()) {
      $('filtroPlan').value = String(idPlanEstudio);
    }
    await cargarTabla();
  } catch (error) {
    $('mensajeError').textContent = error.message || 'No se pudo comunicar con el servidor.';
  }
});

$('btnFiltrar').addEventListener('click', cargarTabla);
$('btnLimpiar').addEventListener('click', () => {
  $('filtroPlan').value = '';
  $('filtroMateria').value = '';
  cargarTabla();
});

(async function inicializar() {
  try {
    await cargarCatalogos();
    await cargarTabla();
  } catch (error) {
    console.error(error);
    $('cuerpoTablaPlanMateria').innerHTML = '<tr><td colspan="6" style="text-align:center;color:#9b1c1c;padding:20px">Esperando conexión con el servidor…</td></tr>';
  }
})();
