package edu.unimag.config.modulos;

import com.cleandev.cli.core.SystemModule;
import com.cleandev.tpa.api.TpaRepository;
import com.cleandev.tpa.api.TpaRepositoryFactory;
import edu.unimag.config.RutaPersistencia;
import edu.unimag.config.modulos.api.ModuloConfigurable;
import edu.unimag.mapeador.CursoPeriodoMapeador;
import edu.unimag.mapeador.EstudianteMapeador;
import edu.unimag.mapeador.MateriaMapeador;
import edu.unimag.mapeador.MonitoriaMapeador;
import edu.unimag.mapeador.PeriodoAcademicoMapeador;
import edu.unimag.mapeador.ProfesorMapeador;
import edu.unimag.modelo.Monitoria;
import edu.unimag.modelo.pk.MonitoriaPk;
import edu.unimag.persistencia.MonitoriaRepositorioImpl;
import edu.unimag.repositorio.CursoEstudianteRepositorio;
import edu.unimag.repositorio.CursoPeriodoRepositorio;
import edu.unimag.repositorio.EstudianteRepositorio;
import edu.unimag.repositorio.MateriaRepositorio;
import edu.unimag.repositorio.MonitoriaRepositorio;
import edu.unimag.repositorio.PeriodoAcademicoRepositorio;
import edu.unimag.repositorio.ProfesorRepositorio;
import edu.unimag.servicio.MonitoriaServicio;

public class ConfiguracionModuloMonitoria implements ModuloConfigurable {

    private final MonitoriaRepositorio monitoriaRepositorio;
    private final MonitoriaServicio monitoriaServicio;

    public ConfiguracionModuloMonitoria(
            CursoPeriodoRepositorio cursoPeriodoRepositorio,
            EstudianteRepositorio estudianteRepositorio,
            ProfesorRepositorio profesorRepositorio,
            CursoEstudianteRepositorio cursoEstudianteRepositorio,
            MateriaRepositorio materiaRepositorio,
            PeriodoAcademicoRepositorio periodoAcademicoRepositorio
    ) {
        if (cursoPeriodoRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de cursos periodo no puede ser nulo");
        }
        if (estudianteRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de estudiantes no puede ser nulo");
        }
        if (profesorRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de profesores no puede ser nulo");
        }
        if (cursoEstudianteRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de inscripciones no puede ser nulo");
        }
        if (materiaRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de materias no puede ser nulo");
        }
        if (periodoAcademicoRepositorio == null) {
            throw new IllegalArgumentException("El repositorio de periodos academicos no puede ser nulo");
        }

        boolean necesitaUUID = false;
        TpaRepository<Monitoria, MonitoriaPk> tpaEngine = TpaRepositoryFactory.create(
                Monitoria.class,
                RutaPersistencia.MONITORIAS.obtenerRuta(),
                necesitaUUID
        );
        this.monitoriaRepositorio = new MonitoriaRepositorioImpl(tpaEngine);

        MonitoriaMapeador mapeador = new MonitoriaMapeador(
                new CursoPeriodoMapeador(new MateriaMapeador(), new PeriodoAcademicoMapeador()),
                new EstudianteMapeador(),
                new ProfesorMapeador());

        this.monitoriaServicio = new MonitoriaServicio(
                this.monitoriaRepositorio,
                cursoPeriodoRepositorio,
                estudianteRepositorio,
                profesorRepositorio,
                cursoEstudianteRepositorio,
                materiaRepositorio,
                periodoAcademicoRepositorio,
                mapeador);
    }

    public MonitoriaRepositorio getMonitoriaRepositorio() {
        return monitoriaRepositorio;
    }

    public MonitoriaServicio getMonitoriaServicio() {
        return monitoriaServicio;
    }

    @Override
    public SystemModule construirVista() {
        
        return null;
    }

    @Override
    public void cerrarRecursos() {
        if (monitoriaRepositorio != null) {
            monitoriaRepositorio.cerrar();
        }
    }
}
