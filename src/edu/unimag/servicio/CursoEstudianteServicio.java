package edu.unimag.servicio;

import edu.unimag.dto.cursoestudiante.CursoEstudianteCrearDto;
import edu.unimag.dto.cursoestudiante.CursoEstudianteDto;
import edu.unimag.dto.cursoestudiante.CursoEstudianteActualizarDto;
import edu.unimag.mapeador.CursoEstudianteMapeador;
import edu.unimag.modelo.CursoEstudiante;
import edu.unimag.modelo.CursoPeriodo;
import edu.unimag.modelo.Estudiante;
import edu.unimag.modelo.Materia;
import edu.unimag.modelo.PeriodoAcademico;
import edu.unimag.modelo.pk.CursoEstudiantePk;
import edu.unimag.repositorio.CursoEstudianteRepositorio;
import edu.unimag.repositorio.CursoPeriodoRepositorio;
import edu.unimag.repositorio.EstudianteRepositorio;
import edu.unimag.repositorio.MateriaRepositorio;
import edu.unimag.repositorio.PeriodoAcademicoRepositorio;
import edu.unimag.repositorio.cargador.CursoPeriodoCargador;
import edu.unimag.repositorio.cargador.EstudianteCargador;
import edu.unimag.repositorio.cargador.MateriaCargador;
import edu.unimag.repositorio.cargador.PeriodoAcademicoCargador;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio del ciclo de vida de la inscripción de un estudiante a un
 * curso-período.
 *
 * Los listados por materia y por período académico se resuelven acá y no en el
 * repositorio: la clave compuesta hidrata el {@code CursoPeriodo} solo con su
 * identificador, así que la fila de inscripción no conoce la materia ni el
 * período. Es el mismo criterio que usa {@code CursoProfesorServicio}.
 */
public class CursoEstudianteServicio {

    private final CursoEstudianteRepositorio cursoEstudianteRepositorio;
    private final EstudianteRepositorio estudianteRepositorio;
    private final CursoPeriodoRepositorio cursoPeriodoRepositorio;
    private final MateriaRepositorio materiaRepositorio;
    private final PeriodoAcademicoRepositorio periodoAcademicoRepositorio;
    private final CursoEstudianteMapeador mapeador;

    public CursoEstudianteServicio(
            CursoEstudianteRepositorio cursoEstudianteRepositorio,
            EstudianteRepositorio estudianteRepositorio,
            CursoPeriodoRepositorio cursoPeriodoRepositorio,
            MateriaRepositorio materiaRepositorio,
            PeriodoAcademicoRepositorio periodoAcademicoRepositorio,
            CursoEstudianteMapeador mapeador
    ) {
        if (cursoEstudianteRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de inscripciones no puede ser nulo");
        }
        if (estudianteRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de estudiantes no puede ser nulo");
        }
        if (cursoPeriodoRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de cursos periodo no puede ser nulo");
        }
        if (materiaRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de materias no puede ser nulo");
        }
        if (periodoAcademicoRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de periodos academicos no puede ser nulo");
        }
        if (mapeador == null) {
            throw new IllegalArgumentException("El mapeador no puede ser nulo");
        }
        this.cursoEstudianteRepositorio = cursoEstudianteRepositorio;
        this.estudianteRepositorio = estudianteRepositorio;
        this.cursoPeriodoRepositorio = cursoPeriodoRepositorio;
        this.materiaRepositorio = materiaRepositorio;
        this.periodoAcademicoRepositorio = periodoAcademicoRepositorio;
        this.mapeador = mapeador;
    }

    // Operaciones del ciclo de vida

    public CursoEstudianteDto inscribir(CursoEstudianteCrearDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la inscripcion no pueden ser nulos");
        }

        Estudiante estudiante = estudianteRepositorio.buscarPorId(dto.idEstudiante()).orElseThrow(
                () -> new IllegalArgumentException("No existe el estudiante con id " + dto.idEstudiante()));

        CursoPeriodo cursoPeriodo = cursoPeriodoRepositorio.buscarPorId(dto.idCursoPeriodo()).orElseThrow(
                () -> new IllegalArgumentException("No existe el curso periodo con id " + dto.idCursoPeriodo()));

