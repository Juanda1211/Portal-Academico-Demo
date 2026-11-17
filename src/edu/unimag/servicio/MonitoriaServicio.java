package edu.unimag.servicio;

import edu.unimag.dto.monitoria.MonitoriaCrearDto;
import edu.unimag.dto.monitoria.MonitoriaDto;
import edu.unimag.dto.monitoria.MonitoriaActualizarDto;
import edu.unimag.mapeador.MonitoriaMapeador;
import edu.unimag.modelo.CursoEstudiante;
import edu.unimag.modelo.CursoPeriodo;
import edu.unimag.modelo.Estudiante;
import edu.unimag.modelo.Monitoria;
import edu.unimag.modelo.Profesor;
import edu.unimag.modelo.pk.CursoEstudiantePk;
import edu.unimag.modelo.pk.MonitoriaPk;
import edu.unimag.repositorio.CursoEstudianteRepositorio;
import edu.unimag.repositorio.CursoPeriodoRepositorio;
import edu.unimag.repositorio.EstudianteRepositorio;
import edu.unimag.repositorio.MateriaRepositorio;
import edu.unimag.repositorio.MonitoriaRepositorio;
import edu.unimag.repositorio.PeriodoAcademicoRepositorio;
import edu.unimag.repositorio.ProfesorRepositorio;
import edu.unimag.repositorio.cargador.CursoPeriodoCargador;
import edu.unimag.repositorio.cargador.EstudianteCargador;
import edu.unimag.repositorio.cargador.MateriaCargador;
import edu.unimag.repositorio.cargador.PeriodoAcademicoCargador;
import edu.unimag.repositorio.cargador.ProfesorCargador;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de monitorías académicas.
 *
 * Una monitoría se identifica por el curso-período, el estudiante y el
 * profesor. Además de existir las tres entidades, el estudiante debe estar
 * inscripto en ese curso-período: esa regla no es expresable en la clave
 * compuesta y se verifica antes de persistir.
 */
public class MonitoriaServicio {

    private final MonitoriaRepositorio monitoriaRepositorio;
    private final CursoPeriodoRepositorio cursoPeriodoRepositorio;
    private final EstudianteRepositorio estudianteRepositorio;
    private final ProfesorRepositorio profesorRepositorio;
    private final CursoEstudianteRepositorio cursoEstudianteRepositorio;
    private final MateriaRepositorio materiaRepositorio;
    private final PeriodoAcademicoRepositorio periodoAcademicoRepositorio;
    private final MonitoriaMapeador mapeador;

    public MonitoriaServicio(
            MonitoriaRepositorio monitoriaRepositorio,
            CursoPeriodoRepositorio cursoPeriodoRepositorio,
            EstudianteRepositorio estudianteRepositorio,
            ProfesorRepositorio profesorRepositorio,
            CursoEstudianteRepositorio cursoEstudianteRepositorio,
            MateriaRepositorio materiaRepositorio,
            PeriodoAcademicoRepositorio periodoAcademicoRepositorio,
            MonitoriaMapeador mapeador
    ) {
        if (monitoriaRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de monitorias no puede ser nulo");
        }
        if (cursoPeriodoRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de cursos periodo no puede ser nulo");
        }
        if (estudianteRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de estudiantes no puede ser nulo");
        }
        if (profesorRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de profesores no puede ser nulo");
        }
        if (cursoEstudianteRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de inscripciones no puede ser nulo");
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
        this.monitoriaRepositorio = monitoriaRepositorio;
        this.cursoPeriodoRepositorio = cursoPeriodoRepositorio;
        this.estudianteRepositorio = estudianteRepositorio;
        this.profesorRepositorio = profesorRepositorio;
        this.cursoEstudianteRepositorio = cursoEstudianteRepositorio;
        this.materiaRepositorio = materiaRepositorio;
        this.periodoAcademicoRepositorio = periodoAcademicoRepositorio;
        this.mapeador = mapeador;
    }

    // Operaciones

    public MonitoriaDto crear(MonitoriaCrearDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la monitoria no pueden ser nulos");
        }

        CursoPeriodo cursoPeriodo = cursoPeriodoRepositorio.buscarPorId(dto.idCursoPeriodo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el curso periodo con id " + dto.idCursoPeriodo()));
        Estudiante estudiante = estudianteRepositorio.buscarPorId(dto.idEstudiante())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el estudiante con id " + dto.idEstudiante()));
        Profesor profesor = profesorRepositorio.buscarPorId(dto.idProfesor())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el profesor con id " + dto.idProfesor()));

        exigirInscripcion(cursoPeriodo, estudiante);

        MonitoriaPk pk = new MonitoriaPk(cursoPeriodo, estudiante, profesor);
        if (monitoriaRepositorio.buscarPorClave(pk).isPresent()) {
            throw new IllegalStateException(
                    "Ya existe una monitoria para ese curso periodo, estudiante y profesor");
        }

        Monitoria nuevaMonitoria = new Monitoria(pk);
        if (!monitoriaRepositorio.insertarSiNoExiste(nuevaMonitoria)) {
            throw new IllegalStateException(
                    "Ya existe una monitoria para ese curso periodo, estudiante y profesor");
        }
        return hidratarYmapearUnico(nuevaMonitoria, cursoPeriodo, estudiante, profesor);
    }

    public MonitoriaDto evaluar(MonitoriaActualizarDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la evaluacion no pueden ser nulos");
        }

        Monitoria monitoria = buscarOFallar(dto.idCursoPeriodo(), dto.idEstudiante(), dto.idProfesor());
        monitoria.evaluar(dto.calificacion(), dto.comentario());

        Monitoria actualizada = monitoriaRepositorio.actualizar(monitoria);
        return hidratarYmapearUnico(actualizada);
    }

