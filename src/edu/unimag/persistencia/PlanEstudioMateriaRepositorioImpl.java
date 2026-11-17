package edu.unimag.persistencia;

import com.cleandev.tpa.api.TpaRepository;
import edu.unimag.modelo.PlanEstudioMateria;
import edu.unimag.modelo.pk.PlanEstudioMateriaPk;
import edu.unimag.persistencia.abstracta.RepositorioBaseAbstracto;
import edu.unimag.repositorio.PlanEstudioMateriaRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlanEstudioMateriaRepositorioImpl
        extends RepositorioBaseAbstracto<PlanEstudioMateria, PlanEstudioMateriaPk>
        implements PlanEstudioMateriaRepositorio {

    public PlanEstudioMateriaRepositorioImpl(TpaRepository<PlanEstudioMateria, PlanEstudioMateriaPk> tpaRepository) {
        super(tpaRepository);
    }

    @Override
    public boolean insertarSiNoExiste(PlanEstudioMateria planEstudioMateria) {

        if (planEstudioMateria == null) {
            throw new IllegalArgumentException("La asignacion no puede ser nula");
        }
        if (planEstudioMateria.getIdPlanEstudioMateria() == null) {
            throw new IllegalArgumentException("La PK no puede ser null");
        }
        return tpaRepository.executeAtomic(repo -> {
            PlanEstudioMateriaPk pk = planEstudioMateria.getIdPlanEstudioMateria();
            if (repo.findById(pk).isPresent()) {
                return false;
            }
            repo.save(planEstudioMateria);
            return true;
        });
    }

    @Override
    public Optional<PlanEstudioMateria> buscarPorClave(PlanEstudioMateriaPk pk) {
        if (pk == null) {
            throw new IllegalArgumentException("La PK no puede ser null");
        }
        return tpaRepository.findById(pk);
    }

    @Override
    public List<PlanEstudioMateria> listarPorPlan(Long idPlanEstudio) {
        if (idPlanEstudio == null) {
            throw new IllegalArgumentException("El ID del plan de estudio es null");
        }
        List<PlanEstudioMateria> resultado = new ArrayList<>();
        for (PlanEstudioMateria asignacion : tpaRepository.findAll()) {
            if (idPlanEstudio.equals(asignacion.getIdPlanEstudio())) {
                resultado.add(asignacion);
            }
        }
        return resultado;
    }

    @Override
    public List<PlanEstudioMateria> listarPorMateria(Integer idMateria) {
        if (idMateria == null) {
            throw new IllegalArgumentException("El ID de la materia es null");
        }
        List<PlanEstudioMateria> resultado = new ArrayList<>();
        for (PlanEstudioMateria asignacion : tpaRepository.findAll()) {
            if (idMateria.equals(asignacion.getIdMateria())) {
                resultado.add(asignacion);
            }
        }
        return resultado;
    }

    @Override
    public boolean existeOrdenEnSemestre(Long idPlanEstudio, Integer semestre, Integer orden) {
        return existeOrdenEnSemestreExcepto(idPlanEstudio, semestre, orden, null);
    }

    @Override
    public boolean existeOrdenEnSemestreExcepto(Long idPlanEstudio, Integer semestre, Integer orden,
            PlanEstudioMateriaPk claveExcluida) {

        if (idPlanEstudio == null) {
            throw new IllegalArgumentException("El ID del plan de estudio es null");
        }
        if (semestre == null) {
            throw new IllegalArgumentException("El semestre es null");
        }
        if (orden == null) {
            throw new IllegalArgumentException("El orden es null");
        }

        for (PlanEstudioMateria asignacion : tpaRepository.findAll()) {
            if (!idPlanEstudio.equals(asignacion.getIdPlanEstudio())) {
                continue;
            }
            if (!semestre.equals(asignacion.getSemestrePlanEstudioMateria())) {
                continue;
            }
            if (!orden.equals(asignacion.getOrdenPlanEstudioMateria())) {
                continue;
            }
            if (claveExcluida != null
                    && claveExcluida.getIdPlanEstudio().equals(asignacion.getIdPlanEstudio())
                    && claveExcluida.getIdMateria().equals(asignacion.getIdMateria())) {
                continue;
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean permiteBorradoFisico() {
        return true;
    }
}
