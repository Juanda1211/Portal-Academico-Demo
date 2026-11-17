package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.PlanEstudioMateriaControlador;
import edu.unimag.dto.planestudiomateria.PlanEstudioMateriaActualizarDto;
import edu.unimag.dto.planestudiomateria.PlanEstudioMateriaCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * API REST para las asignaciones de materias a planes de estudio.
 *
 * La clave compuesta viaja como dos segmentos de ruta:
 * /api/plan-materias/{plan}/{materia}
 */
public class PlanEstudioMateriaRuta extends RutaBase {

    private final PlanEstudioMateriaControlador controlador;

    public PlanEstudioMateriaRuta(PlanEstudioMateriaControlador controlador) {
        super("/api/plan-materias");
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
                PlanEstudioMateriaCrearDto dto = Json.aRecord(
                        PlanEstudioMateriaCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.asignar(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }

        if (partes.size() == 2) {
            Long idPlanEstudio = idLong(partes.get(0));
            Integer idMateria = idEntero(partes.get(1));

            if (metodo.equals("PUT")) {
                Map<String, Object> datos = Json.leerObjeto(leerCuerpo(ex));
                datos.put("idPlanEstudio", idPlanEstudio);
                datos.put("idMateria", idMateria);
                PlanEstudioMateriaActualizarDto dto = Json.aRecord(
                        PlanEstudioMateriaActualizarDto.class, datos);
                responder(ex, 200, controlador.actualizar(dto));
            } else if (metodo.equals("DELETE")) {
                controlador.quitar(idPlanEstudio, idMateria);
                responder(ex, 200, Map.of(
                        "mensaje", "Materia quitada del plan de estudio"));
            } else {
                noPermitido(ex);
            }
            return;
        }

        noEncontrado(ex);
    }

    private void manejarListado(HttpExchange ex) throws IOException {
        Map<String, String> q = query(ex.getRequestURI().getRawQuery());
        String plan = q.get("plan");
        String materia = q.get("materia");

        if (plan != null) {
            responder(ex, 200, controlador.listarPorPlan(idLong(plan)));
        } else if (materia != null) {
            responder(ex, 200, controlador.listarPorMateria(idEntero(materia)));
        } else {
            responder(ex, 400, Map.of("error", "Indicar plan o materia."));
        }
    }
}
