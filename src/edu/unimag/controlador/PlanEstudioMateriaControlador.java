package edu.unimag.controlador;

import edu.unimag.dto.planestudiomateria.PlanEstudioMateriaActualizarDto;
import edu.unimag.dto.planestudiomateria.PlanEstudioMateriaCrearDto;
import edu.unimag.dto.planestudiomateria.PlanEstudioMateriaDto;
import edu.unimag.servicio.PlanEstudioMateriaServicio;
import java.util.List;

public class PlanEstudioMateriaControlador {

    private final PlanEstudioMateriaServicio planEstudioMateriaServicio;

    public PlanEstudioMateriaControlador(PlanEstudioMateriaServicio planEstudioMateriaServicio) {
        if (planEstudioMateriaServicio == null) {
            throw new IllegalArgumentException("El servicio de asignaciones no puede ser nulo");
        }
        this.planEstudioMateriaServicio = planEstudioMateriaServicio;
    }

    public PlanEstudioMateriaDto asignar(PlanEstudioMateriaCrearDto dto) {
        return planEstudioMateriaServicio.asignar(dto);
    }

    public PlanEstudioMateriaDto actualizar(PlanEstudioMateriaActualizarDto dto) {
        return planEstudioMateriaServicio.actualizar(dto);
    }

    public boolean quitar(Long idPlanEstudio, Integer idMateria) {
        return planEstudioMateriaServicio.quitar(idPlanEstudio, idMateria);
    }

    public List<PlanEstudioMateriaDto> listarPorPlan(Long idPlanEstudio) {
        return planEstudioMateriaServicio.listarPorPlan(idPlanEstudio);
    }

    public List<PlanEstudioMateriaDto> listarPorMateria(Integer idMateria) {
        return planEstudioMateriaServicio.listarPorMateria(idMateria);
    }
}
