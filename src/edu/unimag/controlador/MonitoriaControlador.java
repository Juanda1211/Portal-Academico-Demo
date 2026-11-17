package edu.unimag.controlador;

import edu.unimag.dto.monitoria.MonitoriaCrearDto;
import edu.unimag.dto.monitoria.MonitoriaDto;
import edu.unimag.dto.monitoria.MonitoriaActualizarDto;
import edu.unimag.servicio.MonitoriaServicio;
import java.util.List;
import java.util.UUID;

public class MonitoriaControlador {

    private final MonitoriaServicio monitoriaServicio;

    public MonitoriaControlador(MonitoriaServicio monitoriaServicio) {
        if (monitoriaServicio == null) {
            throw new IllegalArgumentException("El servicio de monitorias no puede ser nulo");
        }
        this.monitoriaServicio = monitoriaServicio;
    }

    public MonitoriaDto crear(MonitoriaCrearDto dto) {
        return monitoriaServicio.crear(dto);
    }

    public MonitoriaDto evaluar(MonitoriaActualizarDto dto) {
        return monitoriaServicio.evaluar(dto);
    }

    public boolean eliminar(UUID idCursoPeriodo, UUID idEstudiante, Long idProfesor) {
        return monitoriaServicio.eliminar(idCursoPeriodo, idEstudiante, idProfesor);
    }

    public List<MonitoriaDto> listar() {
        return monitoriaServicio.listar();
    }

    public List<MonitoriaDto> listarPorCursoPeriodo(UUID idCursoPeriodo) {
        return monitoriaServicio.listarPorCursoPeriodo(idCursoPeriodo);
    }

    public List<MonitoriaDto> listarPorEstudiante(UUID idEstudiante) {
        return monitoriaServicio.listarPorEstudiante(idEstudiante);
    }

    public List<MonitoriaDto> listarPorProfesor(Long idProfesor) {
        return monitoriaServicio.listarPorProfesor(idProfesor);
    }
}