    public boolean eliminar(UUID idCursoPeriodo, UUID idEstudiante, Long idProfesor) {
        Monitoria monitoria = buscarOFallar(idCursoPeriodo, idEstudiante, idProfesor);
        return monitoriaRepositorio.eliminar(monitoria.getIdMonitoria());
    }

    // Consultas

    public List<MonitoriaDto> listar() {
        return hidratarYmapearLista(monitoriaRepositorio.listarTodos());
    }

    public List<MonitoriaDto> listarPorCursoPeriodo(UUID idCursoPeriodo) {
        cursoPeriodoRepositorio.buscarPorId(idCursoPeriodo).orElseThrow(
                () -> new IllegalArgumentException(
                        "No existe el curso periodo con id " + idCursoPeriodo));
        return hidratarYmapearLista(monitoriaRepositorio.listarPorCursoPeriodo(idCursoPeriodo));
    }

    public List<MonitoriaDto> listarPorEstudiante(UUID idEstudiante) {
        estudianteRepositorio.buscarPorId(idEstudiante).orElseThrow(
                () -> new IllegalArgumentException(
                        "No existe el estudiante con id " + idEstudiante));
        return hidratarYmapearLista(monitoriaRepositorio.listarPorEstudiante(idEstudiante));
    }

    public List<MonitoriaDto> listarPorProfesor(Long idProfesor) {
        profesorRepositorio.buscarPorId(idProfesor).orElseThrow(
                () -> new IllegalArgumentException(
                        "No existe el profesor con id " + idProfesor));
        return hidratarYmapearLista(monitoriaRepositorio.listarPorProfesor(idProfesor));
    }

    // Apoyo

    private void exigirInscripcion(CursoPeriodo cursoPeriodo, Estudiante estudiante) {
        CursoEstudiantePk pk = new CursoEstudiantePk(cursoPeriodo, estudiante);
        CursoEstudiante inscripcion = cursoEstudianteRepositorio.buscarPorClave(pk).orElse(null);

        // Retirada o rechazada es una inscripción que ya no vincula al
        // estudiante con el curso: no habilita la monitoría.
        boolean inscripto = inscripcion != null
                && !inscripcion.estaRetirada()
                && !inscripcion.estaRechazada();

        if (!inscripto) {
            throw new IllegalStateException(
                    "El estudiante no esta inscripto en el curso periodo indicado");
        }
    }

    private Monitoria buscarOFallar(UUID idCursoPeriodo, UUID idEstudiante, Long idProfesor) {
        MonitoriaPk pk = new MonitoriaPk(
                cursoPeriodoRepositorio.buscarPorId(idCursoPeriodo).orElseThrow(
                        () -> new IllegalArgumentException(
                                "No existe el curso periodo con id " + idCursoPeriodo)),
                estudianteRepositorio.buscarPorId(idEstudiante).orElseThrow(
                        () -> new IllegalArgumentException(
                                "No existe el estudiante con id " + idEstudiante)),
                profesorRepositorio.buscarPorId(idProfesor).orElseThrow(
                        () -> new IllegalArgumentException(
                                "No existe el profesor con id " + idProfesor)));

        return monitoriaRepositorio.buscarPorClave(pk).orElseThrow(
                () -> new IllegalArgumentException(
                        "No existe la monitoria indicada"));
    }

