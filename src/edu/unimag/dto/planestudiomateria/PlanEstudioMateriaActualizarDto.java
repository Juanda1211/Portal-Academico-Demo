package edu.unimag.dto.planestudiomateria;

import edu.unimag.dto.validacion.ReglasValidacion;

/**
 * Actualización parcial de una asignación plan/materia.
 *
 * {@code semestre} y {@code orden} son opcionales por separado, pero al menos
 * uno debe venir informado: si ambos son nulos no hay nada que actualizar. El
 * valor que llegue informado se valida como entero positivo.
 */
public record PlanEstudioMateriaActualizarDto(
        Long idPlanEstudio,
        Integer idMateria,
        Integer semestre,
        Integer orden
        ) {

    public PlanEstudioMateriaActualizarDto {
        idPlanEstudio = ReglasValidacion.limpiarLongRequerido(
                idPlanEstudio, "El plan de estudio es obligatorio");
        idMateria = ReglasValidacion.limpiarEnteroRequerido(
                idMateria, "La materia es obligatoria");

        if (semestre == null && orden == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el semestre, el orden o ambos para actualizar");
        }
        if (semestre != null) {
            semestre = ReglasValidacion.limpiarEnteroPositivo(
                    semestre, "El semestre debe ser mayor o igual a uno");
        }
        if (orden != null) {
            orden = ReglasValidacion.limpiarEnteroPositivo(
                    orden, "El orden debe ser mayor o igual a uno");
        }
    }
}
