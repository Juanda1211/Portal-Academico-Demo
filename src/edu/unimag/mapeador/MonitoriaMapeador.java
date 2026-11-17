package edu.unimag.mapeador;

import edu.unimag.dto.cursoperiodo.CursoPeriodoDto;
import edu.unimag.dto.estudiante.EstudianteDto;
import edu.unimag.dto.monitoria.MonitoriaDto;
import edu.unimag.dto.profesor.ProfesorDto;
import edu.unimag.modelo.CursoPeriodo;
import edu.unimag.modelo.Estudiante;
import edu.unimag.modelo.Monitoria;
import edu.unimag.modelo.Profesor;
import edu.unimag.repositorio.cargador.CursoPeriodoCargador;
import edu.unimag.repositorio.cargador.EstudianteCargador;
import edu.unimag.repositorio.cargador.MateriaCargador;
import edu.unimag.repositorio.cargador.PeriodoAcademicoCargador;
import edu.unimag.repositorio.cargador.ProfesorCargador;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapeador de monitorías.
 *
 * La clave compuesta trae tres referencias y el curso-período a su vez trae
 * materia y período académico: cinco entidades a resolver por fila. Los
 * cargadores entran por parámetro para que el servicio pueda precargar los ids
 * de toda la lista una sola vez.
 */
public class MonitoriaMapeador {

    private final CursoPeriodoMapeador cursoPeriodoMapeador;
    private final EstudianteMapeador estudianteMapeador;
    private final ProfesorMapeador profesorMapeador;

    public MonitoriaMapeador(
            CursoPeriodoMapeador cursoPeriodoMapeador,
            EstudianteMapeador estudianteMapeador,
            ProfesorMapeador profesorMapeador
    ) {
        if (cursoPeriodoMapeador == null) {
            throw new IllegalArgumentException("El mapeador de curso periodo no puede ser nulo");
        }
        if (estudianteMapeador == null) {
            throw new IllegalArgumentException("El mapeador de estudiante no puede ser nulo");
        }
        if (profesorMapeador == null) {
            throw new IllegalArgumentException("El mapeador de profesor no puede ser nulo");
        }
        this.cursoPeriodoMapeador = cursoPeriodoMapeador;
        this.estudianteMapeador = estudianteMapeador;
        this.profesorMapeador = profesorMapeador;
    }

    public List<MonitoriaDto> toDtoList(
            List<Monitoria> entidades,
            CursoPeriodoCargador cursoPeriodoCargador,
            MateriaCargador materiaCargador,
            PeriodoAcademicoCargador periodoCargador,
            EstudianteCargador estudianteCargador,
            ProfesorCargador profesorCargador
    ) {
        if (entidades == null || entidades.isEmpty()) {
            return List.of();
        }
        validarCargadores(
                cursoPeriodoCargador, materiaCargador, periodoCargador,
                estudianteCargador, profesorCargador);

        List<MonitoriaDto> resultado = new ArrayList<>(entidades.size());
        for (Monitoria entidad : entidades) {
            if (!validarIntegridad(
                    entidad, cursoPeriodoCargador, materiaCargador, periodoCargador,
                    estudianteCargador, profesorCargador)) {
                throw new IllegalStateException(
                        "Error de integridad: una monitoria referencia entidades que no existen");
            }
            resultado.add(transformarADto(
                    entidad, cursoPeriodoCargador, materiaCargador, periodoCargador,
                    estudianteCargador, profesorCargador));
        }
        return resultado;
    }

