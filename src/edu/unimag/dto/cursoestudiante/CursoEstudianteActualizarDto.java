package edu.unimag.dto.cursoestudiante;

import edu.unimag.dto.validacion.ReglasValidacion;
import edu.unimag.modelo.constante.PoliticasAcademicas;
import java.util.UUID;

public record CursoEstudianteActualizarDto(
        UUID idCursoPeriodo,
        UUID idEstudiante,
        Integer calificacion
        ) {

    public CursoEstudianteActualizarDto {
        idCursoPeriodo = ReglasValidacion.limpiarUuidRequerido(
                idCursoPeriodo, "El curso periodo es obligatorio");
        idEstudiante = ReglasValidacion.limpiarUuidRequerido(
                idEstudiante, "El estudiante es obligatorio");
        calificacion = ReglasValidacion.limpiarEnteroEnRango(
                calificacion,
                PoliticasAcademicas.NOTA_MINIMA,
                PoliticasAcademicas.NOTA_MAXIMA,
                "La calificacion debe estar entre "
                + PoliticasAcademicas.NOTA_MINIMA + " y "
                + PoliticasAcademicas.NOTA_MAXIMA);
    }
}
