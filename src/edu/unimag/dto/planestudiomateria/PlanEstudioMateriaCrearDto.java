package edu.unimag.dto.planestudiomateria;

import edu.unimag.dto.validacion.ReglasValidacion;

public record PlanEstudioMateriaCrearDto(
        Long idPlanEstudio,
        Integer idMateria,
        Integer semestre,
        Integer orden
        ) {

    public PlanEstudioMateriaCrearDto {
        idPlanEstudio = ReglasValidacion.limpiarLongRequerido(
                idPlanEstudio, "El plan de estudio es obligatorio");
        idMateria = ReglasValidacion.limpiarEnteroRequerido(
                idMateria, "La materia es obligatoria");
        semestre = ReglasValidacion.limpiarEnteroPositivo(
                semestre, "El semestre debe ser mayor o igual a uno");
        orden = ReglasValidacion.limpiarEnteroPositivo(
                orden, "El orden debe ser mayor o igual a uno");
    }
}
