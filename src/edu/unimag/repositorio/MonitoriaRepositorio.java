package edu.unimag.repositorio;

import edu.unimag.modelo.Monitoria;
import edu.unimag.modelo.pk.MonitoriaPk;
import edu.unimag.repositorio.api.RepositorioBase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitoriaRepositorio extends RepositorioBase<Monitoria, MonitoriaPk> {

    boolean insertarSiNoExiste(Monitoria monitoria);

    Optional<Monitoria> buscarPorClave(MonitoriaPk pk);

    List<Monitoria> listarPorCursoPeriodo(UUID idCursoPeriodo);

    List<Monitoria> listarPorEstudiante(UUID idEstudiante);

    List<Monitoria> listarPorProfesor(Long idProfesor);
}
