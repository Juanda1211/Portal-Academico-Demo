package edu.unimag;

import com.cleandev.tpa.api.TpaRepository;
import com.cleandev.tpa.api.TpaRepositoryFactory;

import edu.unimag.config.RutaPersistencia;
import edu.unimag.config.modulos.ConfiguracionModuloCursoEstudiante;
import edu.unimag.config.modulos.ConfiguracionModuloCursoPeriodo;
import edu.unimag.config.modulos.ConfiguracionModuloEstudiante;
import edu.unimag.config.modulos.ConfiguracionModuloFacultad;
import edu.unimag.config.modulos.ConfiguracionModuloFormacionProfesor;
import edu.unimag.config.modulos.ConfiguracionModuloMateria;
import edu.unimag.config.modulos.ConfiguracionModuloMonitoria;
import edu.unimag.config.modulos.ConfiguracionModuloPeriodoAcademico;
import edu.unimag.config.modulos.ConfiguracionModuloPlanEstudio;
import edu.unimag.config.modulos.ConfiguracionModuloPlanEstudioMateria;
import edu.unimag.config.modulos.ConfiguracionModuloProfesor;
import edu.unimag.config.modulos.ConfiguracionModuloPrograma;

import edu.unimag.controlador.CursoEstudianteControlador;
import edu.unimag.controlador.CursoPeriodoControlador;
import edu.unimag.controlador.CursoProfesorControlador;
import edu.unimag.controlador.EstudianteControlador;
import edu.unimag.controlador.FacultadControlador;
import edu.unimag.controlador.FormacionProfesorControlador;
import edu.unimag.controlador.MateriaControlador;
import edu.unimag.controlador.MonitoriaControlador;
import edu.unimag.controlador.PeriodoAcademicoControlador;
import edu.unimag.controlador.PlanEstudioControlador;
import edu.unimag.controlador.PlanEstudioMateriaControlador;
import edu.unimag.controlador.ProfesorControlador;
import edu.unimag.controlador.ProgramaControlador;

import edu.unimag.ensamble.ServidorWeb;
import edu.unimag.ensamble.modelos.CursoEstudianteRuta;
import edu.unimag.ensamble.modelos.CursoPeriodoRuta;
import edu.unimag.ensamble.modelos.CursoProfesorRuta;
import edu.unimag.ensamble.modelos.EstudianteRuta;
import edu.unimag.ensamble.modelos.FacultadRuta;
import edu.unimag.ensamble.modelos.FormacionProfesorRuta;
import edu.unimag.ensamble.modelos.MateriaRuta;
import edu.unimag.ensamble.modelos.MonitoriaRuta;
import edu.unimag.ensamble.modelos.PeriodoAcademicoRuta;
import edu.unimag.ensamble.modelos.PlanEstudioMateriaRuta;
import edu.unimag.ensamble.modelos.PlanEstudioRuta;
import edu.unimag.ensamble.modelos.ProfesorRuta;
import edu.unimag.ensamble.modelos.ProgramaRuta;

import edu.unimag.mapeador.CursoPeriodoMapeador;
import edu.unimag.mapeador.CursoProfesorMapeador;
import edu.unimag.mapeador.MateriaMapeador;
import edu.unimag.mapeador.PeriodoAcademicoMapeador;
import edu.unimag.mapeador.ProfesorMapeador;

import edu.unimag.modelo.CursoProfesor;
import edu.unimag.modelo.pk.CursoProfesorPk;

