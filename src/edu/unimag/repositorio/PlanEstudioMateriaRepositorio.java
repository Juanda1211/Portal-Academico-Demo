package edu.unimag.repositorio;

import edu.unimag.modelo.PlanEstudioMateria;
import edu.unimag.modelo.pk.PlanEstudioMateriaPk;
import edu.unimag.repositorio.api.RepositorioBase;
import java.util.List;
import java.util.Optional;

public interface PlanEstudioMateriaRepositorio extends RepositorioBase<PlanEstudioMateria, PlanEstudioMateriaPk> {

    boolean insertarSiNoExiste(PlanEstudioMateria planEstudioMateria);

    Optional<PlanEstudioMateria> buscarPorClave(PlanEstudioMateriaPk pk);

    List<PlanEstudioMateria> listarPorPlan(Long idPlanEstudio);

    List<PlanEstudioMateria> listarPorMateria(Integer idMateria);

    boolean existeOrdenEnSemestre(Long idPlanEstudio, Integer semestre, Integer orden);

    boolean existeOrdenEnSemestreExcepto(Long idPlanEstudio, Integer semestre, Integer orden,
            PlanEstudioMateriaPk claveExcluida);
}
