// Botón que disparó la última acción: sirve para mostrar el estado de carga.
let botonDisparador = null;

function marcarCargaBoton(boton, activo) {
    if (!boton || !boton.isConnected) return;

    if (activo) {
        boton.dataset.estabaDeshabilitado = String(boton.disabled);
        boton.disabled = true;
        boton.setAttribute('aria-busy', 'true');
        return;
    }

    boton.removeAttribute('aria-busy');
    boton.disabled = boton.dataset.estabaDeshabilitado === 'true';
    delete boton.dataset.estabaDeshabilitado;
}

// Extrae un mensaje legible de una respuesta de error, venga en JSON (API) o en
// texto plano (por ejemplo el "No encontrado" del servidor de archivos).
async function leerMensajeError(respuesta) {
    const texto = (await respuesta.text()).trim();

    if (texto) {
        try {
            const datos = JSON.parse(texto);
            const mensaje = datos && (datos.error || datos.mensaje || datos.message);
            if (mensaje) return String(mensaje);
        } catch (error) {
            // No es JSON: devolvemos el texto tal cual para no ocultar la causa real.
            return texto;
        }
    }

    return `Error ${respuesta.status} al comunicarse con el servidor.`;
}

async function peticionApi(ruta, metodo = 'GET', cuerpo = null) {
    const opciones = {
        method: metodo,
        headers: { 'Content-Type': 'application/json' }
    };

    // Si enviamos datos, los convertimos a JSON
    if (cuerpo !== null) {
        opciones.body = JSON.stringify(cuerpo);
    }

    const boton = botonDisparador;
    marcarCargaBoton(boton, true);

    try {
        const respuesta = await fetch(ruta, opciones);

        // Si Java nos lanza un error de validación (400 o 409) responde JSON
        if (!respuesta.ok) {
            // Esto expulsa el error hacia afuera para que la página lo atrape
            throw new Error(await leerMensajeError(respuesta));
        }

        // Leemos la respuesta. Si Java no devuelve nada, devolvemos un objeto vacío {}
        const textoRespuesta = await respuesta.text();
        return textoRespuesta ? JSON.parse(textoRespuesta) : {};
    } finally {
        marcarCargaBoton(boton, false);
        if (botonDisparador === boton) botonDisparador = null;
    }
}

// Recordamos el botón pulsado para poder marcar su estado de carga.
document.addEventListener('click', (evento) => {
    const boton = evento.target.closest('.btn, .boton, .btn-tabla');
    if (boton) botonDisparador = boton;
}, true);

// ==========================================
// LÓGICA GLOBAL PARA AUTO-ABRIR MODAL REGISTRO
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    // Si la URL dice ?accion=registrar
    if (urlParams.get('accion') === 'registrar') {
        // Le damos un respiro de medio segundo para que la tabla y botones terminen de cargar
        setTimeout(() => {
            let btnRegistrar = document.getElementById('btnRegistrar');
            if (btnRegistrar) {
                btnRegistrar.click(); // Simulamos que el usuario le dio clic al botón
            }
        }, 500);
    }
});

// ==========================================
// LÓGICA GLOBAL (Auto-registro y Menú Móvil)
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    
    // 1. Lógica para el Menú Móvil (Hamburguesa)
    const btnMenu = document.getElementById('btnMenuMovil');
    const menuPrincipal = document.getElementById('menuPrincipal');
    
    if (btnMenu && menuPrincipal) {
        btnMenu.addEventListener('click', () => {
            // El toggle quita o pone la clase dependiendo de si la tiene o no
            menuPrincipal.classList.toggle('menu-abierto');
        });
    }

    // 2. Lógica para auto-abrir el modal de registro desde el submenú
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('accion') === 'registrar') {
        setTimeout(() => {
            let btnRegistrar = document.getElementById('btnRegistrar');
            if (btnRegistrar) {
                btnRegistrar.click(); 
            }
        }, 500);
    }
});

// El menú de Facultades también permite abrir los programas de una facultad
// que ya exista en el backend (no depende de datos ficticios en el HTML).
document.addEventListener('DOMContentLoaded', async () => {
    const enlacesPrincipales = Array.from(document.querySelectorAll('#menuPrincipal > li > a'));
    const enlaceFacultad = enlacesPrincipales.find(enlace =>
        (enlace.getAttribute('href') || '').split('?')[0] === 'facultad.html'
    );
    const submenu = enlaceFacultad?.parentElement?.querySelector('.submenu');
    if (!submenu || submenu.dataset.facultadesCargadas === 'true') return;

    submenu.dataset.facultadesCargadas = 'true';
    const separador = document.createElement('li');
    separador.className = 'submenu-separador';
    const etiqueta = document.createElement('span');
    etiqueta.textContent = 'Programas por facultad';
    separador.appendChild(etiqueta);
    submenu.appendChild(separador);

    try {
        const facultades = await peticionApi('/api/facultades');
        if (!facultades.length) {
            const li = document.createElement('li');
            const aviso = document.createElement('span');
            aviso.className = 'submenu-aviso';
            aviso.textContent = 'No hay facultades registradas';
            li.appendChild(aviso);
            submenu.appendChild(li);
            return;
        }
        facultades.forEach(facultad => {
            const li = document.createElement('li');
            const enlace = document.createElement('a');
            enlace.href = `programa.html?facultad=${encodeURIComponent(facultad.id)}`;
            enlace.textContent = facultad.nombre;
            enlace.title = `Ver programas de ${facultad.nombre}`;
            li.appendChild(enlace);
            submenu.appendChild(li);
        });
    } catch (error) {
        const li = document.createElement('li');
        const aviso = document.createElement('span');
        aviso.className = 'submenu-aviso';
        aviso.textContent = 'Esperando conexión con el servidor…';
        li.appendChild(aviso);
        submenu.appendChild(li);
    }
});


