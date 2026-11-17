package edu.unimag.config.modulos;

import com.cleandev.cli.core.SystemModule;
import com.cleandev.tpa.api.TpaRepository;
import com.cleandev.tpa.api.TpaRepositoryFactory;
import edu.unimag.config.RutaPersistencia;
import edu.unimag.config.modulos.api.ModuloConfigurable;
import edu.unimag.mapeador.CursoEstudianteMapeador;
import edu.unimag.mapeador.CursoPeriodoMapeador;
import edu.unimag.mapeador.EstudianteMapeador;
import edu.unimag.mapeador.MateriaMapeador;
import edu.unimag.mapeador.PeriodoAcademicoMapeador;
import edu.unimag.modelo.CursoEstudiante;
import edu.unimag.modelo.pk.CursoEstudiantePk;
import edu.unimag.persistencia.CursoEstudianteRepositorioImpl;
import edu.unimag.repositorio.CursoEstudianteRepositorio;
import edu.unimag.repositorio.CursoPeriodoRepositorio;
import edu.unimag.repositorio.EstudianteRepositorio;
import edu.unimag.repositorio.MateriaRepositorio;
import edu.unimag.repositorio.PeriodoAcademicoRepositorio;
import edu.unimag.servicio.CursoEstudianteServicio;

public class ConfiguracionModuloCursoEstudiante implements ModuloConfigurable {

    private final CursoEstudianteRepositorio cursoEstudianteRepositorio;
    private final CursoEstudianteServicio cursoEstudianteServicio;
    private final EstudianteRepositorio estudianteRepositorio;
    private final CursoPeriodoRepositorio cursoPeriodoRepositorio;
    private final MateriaRepositorio materiaRepositorio;
    private final PeriodoAcademicoRepositorio periodoAcademicoRepositorio;

    public ConfiguracionModuloCursoEstudiante(
            EstudianteRepositorio estudianteRepositorio,
            CursoPeriodoRepositorio cursoPeriodoRepositorio,
            MateriaRepositorio materiaRepositorio,
            PeriodoAcademicoRepositorio periodoAcademicoRepositorio
    ) {
        if (estudianteRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de estudiantes no puede ser nulo");
        }
        if (cursoPeriodoRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de cursos periodo no puede ser nulo");
        }
        if (materiaRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de materias no puede ser nulo");
        }
        if (periodoAcademicoRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de periodos academicos no puede ser nulo");
        }

        this.estudianteRepositorio = estudianteRepositorio;
        this.cursoPeriodoRepositorio = cursoPeriodoRepositorio;
        this.materiaRepositorio = materiaRepositorio;
        this.periodoAcademicoRepositorio = periodoAcademicoRepositorio;

        // La clave es compuesta: no hay UUID autogenerado.
        boolean necesitaUUID = false;
        TpaRepository<CursoEstudiante, CursoEstudiantePk> tpaEngine = TpaRepositoryFactory.create(
                CursoEstudiante.class,
                RutaPersistencia.CURSOS_ESTUDIANTES.obtenerRuta(),
                necesitaUUID
        );
        this.cursoEstudianteRepositorio = new CursoEstudianteRepositorioImpl(tpaEngine);

        CursoEstudianteMapeador mapeador = new CursoEstudianteMapeador(
                new EstudianteMapeador(),
                new CursoPeriodoMapeador(new MateriaMapeador(), new PeriodoAcademicoMapeador()));

        this.cursoEstudianteServicio = new CursoEstudianteServicio(
                this.cursoEstudianteRepositorio,
                this.estudianteRepositorio,
                this.cursoPeriodoRepositorio,
                this.materiaRepositorio,
                this.periodoAcademicoRepositorio,
                mapeador);
    }

    public CursoEstudianteRepositorio getCursoEstudianteRepositorio() {
        return cursoEstudianteRepositorio;
    }

    public CursoEstudianteServicio getCursoEstudianteServicio() {
        return cursoEstudianteServicio;
    }

    @Override
    public SystemModule construirVista() {
        
        return null;
    }

    @Override
    public void cerrarRecursos() {
        if (cursoEstudianteRepositorio != null) {
            cursoEstudianteRepositorio.cerrar();
        }
    }
}
