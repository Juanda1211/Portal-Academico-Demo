package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.CursoProfesorControlador;
import edu.unimag.dto.cursoprofesor.CursoProfesorActualizarDto;
import edu.unimag.dto.cursoprofesor.CursoProfesorCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** API REST para las asignaciones entre profesores y cursos por periodo. */
public class CursoProfesorRuta extends RutaBase {
    private final CursoProfesorControlador controlador;

    public CursoProfesorRuta(CursoProfesorControlador controlador) {
        super("/api/cursos-profesores");
        if (controlador == null) throw new IllegalArgumentException("El controlador no puede ser nulo");
        this.controlador = controlador;
    }

    @Override
    protected void manejar(HttpExchange ex, String metodo, List<String> partes) throws IOException {
        if (partes.isEmpty()) {
            if (metodo.equals("GET")) {
                Map<String, String> q = query(ex.getRequestURI().getRawQuery());
                String profesor = q.get("profesor");
                String materia = q.get("materia");
                String periodo = q.get("periodo");
                List<edu.unimag.dto.cursoprofesor.CursoProfesorDto> asignaciones;
                if (profesor != null) {
                    asignaciones = controlador.listarPorProfesor(idLong(profesor));
                } else if (materia != null && periodo != null) {
                    asignaciones = controlador.listarPorMateriaPeriodo(idEntero(materia), idEntero(periodo));
                } else if (materia != null) {
                    asignaciones = controlador.listarPorMateria(idEntero(materia));
                } else if (periodo != null) {
                    asignaciones = controlador.listarPorPeriodo(idEntero(periodo));
                } else {
                    responder(ex, 400, Map.of("error", "indicar profesor, materia o periodo."));
                    return;
                }
                responder(ex, 200, asignaciones);
            } else if (metodo.equals("POST")) {
                CursoProfesorCrearDto dto = Json.aRecord(CursoProfesorCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.asignar(dto));
            } else noPermitido(ex);
            return;
        }

        if (partes.size() == 2) {
            UUID idCursoPeriodo = idUuid(partes.get(0));
            Long idProfesor = idLong(partes.get(1));
            if (metodo.equals("PUT")) {
                Map<String, Object> datos = Json.leerObjeto(leerCuerpo(ex));
                datos.put("idCursoPeriodo", idCursoPeriodo.toString());
                datos.put("idProfesor", idProfesor);
                CursoProfesorActualizarDto dto = Json.aRecord(CursoProfesorActualizarDto.class, datos);
                responder(ex, 200, controlador.cambiarTipoAsignacion(dto));
            } else noPermitido(ex);
            return;
        }

        if (partes.size() == 3 && metodo.equals("POST")) {
            UUID idCursoPeriodo = idUuid(partes.get(0));
            Long idProfesor = idLong(partes.get(1));
            Map<String, Object> respuesta = new LinkedHashMap<>();
            if (partes.get(2).equals("retirar")) responder(ex, 200, controlador.retirar(idCursoPeriodo, idProfesor));
            else if (partes.get(2).equals("reactivar")) responder(ex, 200, controlador.reactivar(idCursoPeriodo, idProfesor));
            else noEncontrado(ex);
            return;
        }
        noEncontrado(ex);
    }
}
