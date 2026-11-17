// index.js - Lógica para cargar las estadísticas de la página de inicio

document.addEventListener('DOMContentLoaded', () => {
    cargarEstadisticasInicio();
});

async function cargarEstadisticasInicio() {
    try {
        // Hacemos todas las peticiones al mismo tiempo para que sea más rápido
        const [estudiantes, profesores, programas, facultades] = await Promise.all([
            peticionApi('/api/estudiantes'),
            peticionApi('/api/profesores'),
            peticionApi('/api/programas'),
            peticionApi('/api/facultades')
        ]);

        // Contamos el largo (length) de cada arreglo recibido y lo mostramos en el HTML
        let elemEstudiantes = document.getElementById('totalEstudiantes');
        if (elemEstudiantes) elemEstudiantes.innerText = estudiantes.length;

        let elemProfesores = document.getElementById('totalProfesores');
        if (elemProfesores) elemProfesores.innerText = profesores.length;

        let elemProgramas = document.getElementById('totalProgramas');
        if (elemProgramas) elemProgramas.innerText = programas.length;

        let elemFacultades = document.getElementById('totalFacultades');
        if (elemFacultades) elemFacultades.innerText = facultades.length;

    } catch (error) {
        console.error("Error al cargar las estadísticas:", error);
        
        // Si hay un error de conexión, mostramos un "-" o un mensaje de error
        const ids = ['totalEstudiantes', 'totalProfesores', 'totalProgramas', 'totalFacultades'];
        ids.forEach(id => {
            let elemento = document.getElementById(id);
            if (elemento) elemento.innerText = "-";
        });
    }
}