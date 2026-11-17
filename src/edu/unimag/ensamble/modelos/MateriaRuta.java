package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.MateriaControlador;
import edu.unimag.dto.materia.MateriaActualizarDto;
import edu.unimag.dto.materia.MateriaCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Rutas de Materia:
 *   GET  /api/materias                  -> listar
 *   POST /api/materias                  -> registrar
 *   GET  /api/materias/{id}             -> buscar por id
 *   PUT  /api/materias/{id}             -> actualizar
 *   PUT  /api/materias/{id}/suspender   -> suspender
 *   PUT  /api/materias/{id}/reactivar   -> reactivar
 */
public class MateriaRuta extends RutaBase {

    private final MateriaControlador controlador;

    public MateriaRuta(MateriaControlador controlador) {
        super("/api/materias");
        if (controlador == null) {
            throw new IllegalArgumentException("El controlador no puede ser nulo");
        }
        this.controlador = controlador;
    }

    @Override
    protected void manejar(HttpExchange ex, String metodo, List<String> partes) throws IOException {
        // /api/materias
        if (partes.isEmpty()) {
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.listarTodas());
            } else if (metodo.equals("POST")) {
                Map<String, Object> datos = Json.leerObjeto(leerCuerpo(ex));
                MateriaCrearDto dto = Json.aRecord(MateriaCrearDto.class, datos);
                responder(ex, 201, controlador.registrar(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }

        Integer id = idEntero(partes.get(0));

        // /api/materias/{id}
        if (partes.size() == 1) {
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.buscarPorId(id));
            } else if (metodo.equals("PUT")) {
                Map<String, Object> datos = Json.leerObjeto(leerCuerpo(ex));
                datos.put("id", id);
                MateriaActualizarDto dto = Json.aRecord(MateriaActualizarDto.class, datos);
                responder(ex, 200, controlador.actualizar(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }

        // /api/materias/{id}/suspender  o  /reactivar
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