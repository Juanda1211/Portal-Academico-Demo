package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.FacultadControlador;
import edu.unimag.dto.facultad.FacultadActualizarDto;
import edu.unimag.dto.facultad.FacultadCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Rutas de Facultad:
 *   GET  /api/facultades                  -> listar
 *   POST /api/facultades                  -> registrar
 *   PUT  /api/facultades/{id}             -> actualizar
 *   PUT  /api/facultades/{id}/suspender   -> suspender
 *   PUT  /api/facultades/{id}/reactivar   -> reactivar
 */
public class FacultadRuta extends RutaBase {

    private final FacultadControlador controlador;

    public FacultadRuta(FacultadControlador controlador) {
        super("/api/facultades");
        if (controlador == null) {
            throw new IllegalArgumentException("El controlador no puede ser nulo");
        }
        this.controlador = controlador;
    }

    @Override
    protected void manejar(HttpExchange ex, String metodo, List<String> partes) throws IOException {
        if (partes.isEmpty()) {
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.listarTodas());
            } else if (metodo.equals("POST")) {
                FacultadCrearDto dto = Json.aRecord(FacultadCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.registrar(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }

        Long id = idLong(partes.get(0));

        if (partes.size() == 1) {
            if (metodo.equals("PUT")) {
                Map<String, Object> datos = leerCuerpoConId(ex, id);
                responder(ex, 200, controlador.actualizar(Json.aRecord(FacultadActualizarDto.class, datos)));
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