        CursoEstudiantePk pk = new CursoEstudiantePk(cursoPeriodo, estudiante);
        if (cursoEstudianteRepositorio.buscarPorClave(pk).isPresent()) {
            throw new IllegalStateException("El estudiante ya esta inscripto en este curso periodo");
        }

        CursoEstudiante nuevaInscripcion = new CursoEstudiante(pk);
        if (!cursoEstudianteRepositorio.insertarSiNoExiste(nuevaInscripcion)) {
            throw new IllegalStateException("El estudiante ya esta inscripto en este curso periodo");
        }

        return hidratarYmapearUnico(nuevaInscripcion, estudiante, cursoPeriodo);
    }

    public CursoEstudianteDto admitir(UUID idCursoPeriodo, UUID idEstudiante) {
        CursoEstudiante inscripcion = buscarOFallar(idCursoPeriodo, idEstudiante);
        inscripcion.admitir();
        CursoEstudiante actualizada = cursoEstudianteRepositorio.actualizar(inscripcion);
        return hidratarYmapearUnico(actualizada);
    }

    public CursoEstudianteDto evaluar(CursoEstudianteActualizarDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la evaluacion no pueden ser nulos");
        }
        CursoEstudiante inscripcion = buscarOFallar(dto.idCursoPeriodo(), dto.idEstudiante());
        inscripcion.evaluar(dto.calificacion());
        CursoEstudiante actualizada = cursoEstudianteRepositorio.actualizar(inscripcion);
        return hidratarYmapearUnico(actualizada);
    }

    public CursoEstudianteDto retirar(UUID idCursoPeriodo, UUID idEstudiante) {
        CursoEstudiante inscripcion = buscarOFallar(idCursoPeriodo, idEstudiante);
        inscripcion.retirar();
        CursoEstudiante actualizada = cursoEstudianteRepositorio.actualizar(inscripcion);
        return hidratarYmapearUnico(actualizada);
    }

    public CursoEstudianteDto rechazar(UUID idCursoPeriodo, UUID idEstudiante) {
        CursoEstudiante inscripcion = buscarOFallar(idCursoPeriodo, idEstudiante);
        inscripcion.rechazar();
        CursoEstudiante actualizada = cursoEstudianteRepositorio.actualizar(inscripcion);
        return hidratarYmapearUnico(actualizada);
    }

    // Consultas

    public List<CursoEstudianteDto> listarTodas() {
        return hidratarYmapearLista(cursoEstudianteRepositorio.listarTodos());
    }

    public List<CursoEstudianteDto> listarPorEstudiante(UUID idEstudiante) {
        Estudiante estudiante = estudianteRepositorio.buscarPorId(idEstudiante).orElseThrow(
                () -> new IllegalArgumentException("No existe el estudiante con id " + idEstudiante));

        List<CursoEstudiante> inscripciones
                = cursoEstudianteRepositorio.listarPorEstudiante(idEstudiante);
        return hidratarYmapearLista(inscripciones, estudiante);
    }

    public List<CursoEstudianteDto> listarPorCursoPeriodo(UUID idCursoPeriodo) {
        CursoPeriodo cursoPeriodo = cursoPeriodoRepositorio.buscarPorId(idCursoPeriodo).orElseThrow(
                () -> new IllegalArgumentException("No existe el curso periodo con id " + idCursoPeriodo));

        List<CursoEstudiante> inscripciones
                = cursoEstudianteRepositorio.listarPorCursoPeriodo(idCursoPeriodo);
        return hidratarYmapearLista(inscripciones, null, cursoPeriodo);
    }

    public List<CursoEstudianteDto> listarPorMateria(Integer idMateria) {
        Materia materia = materiaRepositorio.buscarPorId(idMateria).orElseThrow(
                () -> new IllegalArgumentException("No existe la materia con id " + idMateria));

        Set<UUID> idsCursos = new HashSet<>();
        for (CursoPeriodo cursoPeriodo : cursoPeriodoRepositorio.listarTodos()) {
            if (idMateria.equals(cursoPeriodo.getIdMateria())) {
                idsCursos.add(cursoPeriodo.getIdCursoPeriodo());
            }
        }
        return hidratarYmapearLista(filtrarPorCursos(idsCursos), null, null, materia, null);
    }

    public List<CursoEstudianteDto> listarPorMateriaPeriodo(Integer idMateria, Integer idPeriodoAcademico) {
        Materia materia = materiaRepositorio.buscarPorId(idMateria).orElseThrow(
                () -> new IllegalArgumentException("No existe la materia con id " + idMateria));
        PeriodoAcademico periodo = periodoAcademicoRepositorio.buscarPorId(idPeriodoAcademico).orElseThrow(
                () -> new IllegalArgumentException("No existe el periodo con id " + idPeriodoAcademico));

        Set<UUID> idsCursos = new HashSet<>();
        for (CursoPeriodo cursoPeriodo : cursoPeriodoRepositorio.listarTodos()) {
            if (idMateria.equals(cursoPeriodo.getIdMateria())
                    && idPeriodoAcademico.equals(cursoPeriodo.getIdPeriodoAcademico())) {
                idsCursos.add(cursoPeriodo.getIdCursoPeriodo());
            }
        }
        return hidratarYmapearLista(filtrarPorCursos(idsCursos), null, null, materia, periodo);
    }

    public List<CursoEstudianteDto> listarPorPeriodo(Integer idPeriodoAcademico) {
        PeriodoAcademico periodo = periodoAcademicoRepositorio.buscarPorId(idPeriodoAcademico).orElseThrow(
                () -> new IllegalArgumentException("No existe el periodo con id " + idPeriodoAcademico));

        Set<UUID> idsCursos = new HashSet<>();
        for (CursoPeriodo cursoPeriodo : cursoPeriodoRepositorio.listarTodos()) {
            if (idPeriodoAcademico.equals(cursoPeriodo.getIdPeriodoAcademico())) {
                idsCursos.add(cursoPeriodo.getIdCursoPeriodo());
            }
        }
        return hidratarYmapearLista(filtrarPorCursos(idsCursos), null, null, null, periodo);
    }

    // Apoyo

    private CursoEstudiante buscarOFallar(UUID idCursoPeriodo, UUID idEstudiante) {
        CursoPeriodo cursoPeriodo = cursoPeriodoRepositorio.buscarPorId(idCursoPeriodo).orElseThrow(
                () -> new IllegalArgumentException("No existe el curso periodo con id " + idCursoPeriodo));
        Estudiante estudiante = estudianteRepositorio.buscarPorId(idEstudiante).orElseThrow(
                () -> new IllegalArgumentException("No existe el estudiante con id " + idEstudiante));

        CursoEstudiantePk pk = new CursoEstudiantePk(cursoPeriodo, estudiante);
        return cursoEstudianteRepositorio.buscarPorClave(pk).orElseThrow(
                () -> new IllegalArgumentException("No existe la inscripcion de ese estudiante en ese curso periodo"));
    }

    private List<CursoEstudiante> filtrarPorCursos(Set<UUID> idsCursos) {
        if (idsCursos.isEmpty()) {
            return List.of();
        }
        List<CursoEstudiante> resultado = new ArrayList<>();
        for (CursoEstudiante inscripcion : cursoEstudianteRepositorio.listarTodos()) {
            if (idsCursos.contains(inscripcion.getIdCursoPeriodo())) {
                resultado.add(inscripcion);
            }
        }
        return resultado;
    }

    private CursoEstudianteDto hidratarYmapearUnico(CursoEstudiante inscripcion) {
        Estudiante estudiante = estudianteRepositorio.buscarPorId(inscripcion.getIdEstudiante())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, estudiante no encontrado"));
        CursoPeriodo cursoPeriodo = cursoPeriodoRepositorio.buscarPorId(inscripcion.getIdCursoPeriodo())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, curso periodo no encontrado"));
        return hidratarYmapearUnico(inscripcion, estudiante, cursoPeriodo);
    }

    private CursoEstudianteDto hidratarYmapearUnico(
            CursoEstudiante inscripcion,
            Estudiante estudiante,
            CursoPeriodo cursoPeriodo
    ) {
        Materia materia = materiaRepositorio.buscarPorId(cursoPeriodo.getIdMateria())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, materia no encontrada"));
        PeriodoAcademico periodo = periodoAcademicoRepositorio
                .buscarPorId(cursoPeriodo.getIdPeriodoAcademico())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, periodo no encontrado"));

        EstudianteCargador estudianteCargador = new EstudianteCargador(estudianteRepositorio);
        estudianteCargador.registrarEnCache(estudiante);

        CursoPeriodoCargador cursoPeriodoCargador = new CursoPeriodoCargador(cursoPeriodoRepositorio);
        cursoPeriodoCargador.registrarEnCache(cursoPeriodo);

        MateriaCargador materiaCargador = new MateriaCargador(materiaRepositorio);
        materiaCargador.registrarEnCache(materia);

        PeriodoAcademicoCargador periodoCargador = new PeriodoAcademicoCargador(periodoAcademicoRepositorio);
        periodoCargador.registrarEnCache(periodo);

        return mapeador.toDto(
                inscripcion, estudianteCargador, cursoPeriodoCargador, materiaCargador, periodoCargador);
    }

    private List<CursoEstudianteDto> hidratarYmapearLista(List<CursoEstudiante> inscripciones) {
        return hidratarYmapearLista(inscripciones, null, null, null, null);
    }

    private List<CursoEstudianteDto> hidratarYmapearLista(
            List<CursoEstudiante> inscripciones,
            Estudiante estudianteConocido
    ) {
        return hidratarYmapearLista(inscripciones, estudianteConocido, null, null, null);
    }

    private List<CursoEstudianteDto> hidratarYmapearLista(
            List<CursoEstudiante> inscripciones,
            Estudiante estudianteConocido,
            CursoPeriodo cursoPeriodoConocido
    ) {
        return hidratarYmapearLista(inscripciones, estudianteConocido, cursoPeriodoConocido, null, null);
    }

    private List<CursoEstudianteDto> hidratarYmapearLista(
            List<CursoEstudiante> inscripciones,
            Estudiante estudianteConocido,
            CursoPeriodo cursoPeriodoConocido,
            Materia materiaConocida,
            PeriodoAcademico periodoConocido
    ) {
        if (inscripciones == null || inscripciones.isEmpty()) {
            return List.of();
        }

        Set<UUID> idsEstudiante = new HashSet<>();
        Set<UUID> idsCursoPeriodo = new HashSet<>();
        for (CursoEstudiante inscripcion : inscripciones) {
            idsEstudiante.add(inscripcion.getIdEstudiante());
            idsCursoPeriodo.add(inscripcion.getIdCursoPeriodo());
        }

        EstudianteCargador estudianteCargador = new EstudianteCargador(estudianteRepositorio);
        if (estudianteConocido != null) {
            estudianteCargador.registrarEnCache(estudianteConocido);
        }
        estudianteCargador.cargarPorIds(new ArrayList<>(idsEstudiante));

        CursoPeriodoCargador cursoPeriodoCargador = new CursoPeriodoCargador(cursoPeriodoRepositorio);
        if (cursoPeriodoConocido != null) {
            cursoPeriodoCargador.registrarEnCache(cursoPeriodoConocido);
        }
        cursoPeriodoCargador.cargarPorIds(new ArrayList<>(idsCursoPeriodo));

        Set<Integer> idsMateria = new HashSet<>();
        Set<Integer> idsPeriodo = new HashSet<>();
        for (UUID idCursoPeriodo : idsCursoPeriodo) {
            cursoPeriodoCargador.obtener(idCursoPeriodo).ifPresent(curso -> {
                idsMateria.add(curso.getIdMateria());
                idsPeriodo.add(curso.getIdPeriodoAcademico());
            });
        }

        MateriaCargador materiaCargador = new MateriaCargador(materiaRepositorio);
        if (materiaConocida != null) {
            materiaCargador.registrarEnCache(materiaConocida);
        }
        materiaCargador.cargarPorIds(new ArrayList<>(idsMateria));

        PeriodoAcademicoCargador periodoCargador = new PeriodoAcademicoCargador(periodoAcademicoRepositorio);
        if (periodoConocido != null) {
            periodoCargador.registrarEnCache(periodoConocido);
        }
        periodoCargador.cargarPorIds(new ArrayList<>(idsPeriodo));

        return mapeador.toDtoList(
                inscripciones, estudianteCargador, cursoPeriodoCargador, materiaCargador, periodoCargador);
    }
}
