package edu.unimag.persistencia;

import com.cleandev.tpa.api.TpaRepository;
import edu.unimag.modelo.CursoEstudiante;
import edu.unimag.modelo.pk.CursoEstudiantePk;
import edu.unimag.persistencia.abstracta.RepositorioBaseAbstracto;
import edu.unimag.repositorio.CursoEstudianteRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CursoEstudianteRepositorioImpl
        extends RepositorioBaseAbstracto<CursoEstudiante, CursoEstudiantePk>
        implements CursoEstudianteRepositorio {

    public CursoEstudianteRepositorioImpl(TpaRepository<CursoEstudiante, CursoEstudiantePk> tpaRepository) {
        super(tpaRepository);
    }

    @Override
    public boolean insertarSiNoExiste(CursoEstudiante cursoEstudiante) {

        if (cursoEstudiante == null) {
            throw new IllegalArgumentException("La inscripcion no puede ser nula");
        }
        if (cursoEstudiante.getIdCursoEstudiante() == null) {
            throw new IllegalArgumentException("La PK no puede ser null");
        }
        return tpaRepository.executeAtomic(repo -> {
            CursoEstudiantePk pk = cursoEstudiante.getIdCursoEstudiante();
            if (repo.findById(pk).isPresent()) {
                return false;
            }
            repo.save(cursoEstudiante);
            return true;
        });
    }

    @Override
    public Optional<CursoEstudiante> buscarPorClave(CursoEstudiantePk pk) {
        if (pk == null) {
            throw new IllegalArgumentException("La PK no puede ser null");
        }
        return tpaRepository.findById(pk);
    }

    @Override
    public List<CursoEstudiante> listarPorEstudiante(UUID idEstudiante) {
        if (idEstudiante == null) {
            throw new IllegalArgumentException("El ID del estudiante es null");
        }
        List<CursoEstudiante> resultado = new ArrayList<>();
        for (CursoEstudiante inscripcion : tpaRepository.findAll()) {
            if (idEstudiante.equals(inscripcion.getIdEstudiante())) {
                resultado.add(inscripcion);
            }
        }
        return resultado;
    }

    @Override
    public List<CursoEstudiante> listarPorCursoPeriodo(UUID idCursoPeriodo) {
        if (idCursoPeriodo == null) {
            throw new IllegalArgumentException("El ID del curso periodo es null");
        }
        List<CursoEstudiante> resultado = new ArrayList<>();
        for (CursoEstudiante inscripcion : tpaRepository.findAll()) {
            if (idCursoPeriodo.equals(inscripcion.getIdCursoPeriodo())) {
                resultado.add(inscripcion);
            }
        }
        return resultado;
    }
}
