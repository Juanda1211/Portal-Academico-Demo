package edu.unimag.controlador;

import edu.unimag.dto.cursoestudiante.CursoEstudianteCrearDto;
import edu.unimag.dto.cursoestudiante.CursoEstudianteDto;
import edu.unimag.dto.cursoestudiante.CursoEstudianteActualizarDto;
import edu.unimag.servicio.CursoEstudianteServicio;
import java.util.List;
import java.util.UUID;

public class CursoEstudianteControlador {

    private final CursoEstudianteServicio cursoEstudianteServicio;

    public CursoEstudianteControlador(CursoEstudianteServicio cursoEstudianteServicio) {
        if (cursoEstudianteServicio == null) {
            throw new IllegalArgumentException("El servicio de inscripciones no puede ser nulo");
        }
        this.cursoEstudianteServicio = cursoEstudianteServicio;
    }

    public CursoEstudianteDto inscribir(CursoEstudianteCrearDto dto) {
        return cursoEstudianteServicio.inscribir(dto);
    }

    public CursoEstudianteDto admitir(UUID idCursoPeriodo, UUID idEstudiante) {
        return cursoEstudianteServicio.admitir(idCursoPeriodo, idEstudiante);
    }

    public CursoEstudianteDto evaluar(CursoEstudianteActualizarDto dto) {
        return cursoEstudianteServicio.evaluar(dto);
    }

    public CursoEstudianteDto retirar(UUID idCursoPeriodo, UUID idEstudiante) {
        return cursoEstudianteServicio.retirar(idCursoPeriodo, idEstudiante);
    }

    public CursoEstudianteDto rechazar(UUID idCursoPeriodo, UUID idEstudiante) {
        return cursoEstudianteServicio.rechazar(idCursoPeriodo, idEstudiante);
    }

    public List<CursoEstudianteDto> listarTodas() {
        return cursoEstudianteServicio.listarTodas();
    }

    public List<CursoEstudianteDto> listarPorEstudiante(UUID idEstudiante) {
        return cursoEstudianteServicio.listarPorEstudiante(idEstudiante);
    }

    public List<CursoEstudianteDto> listarPorCursoPeriodo(UUID idCursoPeriodo) {
        return cursoEstudianteServicio.listarPorCursoPeriodo(idCursoPeriodo);
    }

    public List<CursoEstudianteDto> listarPorMateria(Integer idMateria) {
        return cursoEstudianteServicio.listarPorMateria(idMateria);
    }

    public List<CursoEstudianteDto> listarPorMateriaPeriodo(Integer idMateria, Integer idPeriodo) {
        return cursoEstudianteServicio.listarPorMateriaPeriodo(idMateria, idPeriodo);
    }

    public List<CursoEstudianteDto> listarPorPeriodo(Integer idPeriodo) {
        return cursoEstudianteServicio.listarPorPeriodo(idPeriodo);
    }
}
