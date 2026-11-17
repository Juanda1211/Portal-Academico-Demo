package edu.unimag.dto.monitoria;

import edu.unimag.dto.validacion.ReglasValidacion;
import edu.unimag.modelo.enumeracion.EstadoMonitoria;
import java.util.UUID;

public record MonitoriaActualizarDto(
        UUID idCursoPeriodo,
        UUID idEstudiante,
        Long idProfesor,
        Integer calificacion,
        String comentario
        ) {

    public MonitoriaActualizarDto {
        idCursoPeriodo = ReglasValidacion.limpiarUuidRequerido(
                idCursoPeriodo, "El curso periodo es obligatorio");
        idEstudiante = ReglasValidacion.limpiarUuidRequerido(
                idEstudiante, "El estudiante es obligatorio");
        idProfesor = ReglasValidacion.limpiarLongRequerido(
                idProfesor, "El profesor es obligatorio");
        calificacion = ReglasValidacion.limpiarEnteroEnRango(
                calificacion,
                EstadoMonitoria.NOTA_MINIMA,
                EstadoMonitoria.NOTA_MAXIMA,
                "La calificacion debe estar entre "
                + EstadoMonitoria.NOTA_MINIMA + " y "
                + EstadoMonitoria.NOTA_MAXIMA);
        comentario = ReglasValidacion.limpiarRequerido(
                comentario, "El comentario de la evaluacion es obligatorio");
    }
}
