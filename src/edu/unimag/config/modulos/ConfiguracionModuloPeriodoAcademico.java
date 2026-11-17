package edu.unimag.config.modulos;

import edu.unimag.config.modulos.api.ModuloConfigurable;
import com.cleandev.cli.core.SystemModule;
import com.cleandev.tpa.api.TpaRepository;
import com.cleandev.tpa.api.TpaRepositoryFactory;
import edu.unimag.config.RutaPersistencia;
import edu.unimag.mapeador.PeriodoAcademicoMapeador;
import edu.unimag.modelo.PeriodoAcademico;
import edu.unimag.persistencia.PeriodoAcademicoRepositorioImpl;
import edu.unimag.repositorio.PeriodoAcademicoRepositorio;
import edu.unimag.servicio.PeriodoAcademicoServicio;

public class ConfiguracionModuloPeriodoAcademico implements ModuloConfigurable {

    private final PeriodoAcademicoRepositorio periodoAcademicoRepositorio;
    private final PeriodoAcademicoServicio periodoAcademicoServicio;

    public ConfiguracionModuloPeriodoAcademico() {
        boolean necesitaUUID = false;
        TpaRepository<PeriodoAcademico, Integer> tpaEngine = TpaRepositoryFactory.create(
                PeriodoAcademico.class,
                RutaPersistencia.PERIODOS_ACADEMICOS.obtenerRuta(),
                necesitaUUID
        );
        this.periodoAcademicoRepositorio = new PeriodoAcademicoRepositorioImpl(tpaEngine);

        // Crear el servicio en el constructor para poder exponerlo
        PeriodoAcademicoMapeador mapper = new PeriodoAcademicoMapeador();
        this.periodoAcademicoServicio = new PeriodoAcademicoServicio(
                this.periodoAcademicoRepositorio,
                mapper
        );
    }

    public PeriodoAcademicoRepositorio getPeriodoAcademicoRepositorio() {
        return periodoAcademicoRepositorio;
    }

    public PeriodoAcademicoServicio getPeriodoAcademicoServicio() {
        return periodoAcademicoServicio;
    }

    @Override
    public SystemModule construirVista() {
        
        return null;
    }

    @Override
    public void cerrarRecursos() {
        if (periodoAcademicoRepositorio != null) {
            periodoAcademicoRepositorio.cerrar();
        }
    }
}
