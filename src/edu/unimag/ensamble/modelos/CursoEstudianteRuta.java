package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.CursoEstudianteControlador;
import edu.unimag.dto.cursoestudiante.CursoEstudianteCrearDto;
import edu.unimag.dto.cursoestudiante.CursoEstudianteActualizarDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API REST para las inscripciones de estudiantes a cursos por periodo.
 *
 * La clave compuesta viaja como dos segmentos de ruta:
 * /api/inscripciones/{curso}/{estudiante}/{accion}
 */
public class CursoEstudianteRuta extends RutaBase {

    private final CursoEstudianteControlador controlador;

    public CursoEstudianteRuta(CursoEstudianteControlador controlador) {
        super("/api/inscripciones");
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
                CursoEstudianteCrearDto dto = Json.aRecord(
                        CursoEstudianteCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.inscribir(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }

        if (partes.size() == 2) {
            UUID idCursoPeriodo = idUuid(partes.get(0));
            UUID idEstudiante = idUuid(partes.get(1));
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.listarPorCursoPeriodo(idCursoPeriodo));
            } else {
                noPermitido(ex);
            }
            return;
        }

        if (partes.size() == 3) {
            UUID idCursoPeriodo = idUuid(partes.get(0));
            UUID idEstudiante = idUuid(partes.get(1));
            String accion = partes.get(2);

            if (accion.equals("admitir") && metodo.equals("POST")) {
                responder(ex, 200, controlador.admitir(idCursoPeriodo, idEstudiante));
            } else if (accion.equals("retirar") && metodo.equals("POST")) {
                responder(ex, 200, controlador.retirar(idCursoPeriodo, idEstudiante));
            } else if (accion.equals("rechazar") && metodo.equals("POST")) {
                responder(ex, 200, controlador.rechazar(idCursoPeriodo, idEstudiante));
            } else if (accion.equals("evaluar") && metodo.equals("PUT")) {
                Map<String, Object> datos = Json.leerObjeto(leerCuerpo(ex));
                datos.put("idCursoPeriodo", idCursoPeriodo.toString());
                datos.put("idEstudiante", idEstudiante.toString());
                CursoEstudianteActualizarDto dto = Json.aRecord(CursoEstudianteActualizarDto.class, datos);
                responder(ex, 200, controlador.evaluar(dto));
            } else {
                noEncontrado(ex);
            }
            return;
        }

        noEncontrado(ex);
    }

    private void manejarListado(HttpExchange ex) throws IOException {
        Map<String, String> q = query(ex.getRequestURI().getRawQuery());
        String estudiante = q.get("estudiante");
        String curso = q.get("curso");
        String materia = q.get("materia");
        String periodo = q.get("periodo");

        if (estudiante != null) {
            responder(ex, 200, controlador.listarPorEstudiante(idUuid(estudiante)));
        } else if (curso != null) {
            responder(ex, 200, controlador.listarPorCursoPeriodo(idUuid(curso)));
        } else if (materia != null && periodo != null) {
            responder(ex, 200, controlador.listarPorMateriaPeriodo(idEntero(materia), idEntero(periodo)));
        } else if (materia != null) {
            responder(ex, 200, controlador.listarPorMateria(idEntero(materia)));
        } else if (periodo != null) {
            responder(ex, 200, controlador.listarPorPeriodo(idEntero(periodo)));
        } else {
            responder(ex, 200, controlador.listarTodas());
        }
    }
}
