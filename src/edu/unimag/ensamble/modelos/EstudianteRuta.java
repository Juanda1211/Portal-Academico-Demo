package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.EstudianteControlador;
import edu.unimag.dto.estudiante.EstudianteActualizarDto;
import edu.unimag.dto.estudiante.EstudianteCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rutas de Estudiante (el id es un UUID):
 *   GET  /api/estudiantes                  -> listar todos
 *   GET  /api/estudiantes/activos          -> listar activos
 *   GET  /api/estudiantes/inactivos        -> listar inactivos
 *   GET  /api/estudiantes/{id}             -> buscar por id
 *   POST /api/estudiantes                  -> registrar
 *   PUT  /api/estudiantes/{id}             -> actualizar
 *   PUT  /api/estudiantes/{id}/activar     -> activar
 *   PUT  /api/estudiantes/{id}/inactivar   -> inactivar
 */
public class EstudianteRuta extends RutaBase {

    private final EstudianteControlador controlador;

    public EstudianteRuta(EstudianteControlador controlador) {
        super("/api/estudiantes");
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
                EstudianteCrearDto dto = Json.aRecord(EstudianteCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.registrar(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }
        if (partes.size() == 1 && partes.get(0).equals("activos") && metodo.equals("GET")) {
            responder(ex, 200, controlador.listarActivos());
            return;
        }
        if (partes.size() == 1 && partes.get(0).equals("inactivos") && metodo.equals("GET")) {
            responder(ex, 200, controlador.listarInactivos());
            return;
        }

        UUID id = idUuid(partes.get(0));
        if (partes.size() == 1) {
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.buscarPorId(id));
            } else if (metodo.equals("PUT")) {
                Map<String, Object> datos = leerCuerpoConId(ex, id);
                responder(ex, 200, controlador.actualizar(Json.aRecord(EstudianteActualizarDto.class, datos)));
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
