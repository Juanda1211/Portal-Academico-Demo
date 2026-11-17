package edu.unimag.mapeador;

import edu.unimag.dto.cursoestudiante.CursoEstudianteDto;
import edu.unimag.dto.cursoperiodo.CursoPeriodoDto;
import edu.unimag.dto.estudiante.EstudianteDto;
import edu.unimag.modelo.CursoEstudiante;
import edu.unimag.modelo.CursoPeriodo;
import edu.unimag.modelo.Estudiante;
import edu.unimag.repositorio.cargador.CursoPeriodoCargador;
import edu.unimag.repositorio.cargador.EstudianteCargador;
import edu.unimag.repositorio.cargador.MateriaCargador;
import edu.unimag.repositorio.cargador.PeriodoAcademicoCargador;
import java.util.ArrayList;
import java.util.List;

public class CursoEstudianteMapeador {

    private final EstudianteMapeador estudianteMapeador;
    private final CursoPeriodoMapeador cursoPeriodoMapeador;

    public CursoEstudianteMapeador(
            EstudianteMapeador estudianteMapeador,
            CursoPeriodoMapeador cursoPeriodoMapeador
    ) {
        if (estudianteMapeador == null) {
            throw new IllegalArgumentException("El mapeador del estudiante no puede ser nulo");
        }
        if (cursoPeriodoMapeador == null) {
            throw new IllegalArgumentException("El mapeador del curso periodo no puede ser nulo");
        }
        this.estudianteMapeador = estudianteMapeador;
        this.cursoPeriodoMapeador = cursoPeriodoMapeador;
    }

    public List<CursoEstudianteDto> toDtoList(
            List<CursoEstudiante> entidades,
            EstudianteCargador estudianteCargador,
            CursoPeriodoCargador cursoPeriodoCargador,
            MateriaCargador materiaCargador,
            PeriodoAcademicoCargador periodoCargador
    ) {
        if (entidades == null || entidades.isEmpty()) {
            return List.of();
        }
        validarCargadores(estudianteCargador, cursoPeriodoCargador, materiaCargador, periodoCargador);

        List<CursoEstudianteDto> resultado = new ArrayList<>(entidades.size());
        for (CursoEstudiante entidad : entidades) {
            if (!validarIntegridad(
                    entidad, estudianteCargador, cursoPeriodoCargador, materiaCargador, periodoCargador)) {
                continue;
            }
            resultado.add(transformarADto(
                    entidad, estudianteCargador, cursoPeriodoCargador, materiaCargador, periodoCargador));
        }
        return resultado;
    }

    public CursoEstudianteDto toDto(
            CursoEstudiante entidad,
            EstudianteCargador estudianteCargador,
            CursoPeriodoCargador cursoPeriodoCargador,
            MateriaCargador materiaCargador,
            PeriodoAcademicoCargador periodoCargador
    ) {
        if (entidad == null) {
            throw new IllegalArgumentException("La entidad no puede ser nula");
        }
        validarCargadores(estudianteCargador, cursoPeriodoCargador, materiaCargador, periodoCargador);

        if (!validarIntegridad(
                entidad, estudianteCargador, cursoPeriodoCargador, materiaCargador, periodoCargador)) {
            throw new IllegalStateException(
                    "Error de integridad: la inscripcion referencia entidades que no existen");
        }
        return transformarADto(
                entidad, estudianteCargador, cursoPeriodoCargador, materiaCargador, periodoCargador);
    }

    public EstudianteMapeador getEstudianteMapeador() {
        return estudianteMapeador;
    }

    public CursoPeriodoMapeador getCursoPeriodoMapeador() {
        return cursoPeriodoMapeador;
    }

    private void validarCargadores(
            EstudianteCargador estudianteCargador,
            CursoPeriodoCargador cursoPeriodoCargador,
            MateriaCargador materiaCargador,
            PeriodoAcademicoCargador periodoCargador
    ) {
        if (estudianteCargador == null) {
            throw new IllegalArgumentException("El cargador de estudiantes no puede ser nulo");
        }
        if (cursoPeriodoCargador == null) {
            throw new IllegalArgumentException("El cargador de cursos periodo no puede ser nulo");
        }
        if (materiaCargador == null) {
            throw new IllegalArgumentException("El cargador de materias no puede ser nulo");
        }
        if (periodoCargador == null) {
            throw new IllegalArgumentException("El cargador de periodos no puede ser nulo");
        }
    }

    private boolean validarIntegridad(
            CursoEstudiante entidad,
            EstudianteCargador estudianteCargador,
            CursoPeriodoCargador cursoPeriodoCargador,
            MateriaCargador materiaCargador,
            PeriodoAcademicoCargador periodoCargador
    ) {
        if (!estudianteCargador.existe(entidad.getIdEstudiante())) {
            return false;
        }
        CursoPeriodo cursoPeriodo = cursoPeriodoCargador.obtener(entidad.getIdCursoPeriodo()).orElse(null);
        if (cursoPeriodo == null) {
            return false;
        }
        if (!materiaCargador.existe(cursoPeriodo.getIdMateria())) {
            return false;
        }
        return periodoCargador.existe(cursoPeriodo.getIdPeriodoAcademico());
    }

    private CursoEstudianteDto transformarADto(
            CursoEstudiante entidad,
            EstudianteCargador estudianteCargador,
            CursoPeriodoCargador cursoPeriodoCargador,
            MateriaCargador materiaCargador,
            PeriodoAcademicoCargador periodoCargador
    ) {
        Estudiante estudiante = estudianteCargador.obtener(entidad.getIdEstudiante())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, estudiante no encontrado"));
        EstudianteDto estudianteDto = estudianteMapeador.toDto(estudiante);

        CursoPeriodo cursoPeriodo = cursoPeriodoCargador.obtener(entidad.getIdCursoPeriodo())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, curso periodo no encontrado"));
        CursoPeriodoDto cursoPeriodoDto = cursoPeriodoMapeador.toDto(
                cursoPeriodo, materiaCargador, periodoCargador);

        return new CursoEstudianteDto(
                estudianteDto,
                cursoPeriodoDto,
                entidad.getEstadoInscripcion(),
                entidad.getCalificacionCursoEstudiante(),
                entidad.getFechaInscripcion(),
                entidad.getFechaEvaluacion());
    }
}
