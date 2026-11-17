package edu.unimag.persistencia;

import com.cleandev.tpa.api.TpaRepository;
import edu.unimag.modelo.Monitoria;
import edu.unimag.modelo.pk.MonitoriaPk;
import edu.unimag.persistencia.abstracta.RepositorioBaseAbstracto;
import edu.unimag.repositorio.MonitoriaRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MonitoriaRepositorioImpl
        extends RepositorioBaseAbstracto<Monitoria, MonitoriaPk>
        implements MonitoriaRepositorio {

    public MonitoriaRepositorioImpl(TpaRepository<Monitoria, MonitoriaPk> tpaRepository) {
        super(tpaRepository);
    }

    @Override
    public boolean insertarSiNoExiste(Monitoria monitoria) {

        if (monitoria == null) {
            throw new IllegalArgumentException("La monitoria no puede ser nula");
        }
        if (monitoria.getIdMonitoria() == null) {
            throw new IllegalArgumentException("La PK no puede ser null");
        }
        return tpaRepository.executeAtomic(repo -> {
            MonitoriaPk pk = monitoria.getIdMonitoria();
            if (repo.findById(pk).isPresent()) {
                return false;
            }
            repo.save(monitoria);
            return true;
        });
    }

    @Override
    public Optional<Monitoria> buscarPorClave(MonitoriaPk pk) {
        if (pk == null) {
            throw new IllegalArgumentException("La PK no puede ser null");
        }
        return tpaRepository.findById(pk);
    }

    @Override
    public List<Monitoria> listarPorCursoPeriodo(UUID idCursoPeriodo) {
        if (idCursoPeriodo == null) {
            throw new IllegalArgumentException("El ID del curso periodo es null");
        }
        List<Monitoria> resultado = new ArrayList<>();
        for (Monitoria monitoria : tpaRepository.findAll()) {
            if (idCursoPeriodo.equals(monitoria.getIdCursoPeriodo())) {
                resultado.add(monitoria);
            }
        }
        return resultado;
    }

    @Override
    public List<Monitoria> listarPorEstudiante(UUID idEstudiante) {
        if (idEstudiante == null) {
            throw new IllegalArgumentException("El ID del estudiante es null");
        }
        List<Monitoria> resultado = new ArrayList<>();
        for (Monitoria monitoria : tpaRepository.findAll()) {
            if (idEstudiante.equals(monitoria.getIdEstudiante())) {
                resultado.add(monitoria);
            }
        }
        return resultado;
    }

    @Override
    public List<Monitoria> listarPorProfesor(Long idProfesor) {
        if (idProfesor == null) {
            throw new IllegalArgumentException("El ID del profesor es null");
        }
        List<Monitoria> resultado = new ArrayList<>();
        for (Monitoria monitoria : tpaRepository.findAll()) {
            if (idProfesor.equals(monitoria.getIdProfesor())) {
                resultado.add(monitoria);
            }
        }
        return resultado;
    }

    @Override
    protected boolean permiteBorradoFisico() {
        return true;
    }
}