    private MonitoriaDto hidratarYmapearUnico(Monitoria monitoria) {
        CursoPeriodo cursoPeriodo = cursoPeriodoRepositorio.buscarPorId(monitoria.getIdCursoPeriodo())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, curso no encontrado"));
        Estudiante estudiante = estudianteRepositorio.buscarPorId(monitoria.getIdEstudiante())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, estudiante no encontrado"));
        Profesor profesor = profesorRepositorio.buscarPorId(monitoria.getIdProfesor())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, profesor no encontrado"));
        return hidratarYmapearUnico(monitoria, cursoPeriodo, estudiante, profesor);
    }

    private MonitoriaDto hidratarYmapearUnico(
            Monitoria monitoria,
            CursoPeriodo cursoPeriodo,
            Estudiante estudiante,
            Profesor profesor
    ) {
        CursoPeriodoCargador cursoPeriodoCargador = new CursoPeriodoCargador(cursoPeriodoRepositorio);
        cursoPeriodoCargador.registrarEnCache(cursoPeriodo);

        MateriaCargador materiaCargador = new MateriaCargador(materiaRepositorio);
        materiaCargador.cargarPorIds(List.of(cursoPeriodo.getIdMateria()));

        PeriodoAcademicoCargador periodoCargador = new PeriodoAcademicoCargador(periodoAcademicoRepositorio);
        periodoCargador.cargarPorIds(List.of(cursoPeriodo.getIdPeriodoAcademico()));

        EstudianteCargador estudianteCargador = new EstudianteCargador(estudianteRepositorio);
        estudianteCargador.registrarEnCache(estudiante);

        ProfesorCargador profesorCargador = new ProfesorCargador(profesorRepositorio);
        profesorCargador.registrarEnCache(profesor);

        return mapeador.toDto(
                monitoria,
                cursoPeriodoCargador,
                materiaCargador,
                periodoCargador,
                estudianteCargador,
                profesorCargador);
    }

    private List<MonitoriaDto> hidratarYmapearLista(List<Monitoria> monitorias) {
        if (monitorias == null || monitorias.isEmpty()) {
            return List.of();
        }

        Set<UUID> idsCursoPeriodo = new HashSet<>();
        Set<UUID> idsEstudiante = new HashSet<>();
        Set<Long> idsProfesor = new HashSet<>();
        for (Monitoria monitoria : monitorias) {
            idsCursoPeriodo.add(monitoria.getIdCursoPeriodo());
            idsEstudiante.add(monitoria.getIdEstudiante());
            idsProfesor.add(monitoria.getIdProfesor());
        }

        CursoPeriodoCargador cursoPeriodoCargador = new CursoPeriodoCargador(cursoPeriodoRepositorio);
        cursoPeriodoCargador.cargarPorIds(new ArrayList<>(idsCursoPeriodo));

        // El curso-período ya está en caché: de ahí salen los ids de materia y
        // de período académico sin volver a consultar el repositorio por fila.
        Set<Integer> idsMateria = new HashSet<>();
        Set<Integer> idsPeriodo = new HashSet<>();
        for (UUID idCursoPeriodo : idsCursoPeriodo) {
            cursoPeriodoCargador.obtener(idCursoPeriodo).ifPresent(cursoPeriodo -> {
                idsMateria.add(cursoPeriodo.getIdMateria());
                idsPeriodo.add(cursoPeriodo.getIdPeriodoAcademico());
            });
        }

        MateriaCargador materiaCargador = new MateriaCargador(materiaRepositorio);
        materiaCargador.cargarPorIds(new ArrayList<>(idsMateria));

        PeriodoAcademicoCargador periodoCargador = new PeriodoAcademicoCargador(periodoAcademicoRepositorio);
        periodoCargador.cargarPorIds(new ArrayList<>(idsPeriodo));

        EstudianteCargador estudianteCargador = new EstudianteCargador(estudianteRepositorio);
        estudianteCargador.cargarPorIds(new ArrayList<>(idsEstudiante));

        ProfesorCargador profesorCargador = new ProfesorCargador(profesorRepositorio);
        profesorCargador.cargarPorIds(new ArrayList<>(idsProfesor));

        return mapeador.toDtoList(
                monitorias,
                cursoPeriodoCargador,
                materiaCargador,
                periodoCargador,
                estudianteCargador,
                profesorCargador);
    }
}
