package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.MonitoriaControlador;
import edu.unimag.dto.monitoria.MonitoriaCrearDto;
import edu.unimag.dto.monitoria.MonitoriaActualizarDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API REST para las monitorías académicas.
 *
 * La clave compuesta viaja como tres segmentos de ruta:
 * /api/monitorias/{curso}/{estudiante}/{profesor}
 */
public class MonitoriaRuta extends RutaBase {

    private final MonitoriaControlador controlador;

    public MonitoriaRuta(MonitoriaControlador controlador) {
        super("/api/monitorias");
        if (controlador == null) {
            throw new IllegalArgumentException("El controlador no puede ser nulo");
        }
        this.controlador = controlador;
    }

    @Override
    protected void manejar(HttpExchange ex, String metodo, List<String> partes) throws IOException {

        if (partes.isEmpty()) {
            if (metodo.equals("GET")) {
                manejarListado(ex);
            } else if (metodo.equals("POST")) {
                MonitoriaCrearDto dto = Json.aRecord(
                        MonitoriaCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.crear(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }

        if (partes.size() == 3) {
            UUID idCursoPeriodo = idUuid(partes.get(0));
            UUID idEstudiante = idUuid(partes.get(1));
            Long idProfesor = idLong(partes.get(2));

            if (metodo.equals("PUT")) {
                Map<String, Object> datos = Json.leerObjeto(leerCuerpo(ex));
                datos.put("idCursoPeriodo", idCursoPeriodo.toString());
                datos.put("idEstudiante", idEstudiante.toString());
                datos.put("idProfesor", idProfesor);
                MonitoriaActualizarDto dto = Json.aRecord(MonitoriaActualizarDto.class, datos);
                responder(ex, 200, controlador.evaluar(dto));
            } else if (metodo.equals("DELETE")) {
                controlador.eliminar(idCursoPeriodo, idEstudiante, idProfesor);
                responder(ex, 200, Map.of("mensaje", "Monitoria eliminada"));
            } else {
                noPermitido(ex);
            }
            return;
        }

        noEncontrado(ex);
    }

    private void manejarListado(HttpExchange ex) throws IOException {
        Map<String, String> q = query(ex.getRequestURI().getRawQuery());
        String curso = q.get("curso");
        String estudiante = q.get("estudiante");
        String profesor = q.get("profesor");

        if (curso != null) {
            responder(ex, 200, controlador.listarPorCursoPeriodo(idUuid(curso)));
        } else if (estudiante != null) {
            responder(ex, 200, controlador.listarPorEstudiante(idUuid(estudiante)));
        } else if (profesor != null) {
            responder(ex, 200, controlador.listarPorProfesor(idLong(profesor)));
        } else {
            responder(ex, 200, controlador.listar());
        }
    }
}
