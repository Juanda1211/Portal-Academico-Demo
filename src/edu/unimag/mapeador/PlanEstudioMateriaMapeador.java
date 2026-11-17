package edu.unimag.mapeador;

import edu.unimag.dto.materia.MateriaDto;
import edu.unimag.dto.planestudio.PlanEstudioDto;
import edu.unimag.dto.planestudiomateria.PlanEstudioMateriaDto;
import edu.unimag.modelo.Materia;
import edu.unimag.modelo.PlanEstudio;
import edu.unimag.modelo.Programa;
import edu.unimag.modelo.PlanEstudioMateria;
import edu.unimag.repositorio.cargador.FacultadCargador;
import edu.unimag.repositorio.cargador.MateriaCargador;
import edu.unimag.repositorio.cargador.PlanEstudioCargador;
import edu.unimag.repositorio.cargador.ProgramaCargador;
import java.util.ArrayList;
import java.util.List;

public class PlanEstudioMateriaMapeador {

    private final PlanEstudioMapeador planEstudioMapeador;
    private final MateriaMapeador materiaMapeador;

    public PlanEstudioMateriaMapeador(
            PlanEstudioMapeador planEstudioMapeador,
            MateriaMapeador materiaMapeador
    ) {
        if (planEstudioMapeador == null) {
            throw new IllegalArgumentException("El mapeador del plan de estudio no puede ser nulo");
        }
        if (materiaMapeador == null) {
            throw new IllegalArgumentException("El mapeador de materias no puede ser nulo");
        }
        this.planEstudioMapeador = planEstudioMapeador;
        this.materiaMapeador = materiaMapeador;
    }

    public List<PlanEstudioMateriaDto> toDtoList(
            List<PlanEstudioMateria> entidades,
            PlanEstudioCargador planEstudioCargador,
            ProgramaCargador programaCargador,
            FacultadCargador facultadCargador,
            MateriaCargador materiaCargador
    ) {
        if (entidades == null || entidades.isEmpty()) {
            return List.of();
        }
        validarCargadores(planEstudioCargador, programaCargador, facultadCargador, materiaCargador);

        List<PlanEstudioMateriaDto> resultado = new ArrayList<>(entidades.size());
        for (PlanEstudioMateria entidad : entidades) {
            if (!validarIntegridad(
                    entidad, planEstudioCargador, programaCargador, facultadCargador, materiaCargador)) {
                continue;
            }
            resultado.add(transformarADto(
                    entidad, planEstudioCargador, programaCargador, facultadCargador, materiaCargador));
        }
        return resultado;
    }

    public PlanEstudioMateriaDto toDto(
            PlanEstudioMateria entidad,
            PlanEstudioCargador planEstudioCargador,
            ProgramaCargador programaCargador,
            FacultadCargador facultadCargador,
            MateriaCargador materiaCargador
    ) {
        if (entidad == null) {
            throw new IllegalArgumentException("La entidad no puede ser nula");
        }
        validarCargadores(planEstudioCargador, programaCargador, facultadCargador, materiaCargador);

        if (!validarIntegridad(
                entidad, planEstudioCargador, programaCargador, facultadCargador, materiaCargador)) {
            throw new IllegalStateException(
                    "Error de integridad: la asignacion referencia entidades que no existen");
        }
        return transformarADto(
                entidad, planEstudioCargador, programaCargador, facultadCargador, materiaCargador);
    }

    private void validarCargadores(
            PlanEstudioCargador planEstudioCargador,
            ProgramaCargador programaCargador,
            FacultadCargador facultadCargador,
            MateriaCargador materiaCargador
    ) {
        if (planEstudioCargador == null) {
            throw new IllegalArgumentException("El cargador de planes no puede ser nulo");
        }
        if (programaCargador == null) {
            throw new IllegalArgumentException("El cargador de programas no puede ser nulo");
        }
        if (facultadCargador == null) {
            throw new IllegalArgumentException("El cargador de facultades no puede ser nulo");
        }
        if (materiaCargador == null) {
            throw new IllegalArgumentException("El cargador de materias no puede ser nulo");
        }
    }

    private boolean validarIntegridad(
            PlanEstudioMateria entidad,
            PlanEstudioCargador planEstudioCargador,
            ProgramaCargador programaCargador,
            FacultadCargador facultadCargador,
            MateriaCargador materiaCargador
    ) {
        PlanEstudio planEstudio = planEstudioCargador.obtener(entidad.getIdPlanEstudio()).orElse(null);
        if (planEstudio == null) {
            return false;
        }

        // El programa que cuelga de un plan viene como referencia: solo trae su
        // id. La facultad hay que resolverla desde el cargador, no desde
        // planEstudio.getPrograma().getIdFacultad(), que cruza dos saltos y el
        // intermedio llega nulo.
        Programa programa = programaCargador.obtener(planEstudio.getIdPrograma()).orElse(null);
        if (programa == null) {
            return false;
        }
        if (!facultadCargador.existe(programa.getIdFacultad())) {
            return false;
        }
        return materiaCargador.existe(entidad.getIdMateria());
    }

    private PlanEstudioMateriaDto transformarADto(
            PlanEstudioMateria entidad,
            PlanEstudioCargador planEstudioCargador,
            ProgramaCargador programaCargador,
            FacultadCargador facultadCargador,
            MateriaCargador materiaCargador
    ) {
        PlanEstudio planEstudio = planEstudioCargador.obtener(entidad.getIdPlanEstudio())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, plan no encontrado"));
        PlanEstudioDto planEstudioDto = planEstudioMapeador.toDto(
                planEstudio, programaCargador, facultadCargador);

        Materia materia = materiaCargador.obtener(entidad.getIdMateria())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, materia no encontrada"));
        MateriaDto materiaDto = materiaMapeador.toDto(materia);

        return new PlanEstudioMateriaDto(
                planEstudioDto,
                materiaDto,
                entidad.getSemestrePlanEstudioMateria(),
                entidad.getOrdenPlanEstudioMateria());
    }
}
