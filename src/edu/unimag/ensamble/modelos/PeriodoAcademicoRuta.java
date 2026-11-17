package edu.unimag.ensamble.modelos;

import com.sun.net.httpserver.HttpExchange;
import edu.unimag.controlador.PeriodoAcademicoControlador;
import edu.unimag.dto.periodoacademico.PeriodoAcademicoActualizarDto;
import edu.unimag.dto.periodoacademico.PeriodoAcademicoCrearDto;
import edu.unimag.ensamble.Json;
import edu.unimag.ensamble.RutaBase;
import java.io.IOException;
import java.util.List;
import java.util.Map;


//Rutas periodo academico

public class PeriodoAcademicoRuta extends RutaBase {

    private final PeriodoAcademicoControlador controlador;

    public PeriodoAcademicoRuta(PeriodoAcademicoControlador controlador) {
        super("/api/periodos");
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
                PeriodoAcademicoCrearDto dto = Json.aRecord(PeriodoAcademicoCrearDto.class, Json.leerObjeto(leerCuerpo(ex)));
                responder(ex, 201, controlador.registrar(dto));
            } else {
                noPermitido(ex);
            }
            return;
        }

        Integer id = idEntero(partes.get(0));

        if (partes.size() == 1) {
            if (metodo.equals("PUT")) {
                Map<String, Object> datos = leerCuerpoConId(ex, id);
                responder(ex, 200, controlador.actualizar(Json.aRecord(PeriodoAcademicoActualizarDto.class, datos)));
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