import edu.unimag.persistencia.CursoProfesorRepositorioImpl;
import edu.unimag.repositorio.CursoProfesorRepositorio;
import edu.unimag.servicio.CursoProfesorServicio;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AppWeb {

    private static final int PUERTO_PREDETERMINADO = 8080;

    public static void main(String[] args) {

        List<AutoCloseable> recursos = new ArrayList<>();
        ServidorWeb servidor = null;

        try {

            String puertoEntorno = System.getenv("PORT");
            int puerto = args.length > 0
                    ? Integer.parseInt(args[0])
                    : (puertoEntorno != null && !puertoEntorno.isBlank()
                            ? Integer.parseInt(puertoEntorno.trim())
                            : PUERTO_PREDETERMINADO);

            // NIVEL 1: módulos independientes.
            ConfiguracionModuloProfesor configProfesor = new ConfiguracionModuloProfesor();
            ConfiguracionModuloEstudiante configEstudiante = new ConfiguracionModuloEstudiante();
            ConfiguracionModuloFacultad configFacultad = new ConfiguracionModuloFacultad();
            ConfiguracionModuloMateria configMateria = new ConfiguracionModuloMateria();
            ConfiguracionModuloPeriodoAcademico configPeriodo = new ConfiguracionModuloPeriodoAcademico();

            recursos.add(configProfesor::cerrarRecursos);
            recursos.add(configEstudiante::cerrarRecursos);
            recursos.add(configFacultad::cerrarRecursos);
            recursos.add(configMateria::cerrarRecursos);
            recursos.add(configPeriodo::cerrarRecursos);

            // NIVEL 2: módulos dependientes de entidades base.
            ConfiguracionModuloFormacionProfesor configFormacion =
                    new ConfiguracionModuloFormacionProfesor(configProfesor.getProfesorRepositorio());
            ConfiguracionModuloPrograma configPrograma =
                    new ConfiguracionModuloPrograma(configFacultad.getFacultadRepositorio());

            recursos.add(configFormacion::cerrarRecursos);
            recursos.add(configPrograma::cerrarRecursos);

            // NIVEL 3: curso-periodo.
            ConfiguracionModuloCursoPeriodo configCursoPeriodo = new ConfiguracionModuloCursoPeriodo(
                    configMateria.getMateriaRepositorio(),
                    configPeriodo.getPeriodoAcademicoRepositorio()
            );
            recursos.add(configCursoPeriodo::cerrarRecursos);

            // NIVEL 4: plan de estudio.
            ConfiguracionModuloPlanEstudio configPlanEstudio = new ConfiguracionModuloPlanEstudio(
                    configPrograma.getProgramaRepositorio(),
                    configPrograma.getProgramaServicio(),
                    configFacultad.getFacultadRepositorio()
            );
            recursos.add(configPlanEstudio::cerrarRecursos);

            // NIVEL 5: curso-estudiante (PK compuesta).
            ConfiguracionModuloCursoEstudiante configCursoEstudiante =
                    new ConfiguracionModuloCursoEstudiante(
                            configEstudiante.getEstudianteRepositorio(),
                            configCursoPeriodo.getCursoPeriodoRepositorio(),
                            configMateria.getMateriaRepositorio(),
                            configPeriodo.getPeriodoAcademicoRepositorio()
                    );
            recursos.add(configCursoEstudiante::cerrarRecursos);

            // NIVEL 6: plan-materia y monitoria (PK compuestas).
            ConfiguracionModuloPlanEstudioMateria configPlanEstudioMateria =
                    new ConfiguracionModuloPlanEstudioMateria(
                            configPlanEstudio.getPlanEstudioRepositorio(),
                            configPlanEstudio.getPlanEstudioServicio(),
                            configMateria.getMateriaRepositorio(),
                            configPrograma.getProgramaRepositorio(),
                            configFacultad.getFacultadRepositorio()
                    );
            recursos.add(configPlanEstudioMateria::cerrarRecursos);

            ConfiguracionModuloMonitoria configMonitoria = new ConfiguracionModuloMonitoria(
                    configCursoPeriodo.getCursoPeriodoRepositorio(),
                    configEstudiante.getEstudianteRepositorio(),
                    configProfesor.getProfesorRepositorio(),
                    configCursoEstudiante.getCursoEstudianteRepositorio(),
                    configMateria.getMateriaRepositorio(),
                    configPeriodo.getPeriodoAcademicoRepositorio()
            );
            recursos.add(configMonitoria::cerrarRecursos);

            // CREACION DE CONTROLADORES.
            ProfesorControlador profesorControlador = new ProfesorControlador(configProfesor.getProfesorServicio());
            EstudianteControlador estudianteControlador =
                    new EstudianteControlador(configEstudiante.getEstudianteServicio());
            FacultadControlador facultadControlador = new FacultadControlador(configFacultad.getFacultadServicio());
            MateriaControlador materiaControlador = new MateriaControlador(configMateria.getMateriaServicio());
            PeriodoAcademicoControlador periodoControlador =
                    new PeriodoAcademicoControlador(configPeriodo.getPeriodoAcademicoServicio());
            FormacionProfesorControlador formacionControlador =
                    new FormacionProfesorControlador(configFormacion.getFormacionProfesorServicio());
            ProgramaControlador programaControlador = new ProgramaControlador(configPrograma.getProgramaServicio());
            CursoPeriodoControlador cursoPeriodoControlador =
                    new CursoPeriodoControlador(configCursoPeriodo.getCursoPeriodoServicio());
            PlanEstudioControlador planEstudioControlador = new PlanEstudioControlador(
                    configPlanEstudio.getPlanEstudioServicio(),
                    configPrograma.getProgramaServicio()
            );

            // NIVEL 5: CURSO-PROFESOR.
            // Se construye directamente porque su configuracion no expone el controlador.
            boolean necesitaUUID = false;
            TpaRepository<CursoProfesor, CursoProfesorPk> tpaCursoProfesor = TpaRepositoryFactory.create(
                    CursoProfesor.class,
                    RutaPersistencia.CURSOS_PROFESORES.obtenerRuta(),
                    necesitaUUID
            );
            CursoProfesorRepositorio cursoProfesorRepositorio = new CursoProfesorRepositorioImpl(tpaCursoProfesor);
            recursos.add(cursoProfesorRepositorio::cerrar);

            // MAPEADORES.
            ProfesorMapeador profesorMapeador = new ProfesorMapeador();
            MateriaMapeador materiaMapeador = new MateriaMapeador();
            PeriodoAcademicoMapeador periodoMapeador = new PeriodoAcademicoMapeador();
            CursoPeriodoMapeador cursoPeriodoMapeador = new CursoPeriodoMapeador(materiaMapeador, periodoMapeador);
            CursoProfesorMapeador cursoProfesorMapeador =
                    new CursoProfesorMapeador(profesorMapeador, cursoPeriodoMapeador);

            // SERVICIO CURSO-PROFESOR.
            CursoProfesorServicio cursoProfesorServicio = new CursoProfesorServicio(
                    cursoProfesorRepositorio,
                    configProfesor.getProfesorRepositorio(),
                    configCursoPeriodo.getCursoPeriodoRepositorio(),
                    configMateria.getMateriaRepositorio(),
                    configPeriodo.getPeriodoAcademicoRepositorio(),
                    cursoProfesorMapeador
            );

            // CONTROLADOR CURSO-PROFESOR.
            CursoProfesorControlador cursoProfesorControlador = new CursoProfesorControlador(
                    cursoProfesorServicio,
                    configProfesor.getProfesorServicio(),
                    configCursoPeriodo.getCursoPeriodoServicio()
            );

            // CONTROLADORES DE LOS MODULOS NUEVOS.
            CursoEstudianteControlador cursoEstudianteControlador = new CursoEstudianteControlador(
                    configCursoEstudiante.getCursoEstudianteServicio()
            );
            PlanEstudioMateriaControlador planEstudioMateriaControlador =
                    new PlanEstudioMateriaControlador(
                            configPlanEstudioMateria.getPlanEstudioMateriaServicio()
                    );
            MonitoriaControlador monitoriaControlador = new MonitoriaControlador(
                    configMonitoria.getMonitoriaServicio()
            );

            // CREAR SERVIDOR WEB.
            servidor = new ServidorWeb(puerto, Paths.get(System.getProperty("user.dir"), "web"));

            // REGISTRAR RUTAS.
            servidor.registrar("/api/profesores", new ProfesorRuta(profesorControlador));
            servidor.registrar("/api/estudiantes", new EstudianteRuta(estudianteControlador));
            servidor.registrar("/api/facultades", new FacultadRuta(facultadControlador));
            servidor.registrar("/api/materias", new MateriaRuta(materiaControlador));
            servidor.registrar("/api/periodos", new PeriodoAcademicoRuta(periodoControlador));
            servidor.registrar("/api/formaciones", new FormacionProfesorRuta(formacionControlador));
            servidor.registrar("/api/programas", new ProgramaRuta(programaControlador));
            servidor.registrar("/api/cursos-periodos", new CursoPeriodoRuta(cursoPeriodoControlador));
            servidor.registrar("/api/cursos-profesores", new CursoProfesorRuta(cursoProfesorControlador));
            servidor.registrar("/api/planes-estudio", new PlanEstudioRuta(planEstudioControlador));
            servidor.registrar("/api/inscripciones", new CursoEstudianteRuta(cursoEstudianteControlador));
            servidor.registrar("/api/plan-materias", new PlanEstudioMateriaRuta(planEstudioMateriaControlador));
            servidor.registrar("/api/monitorias", new MonitoriaRuta(monitoriaControlador));

            // CIERRE AUTOMATICO DE RECURSOS.
            final ServidorWeb servidorFinal = servidor;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                servidorFinal.detener();
                cerrarRecursos(recursos);
            }));

            // INICIAR SERVIDOR.
            servidor.iniciar();

        } catch (Exception e) {

            System.err.println("ERROR: No se puede iniciar la web");
            System.err.println(e.getMessage());

            if (servidor != null) {
                servidor.detener();
            }

            cerrarRecursos(recursos);

            System.exit(1);
        }
    }

    // METODO AUXILIAR PARA CERRAR RECURSOS.
    private static void cerrarRecursos(List<AutoCloseable> recursos) {

        for (int i = recursos.size() - 1; i >= 0; i--) {

            try {

                recursos.get(i).close();

            } catch (Exception e) {

                System.err.println("ADVERTENCIA: No se pudo cerrar un recurso: " + e.getMessage());
            }
        }

        recursos.clear();
    }
}
