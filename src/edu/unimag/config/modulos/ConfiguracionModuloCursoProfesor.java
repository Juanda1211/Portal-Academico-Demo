package edu.unimag.config.modulos;

import com.cleandev.cli.core.SystemModule;
import com.cleandev.tpa.api.TpaRepository;
import com.cleandev.tpa.api.TpaRepositoryFactory;
import edu.unimag.config.RutaPersistencia;
import edu.unimag.config.modulos.api.ModuloConfigurable;
import edu.unimag.modelo.CursoProfesor;
import edu.unimag.modelo.pk.CursoProfesorPk;
import edu.unimag.persistencia.CursoProfesorRepositorioImpl;
import edu.unimag.repositorio.CursoPeriodoRepositorio;
import edu.unimag.repositorio.CursoProfesorRepositorio;
import edu.unimag.repositorio.MateriaRepositorio;
import edu.unimag.repositorio.PeriodoAcademicoRepositorio;
import edu.unimag.repositorio.ProfesorRepositorio;
import edu.unimag.servicio.CursoPeriodoServicio;
import edu.unimag.servicio.ProfesorServicio;

public class ConfiguracionModuloCursoProfesor implements ModuloConfigurable {

    private final CursoProfesorRepositorio cursoProfesorRepositorio;
    private final ProfesorRepositorio profesorRepositorio;
    private final ProfesorServicio profesorServicio;
    private final CursoPeriodoRepositorio cursoPeriodoRepositorio;
    private final CursoPeriodoServicio cursoPeriodoServicio;
    private final MateriaRepositorio materiaRepositorio;
    private final PeriodoAcademicoRepositorio periodoAcademicoRepositorio;

    public ConfiguracionModuloCursoProfesor(
            ProfesorRepositorio profesorRepositorio,
            ProfesorServicio profesorServicio,
            CursoPeriodoRepositorio cursoPeriodoRepositorio,
            CursoPeriodoServicio cursoPeriodoServicio,
            MateriaRepositorio materiaRepositorio,
            PeriodoAcademicoRepositorio periodoAcademicoRepositorio
    ) {

        if (profesorRepositorio == null) {
            throw new IllegalArgumentException("Repositorio de profesor no puede ser nulo");
        }
        if (profesorServicio == null) {
            throw new IllegalArgumentException("Servicio de profesor no puede ser nulo");
        }
        if (cursoPeriodoRepositorio == null) {
            throw new IllegalArgumentException("Repositorio de curso periodo no puede ser nulo");
        }
        if (cursoPeriodoServicio == null) {
            throw new IllegalArgumentException("Servicio de curso periodo no puede ser nulo");
        }
        if (materiaRepositorio == null) {
            throw new IllegalArgumentException("Repositorio de materia no puede ser nulo");
        }
        if (periodoAcademicoRepositorio == null) {
            throw new IllegalArgumentException("Repositorio de periodo academico no puede ser nulo");
        }
        this.profesorRepositorio = profesorRepositorio;
        this.profesorServicio = profesorServicio;
        this.cursoPeriodoRepositorio = cursoPeriodoRepositorio;
        this.cursoPeriodoServicio = cursoPeriodoServicio;
        this.materiaRepositorio = materiaRepositorio;
        this.periodoAcademicoRepositorio = periodoAcademicoRepositorio;

        boolean necesitaUUID = false;
        TpaRepository<CursoProfesor, CursoProfesorPk> tpaEngine = TpaRepositoryFactory.create(
                CursoProfesor.class, RutaPersistencia.CURSOS_PROFESORES.obtenerRuta(), necesitaUUID);

        cursoProfesorRepositorio = new CursoProfesorRepositorioImpl(tpaEngine);

    }

    @Override
    public SystemModule construirVista() {
        
        return null;
    }

    @Override
    public void cerrarRecursos() {
        if (cursoProfesorRepositorio != null) {
            cursoProfesorRepositorio.cerrar();
        
        }
    }

}