// Navegación adaptable del portal institucional
document.addEventListener('DOMContentLoaded', () => {
  const boton = document.getElementById('boton-menu');
  const nav = document.getElementById('navegacion-principal');
  if (boton && nav) {
    boton.addEventListener('click', () => {
      const abierto = nav.classList.toggle('navegacion--abierta');
      boton.setAttribute('aria-expanded', String(abierto));
    });
  }
});


// ==========================================
// BUSCADOR INSTANTÁNEO POR NOMBRE/TEXTO
// Compartido por todos los módulos del sistema.
// Filtra las filas visibles sin modificar ni consultar de nuevo el backend.
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    const buscador = document.getElementById('buscarPorNombre');
    if (!buscador) return;

    const cuerpo = document.querySelector('table tbody');
    if (!cuerpo) return;

    const normalizar = (texto) => (texto || '')
        .toLocaleLowerCase('es')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .trim();

    const aplicarFiltro = () => {
        const consulta = normalizar(buscador.value);
        const filas = Array.from(cuerpo.querySelectorAll('tr'))
            .filter(fila => !fila.classList.contains('fila-sin-resultados'));
        let visibles = 0;

        filas.forEach(fila => {
            const celdas = fila.querySelectorAll('td');
            // Mantener visibles los mensajes de carga/error cuando el campo está vacío.
            const esMensaje = celdas.length === 1 && celdas[0].hasAttribute('colspan');
            if (esMensaje) {
                fila.hidden = Boolean(consulta);
                return;
            }
            const coincide = !consulta || normalizar(fila.textContent).includes(consulta);
            fila.hidden = !coincide;
            if (coincide) visibles++;
        });

        let aviso = cuerpo.querySelector('.fila-sin-resultados');
        if (consulta && visibles === 0) {
            if (!aviso) {
                aviso = document.createElement('tr');
                aviso.className = 'fila-sin-resultados';
                const celda = document.createElement('td');
                celda.colSpan = cuerpo.closest('table')?.querySelectorAll('thead th').length || 1;
                celda.className = 'busqueda-sin-resultados';
                celda.textContent = 'No se encontraron registros que coincidan con la búsqueda.';
                aviso.appendChild(celda);
                cuerpo.appendChild(aviso);
            }
            aviso.hidden = false;
        } else if (aviso) {
            aviso.hidden = true;
        }
    };

    buscador.addEventListener('input', aplicarFiltro);
    // Las tablas se vuelven a dibujar cuando se registra/actualiza un elemento o se consulta la API.
    const observador = new MutationObserver(aplicarFiltro);
    observador.observe(cuerpo, { childList: true, subtree: true, characterData: true });
    aplicarFiltro();
});

// ==========================================
// PÁGINA ACTUAL EN LA NAVEGACIÓN
// Marca el enlace del módulo abierto para que la barra indique
// dónde está el usuario, sin repetir el atributo en cada HTML.
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    const paginaActual = (window.location.pathname.split('/').pop() || 'index.html').toLowerCase();
    let marcado = false;

    document.querySelectorAll('.navegacion__enlace').forEach((enlace) => {
        if (marcado) return;

        const href = enlace.getAttribute('href') || '';
        // Los enlaces con ancla apuntan a una sección, no a otra página.
        if (!href || href.includes('#')) return;

        if (href.split('?')[0].toLowerCase() === paginaActual) {
            enlace.setAttribute('aria-current', 'page');
            marcado = true;
        }
    });
});

// ==========================================
// COMPORTAMIENTO COMÚN DE LOS MODALES
// - Cierra con Escape o al pulsar el fondo.
// - Enfoca el primer campo editable al abrirse.
// Los botones de cierre de cada vista siguen mandando: reutilizamos
// su onclick para no duplicar la limpieza de campos.
// ==========================================
function cerrarModalVisible(modal) {
    const botonCerrar = modal.querySelector('.btn-cerrar');
    if (botonCerrar) {
        botonCerrar.click();
        return;
    }
    modal.classList.remove('activo');
}

document.addEventListener('DOMContentLoaded', () => {
    const modales = document.querySelectorAll('.modal-overlay');

    modales.forEach((modal) => {
        modal.addEventListener('click', (evento) => {
            if (evento.target === modal) cerrarModalVisible(modal);
        });

        new MutationObserver(() => {
            if (!modal.classList.contains('activo')) return;
            const campo = modal.querySelector(
                'input:not([disabled]), select:not([disabled]), textarea:not([disabled])'
            );
            if (campo) campo.focus({ preventScroll: true });
        }).observe(modal, { attributes: true, attributeFilter: ['class'] });
    });

    document.addEventListener('keydown', (evento) => {
        if (evento.key !== 'Escape') return;
        const abierto = document.querySelector('.modal-overlay.activo');
        if (abierto) cerrarModalVisible(abierto);
    });
});
