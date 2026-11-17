package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.ProfesorControlador;
import edu.unimag.dto.profesor.ProfesorActualizarDto;
import edu.unimag.dto.profesor.ProfesorCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.List;
import java.util.Map;


//Rutas profesor
public class ProfesorRuta extends RutaBase {

    private final ProfesorControlador controlador;

    public ProfesorRuta(ProfesorControlador controlador) {
        super("/api/profesores");
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
                ProfesorCrearDto dto = Json.aRecord(ProfesorCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
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
        if (partes.size() == 1) {
            Long id = idLong(partes.get(0));
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.buscarPorId(id));
            } else if (metodo.equals("PUT")) {
                Map<String, Object> datos = leerCuerpoConId(ex, id);
                responder(ex, 200, controlador.actualizar(Json.aRecord(ProfesorActualizarDto.class, datos)));
            } else {
                noPermitido(ex);
            }
            return;
        }
        noEncontrado(ex);
    }
}