    public MonitoriaDto toDto(
            Monitoria entidad,
            CursoPeriodoCargador cursoPeriodoCargador,
            MateriaCargador materiaCargador,
            PeriodoAcademicoCargador periodoCargador,
            EstudianteCargador estudianteCargador,
            ProfesorCargador profesorCargador
    ) {
        if (entidad == null) {
            throw new IllegalArgumentException("La entidad de monitoria no puede ser nula");
        }
        validarCargadores(
                cursoPeriodoCargador, materiaCargador, periodoCargador,
                estudianteCargador, profesorCargador);

        if (!validarIntegridad(
                entidad, cursoPeriodoCargador, materiaCargador, periodoCargador,
                estudianteCargador, profesorCargador)) {
            throw new IllegalStateException(
                    "Error de integridad: la monitoria referencia entidades que no existen");
        }
        return transformarADto(
                entidad, cursoPeriodoCargador, materiaCargador, periodoCargador,
                estudianteCargador, profesorCargador);
    }

    private void validarCargadores(
            CursoPeriodoCargador cursoPeriodoCargador,
            MateriaCargador materiaCargador,
            PeriodoAcademicoCargador periodoCargador,
            EstudianteCargador estudianteCargador,
            ProfesorCargador profesorCargador
    ) {
        if (cursoPeriodoCargador == null) {
            throw new IllegalArgumentException("El cargador de cursos periodo no puede ser nulo");
        }
        if (materiaCargador == null) {
            throw new IllegalArgumentException("El cargador de materias no puede ser nulo");
        }
        if (periodoCargador == null) {
            throw new IllegalArgumentException("El cargador de periodos academicos no puede ser nulo");
        }
        if (estudianteCargador == null) {
            throw new IllegalArgumentException("El cargador de estudiantes no puede ser nulo");
        }
        if (profesorCargador == null) {
            throw new IllegalArgumentException("El cargador de profesores no puede ser nulo");
        }
    }

    private boolean validarIntegridad(
            Monitoria entidad,
            CursoPeriodoCargador cursoPeriodoCargador,
            MateriaCargador materiaCargador,
            PeriodoAcademicoCargador periodoCargador,
            EstudianteCargador estudianteCargador,
            ProfesorCargador profesorCargador
    ) {
        if (!estudianteCargador.existe(entidad.getIdEstudiante())) {
            return false;
        }
        if (!profesorCargador.existe(entidad.getIdProfesor())) {
            return false;
        }

        CursoPeriodo cursoPeriodo = cursoPeriodoCargador
                .obtener(entidad.getIdCursoPeriodo()).orElse(null);
        if (cursoPeriodo == null) {
            return false;
        }
        if (!materiaCargador.existe(cursoPeriodo.getIdMateria())) {
            return false;
        }
        return periodoCargador.existe(cursoPeriodo.getIdPeriodoAcademico());
    }

    private MonitoriaDto transformarADto(
            Monitoria entidad,
            CursoPeriodoCargador cursoPeriodoCargador,
            MateriaCargador materiaCargador,
            PeriodoAcademicoCargador periodoCargador,
            EstudianteCargador estudianteCargador,
            ProfesorCargador profesorCargador
    ) {
        CursoPeriodo cursoPeriodo = cursoPeriodoCargador.obtener(entidad.getIdCursoPeriodo())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, curso no encontrado"));
        CursoPeriodoDto cursoPeriodoDto = cursoPeriodoMapeador.toDto(
                cursoPeriodo, materiaCargador, periodoCargador);

        Estudiante estudiante = estudianteCargador.obtener(entidad.getIdEstudiante())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, estudiante no encontrado"));
        EstudianteDto estudianteDto = estudianteMapeador.toDto(estudiante);

        Profesor profesor = profesorCargador.obtener(entidad.getIdProfesor())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, profesor no encontrado"));
        ProfesorDto profesorDto = profesorMapeador.toDto(profesor);

        return new MonitoriaDto(
                cursoPeriodoDto,
                estudianteDto,
                profesorDto,
                entidad.getEstadoMonitoria(),
                entidad.getCalificacionMonitoria(),
                entidad.getComentarioMonitoria(),
                entidad.getFechaCreacionMonitoria(),
                entidad.getFechaEvaluacionMonitoria());
    }
}
