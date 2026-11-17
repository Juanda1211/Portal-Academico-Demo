package edu.unimag.persistencia;

import com.cleandev.tpa.api.TpaRepository;
import edu.unimag.modelo.CursoProfesor;
import edu.unimag.modelo.pk.CursoProfesorPk;
import edu.unimag.persistencia.abstracta.RepositorioBaseAbstracto;
import edu.unimag.repositorio.CursoProfesorRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CursoProfesorRepositorioImpl extends RepositorioBaseAbstracto<CursoProfesor, CursoProfesorPk> implements CursoProfesorRepositorio {

    public CursoProfesorRepositorioImpl(TpaRepository<CursoProfesor, CursoProfesorPk> tpaRepository) {
        super(tpaRepository);
    }

    @Override
    public boolean insertarSiNoExiste(CursoProfesor cursoProfesor) {

        if (cursoProfesor == null) {
            throw new IllegalArgumentException("Asignacion de profesor incorrecta");
        }
        if (cursoProfesor.getIdCursoProfesorPeriodo() == null) {
            throw new IllegalArgumentException("La PK no puede ser null");
        }
        return tpaRepository.executeAtomic(repo -> {
            CursoProfesorPk pk = cursoProfesor.getIdCursoProfesorPeriodo();
            if (repo.findById(pk).isPresent()) {
                return false;
            }
            repo.save(cursoProfesor);
            return true;
        });

    }

    @Override
    public List<CursoProfesor> listarPorCursoPeriodo(UUID idCursoPeriodo) {
        if (idCursoPeriodo == null) {
            throw new IllegalArgumentException("El ID del curso periodo es null");
        }
        List<CursoProfesor> resultado = new ArrayList<>();
        for (CursoProfesor asignacion : tpaRepository.findAll()) {
            if (asignacion.getIdCursoPeriodo().equals(idCursoPeriodo)) {
                resultado.add(asignacion);
            }

        }
        return resultado;
    }

}
