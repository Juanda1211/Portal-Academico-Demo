package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.PlanEstudioControlador;
import edu.unimag.dto.planestudio.PlanEstudioActualizarDto;
import edu.unimag.dto.planestudio.PlanEstudioCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.List;
import java.util.Map;


//Rutas de PlanEstudio (el id es Long; el programa es un UUID):


public class PlanEstudioRuta extends RutaBase {

    private final PlanEstudioControlador controlador;

    public PlanEstudioRuta(PlanEstudioControlador controlador) {
        super("/api/planes-estudio");
        if (controlador == null) {
            throw new IllegalArgumentException("El controlador no puede ser nulo");
        }
        this.controlador = controlador;
    }

    @Override
    protected void manejar(HttpExchange ex, String metodo, List<String> partes) throws IOException {
        if (partes.isEmpty()) {
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.listarTodos());
            } else if (metodo.equals("POST")) {
                PlanEstudioCrearDto dto = Json.aRecord(PlanEstudioCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.registrar(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }

        // /api/planes-estudio/programa/{idPrograma}
        if (partes.size() == 2 && partes.get(0).equals("programa")) {
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.listarPorPrograma(idUuid(partes.get(1))));
            } else {
                noPermitido(ex);
            }
            return;
        }

        Long id = idLong(partes.get(0));

        if (partes.size() == 1) {
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.buscarPorId(id));
            } else if (metodo.equals("PUT")) {
                Map<String, Object> datos = leerCuerpoConId(ex, id);
                responder(ex, 200, controlador.actualizar(Json.aRecord(PlanEstudioActualizarDto.class, datos)));
            } else {
                noPermitido(ex);
            }
            return;
        }
        if (partes.size() == 2) {
            if (!metodo.equals("PUT")) {
                noPermitido(ex);
            } else if (partes.get(1).equals("activar")) {
                responder(ex, 200, controlador.activar(id));
            } else if (partes.get(1).equals("inactivar")) {
                responder(ex, 200, controlador.inactivar(id));
            } else {
                noEncontrado(ex);
            }
            return;
        }
        noEncontrado(ex);
    }
}
