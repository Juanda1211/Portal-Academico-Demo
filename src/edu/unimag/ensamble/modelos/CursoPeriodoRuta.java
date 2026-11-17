package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.CursoPeriodoControlador;
import edu.unimag.dto.cursoperiodo.CursoPeriodoActualizarDto;
import edu.unimag.dto.cursoperiodo.CursoPeriodoCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rutas de CursoPeriodo (el id es un UUID):
 *   GET    /api/cursos-periodos        -> listar todos
 *   POST   /api/cursos-periodos        -> registrar (idMateria, idPeriodoAcademico, cupo)
 *   GET    /api/cursos-periodos/{id}   -> buscar por id
 *   PUT    /api/cursos-periodos/{id}   -> actualizar
 *   DELETE /api/cursos-periodos/{id}   -> eliminar
 */
public class CursoPeriodoRuta extends RutaBase {

    private final CursoPeriodoControlador controlador;

    public CursoPeriodoRuta(CursoPeriodoControlador controlador) {
        super("/api/cursos-periodos");
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
                CursoPeriodoCrearDto dto = Json.aRecord(CursoPeriodoCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.registrar(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }
        if (partes.size() == 1) {
            UUID id = idUuid(partes.get(0));
            if (metodo.equals("GET")) {
                responder(ex, 200, controlador.buscarPorId(id));
            } else if (metodo.equals("PUT")) {
                Map<String, Object> datos = leerCuerpoConId(ex, id);
                responder(ex, 200, controlador.actualizar(Json.aRecord(CursoPeriodoActualizarDto.class, datos)));
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