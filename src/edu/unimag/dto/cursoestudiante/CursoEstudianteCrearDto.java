package edu.unimag.dto.cursoestudiante;

import edu.unimag.dto.validacion.ReglasValidacion;
import java.util.UUID;

public record CursoEstudianteCrearDto(
        UUID idCursoPeriodo,
        UUID idEstudiante
        ) {

    public CursoEstudianteCrearDto {
        idCursoPeriodo = ReglasValidacion.limpiarUuidRequerido(
                idCursoPeriodo, "El curso periodo es obligatorio");
        idEstudiante = ReglasValidacion.limpiarUuidRequerido(
                idEstudiante, "El estudiante es obligatorio");
    }
}
