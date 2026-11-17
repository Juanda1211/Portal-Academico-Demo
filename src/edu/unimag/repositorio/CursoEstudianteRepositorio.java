package edu.unimag.repositorio;

import edu.unimag.modelo.CursoEstudiante;
import edu.unimag.modelo.pk.CursoEstudiantePk;
import edu.unimag.repositorio.api.RepositorioBase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de inscripciones de estudiantes.
 *
 * Solo expone filtros resolubles con los datos que la fila almacena. La clave
 * compuesta hidrata el {@code CursoPeriodo} de forma superficial (únicamente su
 * {@code @TpaId}), así que las consultas por materia o por período académico se
 * resuelven en el servicio, cruzando con {@code CursoPeriodoRepositorio}.
 */
public interface CursoEstudianteRepositorio extends RepositorioBase<CursoEstudiante, CursoEstudiantePk> {

    boolean insertarSiNoExiste(CursoEstudiante cursoEstudiante);

    Optional<CursoEstudiante> buscarPorClave(CursoEstudiantePk pk);

    List<CursoEstudiante> listarPorEstudiante(UUID idEstudiante);

    List<CursoEstudiante> listarPorCursoPeriodo(UUID idCursoPeriodo);
}
