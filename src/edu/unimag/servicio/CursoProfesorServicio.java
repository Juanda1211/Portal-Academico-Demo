package edu.unimag.servicio;

import edu.unimag.dto.cursoprofesor.CursoProfesorActualizarDto;
import edu.unimag.dto.cursoprofesor.CursoProfesorCrearDto;
import edu.unimag.dto.cursoprofesor.CursoProfesorDto;
import edu.unimag.mapeador.CursoProfesorMapeador;
import edu.unimag.modelo.CursoPeriodo;
import edu.unimag.modelo.CursoProfesor;
import edu.unimag.modelo.Materia;
import edu.unimag.modelo.PeriodoAcademico;
import edu.unimag.modelo.Profesor;
import edu.unimag.modelo.pk.CursoProfesorPk;
import edu.unimag.repositorio.CursoPeriodoRepositorio;
import edu.unimag.repositorio.CursoProfesorRepositorio;
import edu.unimag.repositorio.MateriaRepositorio;
import edu.unimag.repositorio.PeriodoAcademicoRepositorio;
import edu.unimag.repositorio.ProfesorRepositorio;
import edu.unimag.repositorio.cargador.CursoPeriodoCargador;
import edu.unimag.repositorio.cargador.MateriaCargador;
import edu.unimag.repositorio.cargador.PeriodoAcademicoCargador;
import edu.unimag.repositorio.cargador.ProfesorCargador;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CursoProfesorServicio {

    private final CursoProfesorRepositorio cursoProfesorRepositorio;
    private final ProfesorRepositorio profesorRepositorio;
    private final CursoPeriodoRepositorio cursoPeriodoRepositorio;
    private final MateriaRepositorio materiaRepositorio;
    private final PeriodoAcademicoRepositorio periodoAcademicoRepositorio;
    private final CursoProfesorMapeador mapeador;

    public CursoProfesorServicio(
            CursoProfesorRepositorio cursoProfesorRepositorio,
            ProfesorRepositorio profesorRepositorio,
            CursoPeriodoRepositorio cursoPeriodoRepositorio,
            MateriaRepositorio materiaRepositorio,
            PeriodoAcademicoRepositorio periodoAcademicoRepositorio,
            CursoProfesorMapeador mapeador
    ) {
        if (cursoProfesorRepositorio == null) {
            throw new IllegalArgumentException("Repo para asignar profesores, no puede ser nulo");
        }
        if (profesorRepositorio == null) {
            throw new IllegalArgumentException("Repo para profesores, no puede ser nulo");
        }

        if (cursoPeriodoRepositorio == null) {
            throw new IllegalArgumentException("Repo para asignar curso Periodo, no puede ser nulo");
        }
        if (materiaRepositorio == null) {
            throw new IllegalArgumentException("Repo para materias, no puede ser nulo");
        }

        if (periodoAcademicoRepositorio == null) {
            throw new IllegalArgumentException("Repo para  Periodo Academico, no puede ser nulo");
        }

        if (mapeador == null) {
            throw new IllegalArgumentException("Mapeador, no puede ser nulo");
        }
        this.cursoProfesorRepositorio = cursoProfesorRepositorio;
        this.profesorRepositorio = profesorRepositorio;
        this.cursoPeriodoRepositorio = cursoPeriodoRepositorio;
        this.materiaRepositorio = materiaRepositorio;
        this.periodoAcademicoRepositorio = periodoAcademicoRepositorio;
        this.mapeador = mapeador;
    }

    //Metodos auxiliares de hidratacion
    //**********************************************************************
    private List<CursoProfesorDto> hidratarYmapearListaGenerico(
            List<CursoProfesor> asignaciones,
            MateriaCargador materiaCargadorInicial,
            PeriodoAcademicoCargador periodoCargadorInicial
    ) {
        Set<Long> idsProfesor = new HashSet<>();
        Set<UUID> idsCursoPeriodo = new HashSet<>();

        for (CursoProfesor asignacion : asignaciones) {
            idsProfesor.add(asignacion.getidProfesor());
            idsCursoPeriodo.add(asignacion.getIdCursoPeriodo());
        }
        ProfesorCargador profesorCargador = new ProfesorCargador(profesorRepositorio);
        profesorCargador.cargarPorIds(new ArrayList<>(idsProfesor));

        CursoPeriodoCargador cursoPeriodoCargador = new CursoPeriodoCargador(cursoPeriodoRepositorio);
        cursoPeriodoCargador.cargarPorIds(new ArrayList<>(idsCursoPeriodo));

        Set<Integer> idsMateria = new HashSet<>();
        Set<Integer> idsPeriodo = new HashSet<>();

        for (UUID idCursoPeriodo : idsCursoPeriodo) {
            cursoPeriodoCargador.obtener(idCursoPeriodo).ifPresent(
                    curso -> {
                        idsMateria.add(curso.getIdMateria());
                        idsPeriodo.add(curso.getIdPeriodoAcademico());

                    }
            );

        }

        MateriaCargador materiaCargador = materiaCargadorInicial != null
                ? materiaCargadorInicial : new MateriaCargador(materiaRepositorio);
        materiaCargador.cargarPorIds(new ArrayList<>(idsMateria));

        PeriodoAcademicoCargador periodoCargador = periodoCargadorInicial != null
                ? periodoCargadorInicial : new PeriodoAcademicoCargador(periodoAcademicoRepositorio);
        periodoCargador.cargarPorIds(new ArrayList<>(idsPeriodo));

        return mapeador.toDtoList(
                asignaciones,
                profesorCargador,
                cursoPeriodoCargador,
                materiaCargador,
                periodoCargador);
    }

    private List<CursoProfesorDto> hidratarYmapearConMateria(List<CursoProfesor> asignaciones, Materia materia) {
        MateriaCargador materiaCargador = new MateriaCargador(materiaRepositorio);
        materiaCargador.registrarEnCache(materia);
        return hidratarYmapearListaGenerico(asignaciones, materiaCargador, null);

    }

    private List<CursoProfesorDto> hidratarYmapearConPeriodo(List<CursoProfesor> asignaciones, PeriodoAcademico periodo) {
        PeriodoAcademicoCargador periodoCargador = new PeriodoAcademicoCargador(periodoAcademicoRepositorio);
        periodoCargador.registrarEnCache(periodo);
        return hidratarYmapearListaGenerico(asignaciones, null, periodoCargador);
    }

    private List<CursoProfesorDto> hidratarYmapearListaConMateriaYperiodo(List<CursoProfesor> asignaciones, Materia materia, PeriodoAcademico periodo) {
        MateriaCargador materiaCargador = new MateriaCargador(materiaRepositorio);
        materiaCargador.registrarEnCache(materia);

        PeriodoAcademicoCargador periodoCargador = new PeriodoAcademicoCargador(periodoAcademicoRepositorio);
        periodoCargador.registrarEnCache(periodo);

        return hidratarYmapearListaGenerico(asignaciones, materiaCargador, periodoCargador);

    }

    private CursoProfesorDto hidratarYmapearUnico(
            CursoProfesor asignacion,
            Profesor profesor,
            CursoPeriodo cursoPeriodo
    ) {
        Materia materia = materiaRepositorio.buscarPorId(cursoPeriodo.getIdMateria()).orElseThrow(
                () -> new IllegalStateException("Materia incorrecta")
        );

        PeriodoAcademico periodo = periodoAcademicoRepositorio.buscarPorId(cursoPeriodo.getIdPeriodoAcademico()).orElseThrow(
                () -> new IllegalStateException("Periodo incorrecto")
        );

        //AQUI
        ProfesorCargador profesorCargador = new ProfesorCargador(profesorRepositorio);
        profesorCargador.registrarEnCache(profesor);

        CursoPeriodoCargador cursoPeriodoCargador = new CursoPeriodoCargador(cursoPeriodoRepositorio);
        cursoPeriodoCargador.registrarEnCache(cursoPeriodo);

        MateriaCargador materiaCargador = new MateriaCargador(materiaRepositorio);
        materiaCargador.registrarEnCache(materia);

        PeriodoAcademicoCargador periodoCargador = new PeriodoAcademicoCargador(periodoAcademicoRepositorio);
        periodoCargador.registrarEnCache(periodo);

        return mapeador.toDto(asignacion, profesorCargador, cursoPeriodoCargador, materiaCargador, periodoCargador);

    }

    private List<CursoProfesorDto> hidratarYmapearLista(List<CursoProfesor> asignaciones, Profesor profesor) {
        ProfesorCargador profesorCargador = new ProfesorCargador(profesorRepositorio);
        profesorCargador.registrarEnCache(profesor);

        Set<UUID> idsCursoPeriodo = new HashSet<>();
        for (CursoProfesor asignacion : asignaciones) {
            idsCursoPeriodo.add(asignacion.getIdCursoPeriodo());
        }

        CursoPeriodoCargador cursoPeriodoCargador = new CursoPeriodoCargador(cursoPeriodoRepositorio);
        cursoPeriodoCargador.cargarPorIds(new ArrayList<>(idsCursoPeriodo));

        Set<Integer> idsMateria = new HashSet<>();
        Set<Integer> idsPeriodo = new HashSet<>();
        for (UUID idCursoPeriodo : idsCursoPeriodo) {
            cursoPeriodoCargador.obtener(idCursoPeriodo).ifPresent(
                    curso -> {
                        idsMateria.add(curso.getIdMateria());
                        idsPeriodo.add(curso.getIdPeriodoAcademico());

                    }
            );
        }
        MateriaCargador materiaCargador = new MateriaCargador(materiaRepositorio);
        materiaCargador.cargarPorIds(new ArrayList<>(idsMateria));

        PeriodoAcademicoCargador periodoCargador = new PeriodoAcademicoCargador(periodoAcademicoRepositorio);
        periodoCargador.cargarPorIds(new ArrayList<>(idsPeriodo));

        return mapeador.toDtoList(
                asignaciones,
                profesorCargador,
                cursoPeriodoCargador,
                materiaCargador,
                periodoCargador);
    }
    //Logica de negocio

    public CursoProfesorDto asignar(CursoProfesorCrearDto dto) {
        Profesor profesor = profesorRepositorio.buscarPorId(dto.idProfesor()).orElseThrow(
                () -> new IllegalArgumentException("Profe no encontrado")
        );

        CursoPeriodo cursoPeriodo = cursoPeriodoRepositorio.buscarPorId(dto.idCursoPeriodo()).orElseThrow(
                () -> new IllegalArgumentException("no existe el curso periodo")
        );

        CursoProfesorPk pk = new CursoProfesorPk(cursoPeriodo, profesor);
        if (cursoProfesorRepositorio.buscarPorId(pk).isPresent()) {
            throw new IllegalStateException(
                    "El profesor ya está asignado a este curso. Puede modificar el tipo de asignación desde la tabla."
            );
        }
        CursoProfesor nuevaAsignacion = new CursoProfesor(pk, dto.tipoAsignacion());

        boolean insertado = cursoProfesorRepositorio.insertarSiNoExiste(nuevaAsignacion);
        if (!insertado) {
            throw new IllegalStateException(
                    "El profesor ya está asignado a este curso. Puede modificar el tipo de asignación desde la tabla."
            );
        }

        return hidratarYmapearUnico(nuevaAsignacion, profesor, cursoPeriodo);

    }

    public CursoProfesorDto cambiarTipoAsignacion(CursoProfesorActualizarDto dto) {
        CursoPeriodo cursoPeriodo = cursoPeriodoRepositorio.buscarPorId(dto.idCursoPeriodo()).orElseThrow(
                () -> new IllegalArgumentException("no existe ese curso")
        );

        Profesor profesor = profesorRepositorio.buscarPorId(dto.idProfesor()).orElseThrow(
                () -> new IllegalArgumentException("No encuentro el profesor")
        );

        CursoProfesorPk pk = new CursoProfesorPk(cursoPeriodo, profesor);
        CursoProfesor asignacion = cursoProfesorRepositorio.buscarPorId(pk).orElseThrow(
                () -> new IllegalArgumentException("No existe la asignacion")
        );

        asignacion.cambiarTipoAsignacion(dto.tipoAsignacion());
        CursoProfesor actualizado = cursoProfesorRepositorio.actualizar(asignacion);

        return hidratarYmapearUnico(actualizado, profesor, cursoPeriodo);

    }

    public CursoProfesorDto retirar(UUID idCursoPeriodo, Long idProfesor) {
        CursoPeriodo cursoPeriodo = cursoPeriodoRepositorio.buscarPorId(idCursoPeriodo).orElseThrow(
                () -> new IllegalArgumentException("")
        );

        Profesor profesor = profesorRepositorio.buscarPorId(idProfesor).orElseThrow(
                () -> new IllegalArgumentException("No se encuentra el profesor")
        );

        CursoProfesorPk pk = new CursoProfesorPk(cursoPeriodo, profesor);
        CursoProfesor asignacion = cursoProfesorRepositorio.buscarPorId(pk).orElseThrow(
                () -> new IllegalArgumentException("No existe la asignacion a retirar")
        );

        asignacion.retirar();
        CursoProfesor actualizado = cursoProfesorRepositorio.actualizar(asignacion);

        return hidratarYmapearUnico(actualizado, profesor, cursoPeriodo);

    }

    public CursoProfesorDto reactivar(UUID idCursoPeriodo, Long idProfesor) {
        CursoPeriodo cursoPeriodo = cursoPeriodoRepositorio.buscarPorId(idCursoPeriodo).orElseThrow(
                () -> new IllegalArgumentException("")
        );

        Profesor profesor = profesorRepositorio.buscarPorId(idProfesor).orElseThrow(
                () -> new IllegalArgumentException("No se encuentra el profesor")
        );

        CursoProfesorPk pk = new CursoProfesorPk(cursoPeriodo, profesor);
        CursoProfesor asignacion = cursoProfesorRepositorio.buscarPorId(pk).orElseThrow(
                () -> new IllegalArgumentException("No existe la asignacion a retirar")
        );
        asignacion.reactivar();
        CursoProfesor actualizado = cursoProfesorRepositorio.actualizar(asignacion);

        return hidratarYmapearUnico(actualizado, profesor, cursoPeriodo);
    }

    public List<CursoProfesorDto> listarPorProfesor(Long idProfesor) {
        Profesor profesor = profesorRepositorio.buscarPorId(idProfesor).orElseThrow(
                () -> new IllegalArgumentException("No existe ese profesor")
        );

        List<CursoProfesor> todas = cursoProfesorRepositorio.listarTodos();
        List<CursoProfesor> delProfesor = new ArrayList<>();

        for (CursoProfesor asignacion : todas) {
            if (asignacion.getidProfesor().equals(idProfesor)) {
                delProfesor.add(asignacion);
            }
        }

        if (delProfesor.isEmpty()) {
            return new ArrayList<>();
        }

        delProfesor.sort(Comparator.comparing(CursoProfesor::getFechaAsignacion).reversed());
        return hidratarYmapearLista(delProfesor, profesor);
    }

    public List<CursoProfesorDto> listarPorMateria(Integer idMateria) {
        Materia materia = materiaRepositorio.buscarPorId(idMateria).orElseThrow(
                () -> new IllegalArgumentException("no existe la materia")
        );

        Set<UUID> idsCursoValidos = new HashSet<>();
        for (CursoPeriodo cp : cursoPeriodoRepositorio.listarTodos()) {
            if (cp.getIdMateria().equals(idMateria)) {
                idsCursoValidos.add(cp.getIdCursoPeriodo());
            }
        }

        List<CursoProfesor> deLaMateria = new ArrayList<>();
        for (CursoProfesor asignacion : cursoProfesorRepositorio.listarTodos()) {
            if (idsCursoValidos.contains(asignacion.getIdCursoPeriodo())) {
                deLaMateria.add(asignacion);
            }
        }

        if (deLaMateria.isEmpty()) {
            return List.of();
        }

        deLaMateria.sort(Comparator.comparing(CursoProfesor::getFechaAsignacion).reversed());
        return hidratarYmapearConMateria(deLaMateria, materia);

    }

    public List<CursoProfesorDto> listarPorPeriodo(Integer idPeriodo) {
        PeriodoAcademico periodo = periodoAcademicoRepositorio.buscarPorId(idPeriodo).orElseThrow(
                () -> new IllegalArgumentException("no existe el periodo" + idPeriodo)
        );

        Set<UUID> idsCursosValidos = new HashSet<>();
        for (CursoPeriodo cp : cursoPeriodoRepositorio.listarTodos()) {
            if (cp.getIdPeriodoAcademico().equals(idPeriodo)) {
                idsCursosValidos.add(cp.getIdCursoPeriodo());
            }
        }

        List<CursoProfesor> delPeriodo = new ArrayList<>();
        for (CursoProfesor asignacion : cursoProfesorRepositorio.listarTodos()) {
            if (idsCursosValidos.contains(asignacion.getIdCursoPeriodo())) {
                delPeriodo.add(asignacion);
            }
        }

        if (delPeriodo.isEmpty()) {
            return List.of();
        }
        delPeriodo.sort(Comparator.comparing(CursoProfesor::getFechaAsignacion).reversed());
        return hidratarYmapearConPeriodo(delPeriodo, periodo);
    }

    public List<CursoProfesorDto> listarPorMateriaPeriodo(Integer idMateria, Integer idPeriodo) {
        Materia materia = materiaRepositorio.buscarPorId(idMateria).orElseThrow(
                () -> new IllegalArgumentException("No existe la materia")
        );

        PeriodoAcademico periodo = periodoAcademicoRepositorio.buscarPorId(idPeriodo).orElseThrow(
                () -> new IllegalArgumentException("No encuentro el periodo")
        );

        Set<UUID> idsCursosValidos = new HashSet<>();
        for (CursoPeriodo cp : cursoPeriodoRepositorio.listarTodos()) {
            if (cp.getIdMateria().equals(idMateria) && cp.getIdPeriodoAcademico().equals(idPeriodo)) {
                idsCursosValidos.add(cp.getIdCursoPeriodo());
            }
        }
        List<CursoProfesor> filtradas = new ArrayList<>();
        for (CursoProfesor asignacion : cursoProfesorRepositorio.listarTodos()) {
            if (idsCursosValidos.contains(asignacion.getIdCursoPeriodo())) {
                filtradas.add(asignacion);
            }
        }
        if (filtradas.isEmpty()) {
            return List.of();
        }
        filtradas.sort(Comparator.comparing(CursoProfesor::getFechaAsignacion).reversed());
        return hidratarYmapearListaConMateriaYperiodo(filtradas, materia, periodo);
    }

}
