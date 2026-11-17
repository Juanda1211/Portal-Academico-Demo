package edu.unimag.servicio;

import edu.unimag.dto.planestudiomateria.PlanEstudioMateriaActualizarDto;
import edu.unimag.dto.planestudiomateria.PlanEstudioMateriaCrearDto;
import edu.unimag.dto.planestudiomateria.PlanEstudioMateriaDto;
import edu.unimag.mapeador.PlanEstudioMateriaMapeador;
import edu.unimag.modelo.Materia;
import edu.unimag.modelo.PlanEstudio;
import edu.unimag.modelo.PlanEstudioMateria;
import edu.unimag.modelo.pk.PlanEstudioMateriaPk;
import edu.unimag.repositorio.FacultadRepositorio;
import edu.unimag.repositorio.MateriaRepositorio;
import edu.unimag.repositorio.PlanEstudioMateriaRepositorio;
import edu.unimag.repositorio.PlanEstudioRepositorio;
import edu.unimag.repositorio.ProgramaRepositorio;
import edu.unimag.repositorio.cargador.FacultadCargador;
import edu.unimag.repositorio.cargador.MateriaCargador;
import edu.unimag.repositorio.cargador.PlanEstudioCargador;
import edu.unimag.repositorio.cargador.ProgramaCargador;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de asignación de materias a planes de estudio.
 *
 * Cada asignación ocupa un semestre y un orden dentro de ese semestre. El orden
 * es único por semestre dentro del mismo plan; el par plan/materia es único.
 */
public class PlanEstudioMateriaServicio {

    private final PlanEstudioMateriaRepositorio planEstudioMateriaRepositorio;
    private final PlanEstudioRepositorio planEstudioRepositorio;
    private final MateriaRepositorio materiaRepositorio;
    private final ProgramaRepositorio programaRepositorio;
    private final FacultadRepositorio facultadRepositorio;
    private final PlanEstudioMateriaMapeador mapeador;

    public PlanEstudioMateriaServicio(
            PlanEstudioMateriaRepositorio planEstudioMateriaRepositorio,
            PlanEstudioRepositorio planEstudioRepositorio,
            MateriaRepositorio materiaRepositorio,
            ProgramaRepositorio programaRepositorio,
            FacultadRepositorio facultadRepositorio,
            PlanEstudioMateriaMapeador mapeador
    ) {
        if (planEstudioMateriaRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de asignaciones no puede ser nulo");
        }
        if (planEstudioRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de planes no puede ser nulo");
        }
        if (materiaRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de materias no puede ser nulo");
        }
        if (programaRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de programas no puede ser nulo");
        }
        if (facultadRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de facultades no puede ser nulo");
        }
        if (mapeador == null) {
            throw new IllegalArgumentException("El mapeador no puede ser nulo");
        }
        this.planEstudioMateriaRepositorio = planEstudioMateriaRepositorio;
        this.planEstudioRepositorio = planEstudioRepositorio;
        this.materiaRepositorio = materiaRepositorio;
        this.programaRepositorio = programaRepositorio;
        this.facultadRepositorio = facultadRepositorio;
        this.mapeador = mapeador;
    }

    // Operaciones

    public PlanEstudioMateriaDto asignar(PlanEstudioMateriaCrearDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la asignacion no pueden ser nulos");
        }

        PlanEstudio planEstudio = planEstudioRepositorio.buscarPorId(dto.idPlanEstudio()).orElseThrow(
                () -> new IllegalArgumentException("No existe el plan de estudio con id " + dto.idPlanEstudio()));
        Materia materia = materiaRepositorio.buscarPorId(dto.idMateria()).orElseThrow(
                () -> new IllegalArgumentException("No existe la materia con id " + dto.idMateria()));

        PlanEstudioMateriaPk pk = new PlanEstudioMateriaPk(planEstudio, materia);
        if (planEstudioMateriaRepositorio.buscarPorClave(pk).isPresent()) {
            throw new IllegalStateException("La materia ya esta asignada a este plan de estudio");
        }
        if (planEstudioMateriaRepositorio.existeOrdenEnSemestre(
                dto.idPlanEstudio(), dto.semestre(), dto.orden())) {
            throw new IllegalStateException(
                    "El orden " + dto.orden() + " ya esta ocupado en el semestre " + dto.semestre());
        }

