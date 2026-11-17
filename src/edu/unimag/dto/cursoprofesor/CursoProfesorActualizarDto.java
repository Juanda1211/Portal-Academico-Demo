
package edu.unimag.dto.cursoprofesor;

import edu.unimag.modelo.enumeracion.TipoAsignacion;
import java.util.UUID;


public record CursoProfesorActualizarDto(
        UUID idCursoPeriodo,
        Long idProfesor,
        TipoAsignacion tipoAsignacion
        ){
    public CursoProfesorActualizarDto {
        if (idCursoPeriodo == null) {
            throw new IllegalArgumentException("Id curso null");
        }
        if (idProfesor == null) {
            throw new IllegalArgumentException("Id profesor null");
        }
        if (tipoAsignacion == null) {
            throw new IllegalArgumentException("tipo de asignacion null");
        }
    }

}
