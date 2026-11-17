package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.ProgramaControlador;
import edu.unimag.dto.programa.ProgramaActualizarDto;
import edu.unimag.dto.programa.ProgramaCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Rutas de programa

public class ProgramaRuta extends RutaBase {

    private final ProgramaControlador controlador;

    public ProgramaRuta(ProgramaControlador controlador) {
        super("/api/programas");
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
                ProgramaCrearDto dto = Json.aRecord(ProgramaCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.registrar(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }

        // /api/programas/facultad/{idFacultad}
        if (partes.size() == 2 && partes.get(0).equals("facultad")) {
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.listarPorFacultad(idLong(partes.get(1))));
            } else {
                noPermitido(ex);
            }
            return;
        }

        UUID id = idUuid(partes.get(0));

        if (partes.size() == 1) {
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.buscarPorId(id));
            } else if (metodo.equals("PUT")) {
                Map<String, Object> datos = leerCuerpoConId(ex, id);
                responder(ex, 200, controlador.actualizar(Json.aRecord(ProgramaActualizarDto.class, datos)));
            } else {
                noPermitido(ex);
            }
            return;
        }
        if (partes.size() == 2) {
            if (!metodo.equals("PUT")) {
                noPermitido(ex);
            } else if (partes.get(1).equals("suspender")) {
                responder(ex, 200, controlador.suspender(id));
            } else if (partes.get(1).equals("reactivar")) {
                responder(ex, 200, controlador.reactivar(id));
            } else {
                noEncontrado(ex);
            }
            return;
        }
        noEncontrado(ex);
    }
}
