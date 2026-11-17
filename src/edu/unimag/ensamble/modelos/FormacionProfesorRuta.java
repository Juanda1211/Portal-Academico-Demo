package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.FormacionProfesorControlador;
import edu.unimag.dto.formacion.FormacionProfesorActualizarDto;
import edu.unimag.dto.formacion.FormacionProfesorCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Rutas de FormacionProfesor:
 *   GET    /api/formaciones                       -> listar todas
 *   GET    /api/formaciones/profesor/{idProfesor} -> listar las de un profesor
 *   POST   /api/formaciones                       -> registrar
 *   PUT    /api/formaciones/{id}                  -> actualizar
 *   DELETE /api/formaciones/{id}                  -> eliminar
 */
public class FormacionProfesorRuta extends RutaBase {

    private final FormacionProfesorControlador controlador;

    public FormacionProfesorRuta(FormacionProfesorControlador controlador) {
        super("/api/formaciones");
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
                FormacionProfesorCrearDto dto = Json.aRecord(FormacionProfesorCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.registrar(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }

        // /api/formaciones/profesor/{idProfesor}
        if (partes.size() == 2 && partes.get(0).equals("profesor")) {
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.listarPorProfesor(idLong(partes.get(1))));
            } else {
                noPermitido(ex);
            }
            return;
        }

        // /api/formaciones/{id}
        if (partes.size() == 1) {
            Long id = idLong(partes.get(0));
            if (metodo.equals("PUT")) {
                Map<String, Object> datos = leerCuerpoConId(ex, id);
                responder(ex, 200, controlador.actualizar(Json.aRecord(FormacionProfesorActualizarDto.class, datos)));
            } else if (metodo.equals("DELETE")) {
                Map<String, Object> respuesta = new LinkedHashMap<>();
                respuesta.put("eliminado", controlador.eliminar(id));
                responder(ex, 200, respuesta);
            } else {
                noPermitido(ex);
            }
            return;
        }
        noEncontrado(ex);
    }
}