        PlanEstudioMateria nuevaAsignacion = new PlanEstudioMateria(pk, dto.semestre(), dto.orden());
        if (!planEstudioMateriaRepositorio.insertarSiNoExiste(nuevaAsignacion)) {
            throw new IllegalStateException("La materia ya esta asignada a este plan de estudio");
        }
        return hidratarYmapearUnico(nuevaAsignacion, planEstudio, materia);
    }

    public PlanEstudioMateriaDto actualizar(PlanEstudioMateriaActualizarDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la actualizacion no pueden ser nulos");
        }

        PlanEstudioMateria asignacion = buscarOFallar(dto.idPlanEstudio(), dto.idMateria());

        Integer semestreFinal = asignacion.getSemestrePlanEstudioMateria();
        Integer ordenFinal = asignacion.getOrdenPlanEstudioMateria();

        boolean cambiaSemestre = false;
        if (dto.semestre() != null) {
            if (dto.semestre().equals(semestreFinal)) {
                throw new IllegalArgumentException(
                        "La materia ya se encuentra en el semestre " + dto.semestre());
            }
            semestreFinal = dto.semestre();
            cambiaSemestre = true;
        }

        boolean cambiaOrden = false;
        if (dto.orden() != null) {
            if (dto.orden().equals(ordenFinal)) {
                throw new IllegalArgumentException("La materia ya tiene el orden " + dto.orden());
            }
            ordenFinal = dto.orden();
            cambiaOrden = true;
        }

        if (planEstudioMateriaRepositorio.existeOrdenEnSemestreExcepto(
                dto.idPlanEstudio(), semestreFinal, ordenFinal, asignacion.getIdPlanEstudioMateria())) {
            throw new IllegalStateException(
                    "El orden " + ordenFinal + " ya esta ocupado en el semestre " + semestreFinal);
        }

        if (cambiaSemestre) {
            asignacion.reubicarSemestre(semestreFinal);
        }
        if (cambiaOrden) {
            asignacion.actualizarOrden(ordenFinal);
        }

        PlanEstudioMateria actualizada = planEstudioMateriaRepositorio.actualizar(asignacion);
        return hidratarYmapearUnico(actualizada);
    }

    public boolean quitar(Long idPlanEstudio, Integer idMateria) {
        PlanEstudioMateria asignacion = buscarOFallar(idPlanEstudio, idMateria);
        return planEstudioMateriaRepositorio.eliminar(asignacion.getIdPlanEstudioMateria());
    }

    // Consultas

    public List<PlanEstudioMateriaDto> listarPorPlan(Long idPlanEstudio) {
        PlanEstudio planEstudio = planEstudioRepositorio.buscarPorId(idPlanEstudio).orElseThrow(
                () -> new IllegalArgumentException("No existe el plan de estudio con id " + idPlanEstudio));

        List<PlanEstudioMateria> asignaciones = planEstudioMateriaRepositorio.listarPorPlan(idPlanEstudio);
        asignaciones.sort(Comparator
                .comparing(PlanEstudioMateria::getSemestrePlanEstudioMateria)
                .thenComparing(PlanEstudioMateria::getOrdenPlanEstudioMateria));

        return hidratarYmapearLista(asignaciones, planEstudio);
    }

    public List<PlanEstudioMateriaDto> listarPorMateria(Integer idMateria) {
        Materia materia = materiaRepositorio.buscarPorId(idMateria).orElseThrow(
                () -> new IllegalArgumentException("No existe la materia con id " + idMateria));

        List<PlanEstudioMateria> asignaciones = planEstudioMateriaRepositorio.listarPorMateria(idMateria);
        asignaciones.sort(Comparator
                .comparing(PlanEstudioMateria::getSemestrePlanEstudioMateria)
                .thenComparing(PlanEstudioMateria::getOrdenPlanEstudioMateria));

        return hidratarYmapearLista(asignaciones, null, materia);
    }

    // Apoyo

    private PlanEstudioMateria buscarOFallar(Long idPlanEstudio, Integer idMateria) {
        PlanEstudio planEstudio = planEstudioRepositorio.buscarPorId(idPlanEstudio).orElseThrow(
                () -> new IllegalArgumentException("No existe el plan de estudio con id " + idPlanEstudio));
        Materia materia = materiaRepositorio.buscarPorId(idMateria).orElseThrow(
                () -> new IllegalArgumentException("No existe la materia con id " + idMateria));

        PlanEstudioMateriaPk pk = new PlanEstudioMateriaPk(planEstudio, materia);
        return planEstudioMateriaRepositorio.buscarPorClave(pk).orElseThrow(
                () -> new IllegalArgumentException(
                        "No existe la asignacion de esa materia a ese plan de estudio"));
    }

    private PlanEstudioMateriaDto hidratarYmapearUnico(PlanEstudioMateria asignacion) {
        PlanEstudio planEstudio = planEstudioRepositorio.buscarPorId(asignacion.getIdPlanEstudio())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, plan no encontrado"));
        Materia materia = materiaRepositorio.buscarPorId(asignacion.getIdMateria())
                .orElseThrow(() -> new IllegalStateException("Error de integridad, materia no encontrada"));
        return hidratarYmapearUnico(asignacion, planEstudio, materia);
    }

    private PlanEstudioMateriaDto hidratarYmapearUnico(
            PlanEstudioMateria asignacion,
            PlanEstudio planEstudio,
            Materia materia
    ) {
        Cargadores cargadores = cargadoresDe(List.of(planEstudio), List.of(materia));
        return mapeador.toDto(
                asignacion,
                cargadores.planEstudioCargador,
                cargadores.programaCargador,
                cargadores.facultadCargador,
                cargadores.materiaCargador);
    }

    private List<PlanEstudioMateriaDto> hidratarYmapearLista(
            List<PlanEstudioMateria> asignaciones,
            PlanEstudio planEstudioConocido
    ) {
        return hidratarYmapearLista(asignaciones, planEstudioConocido, null);
    }

    private List<PlanEstudioMateriaDto> hidratarYmapearLista(
            List<PlanEstudioMateria> asignaciones,
            PlanEstudio planEstudioConocido,
            Materia materiaConocida
    ) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            return List.of();
        }

        Set<Long> idsPlan = new HashSet<>();
        Set<Integer> idsMateria = new HashSet<>();
        for (PlanEstudioMateria asignacion : asignaciones) {
            idsPlan.add(asignacion.getIdPlanEstudio());
            idsMateria.add(asignacion.getIdMateria());
        }

        PlanEstudioCargador planEstudioCargador = new PlanEstudioCargador(planEstudioRepositorio);
        if (planEstudioConocido != null) {
            planEstudioCargador.registrarEnCache(planEstudioConocido);
        }
        planEstudioCargador.cargarPorIds(new ArrayList<>(idsPlan));

        MateriaCargador materiaCargador = new MateriaCargador(materiaRepositorio);
        if (materiaConocida != null) {
            materiaCargador.registrarEnCache(materiaConocida);
        }
        materiaCargador.cargarPorIds(new ArrayList<>(idsMateria));

        Cargadores cargadores = cargadoresDe(planEstudioCargador, materiaCargador, idsPlan);
        return mapeador.toDtoList(
                asignaciones,
                cargadores.planEstudioCargador,
                cargadores.programaCargador,
                cargadores.facultadCargador,
                cargadores.materiaCargador);
    }

    private Cargadores cargadoresDe(List<PlanEstudio> planes, List<Materia> materias) {
        PlanEstudioCargador planEstudioCargador = new PlanEstudioCargador(planEstudioRepositorio);
        for (PlanEstudio plan : planes) {
            planEstudioCargador.registrarEnCache(plan);
        }
        MateriaCargador materiaCargador = new MateriaCargador(materiaRepositorio);
        for (Materia materia : materias) {
            materiaCargador.registrarEnCache(materia);
        }
        Set<Long> idsPlan = new HashSet<>();
        for (PlanEstudio plan : planes) {
            idsPlan.add(plan.getIdPlanEstudio());
        }
        return cargadoresDe(planEstudioCargador, materiaCargador, idsPlan);
    }

    /**
     * Completa la cadena plan → programa → facultad a partir de los planes ya
     * cargados, para no recorrer los repositorios una vez por asignación.
     */
    private Cargadores cargadoresDe(
            PlanEstudioCargador planEstudioCargador,
            MateriaCargador materiaCargador,
            Set<Long> idsPlan
    ) {
        Set<UUID> idsPrograma = new HashSet<>();
        for (Long idPlan : idsPlan) {
            planEstudioCargador.obtener(idPlan)
                    .ifPresent(plan -> idsPrograma.add(plan.getIdPrograma()));
        }

        ProgramaCargador programaCargador = new ProgramaCargador(programaRepositorio);
        programaCargador.cargarPorIds(new ArrayList<>(idsPrograma));

        Set<Long> idsFacultad = new HashSet<>();
        for (UUID idPrograma : idsPrograma) {
            programaCargador.obtener(idPrograma)
                    .ifPresent(programa -> idsFacultad.add(programa.getIdFacultad()));
        }

        FacultadCargador facultadCargador = new FacultadCargador(facultadRepositorio);
        facultadCargador.cargarPorIds(new ArrayList<>(idsFacultad));

        return new Cargadores(planEstudioCargador, programaCargador, facultadCargador, materiaCargador);
    }

    private static final class Cargadores {

        private final PlanEstudioCargador planEstudioCargador;
        private final ProgramaCargador programaCargador;
        private final FacultadCargador facultadCargador;
        private final MateriaCargador materiaCargador;

        private Cargadores(
                PlanEstudioCargador planEstudioCargador,
                ProgramaCargador programaCargador,
                FacultadCargador facultadCargador,
                MateriaCargador materiaCargador
        ) {
            this.planEstudioCargador = planEstudioCargador;
            this.programaCargador = programaCargador;
            this.facultadCargador = facultadCargador;
            this.materiaCargador = materiaCargador;
        }
    }
}
