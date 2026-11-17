package edu.unimag.ensamble;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base de todas las rutas de la API (una por controlador).
 * Se encarga de lo repetitivo: separar la URL, leer el cuerpo, responder en JSON
 * y convertir las excepciones de Java en códigos HTTP.
 */
public abstract class RutaBase implements HttpHandler {

    private final String base;

    protected RutaBase(String base) {
        this.base = base;
    }

    /**
     * @param metodo GET, POST, PUT, DELETE
     * @param partes lo que viene después de la ruta base, por ejemplo
     *               /api/materias/3/suspender -> ["3", "suspender"]
     */
    protected abstract void manejar(HttpExchange ex, String metodo, List<String> partes) throws IOException;

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String resto = ex.getRequestURI().getPath().substring(base.length());
            List<String> partes = Arrays.stream(resto.split("/"))
                    .filter(p -> !p.isEmpty())
                    .toList();
            manejar(ex, ex.getRequestMethod(), partes);
            if (ex.getResponseCode() == -1) { // red de seguridad: toda petición debe responderse
                responder(ex, 500, error("La ruta no envió ninguna respuesta"));
            }
        } catch (IllegalArgumentException e) {
            // Validación de los DTO o datos inválidos
            System.out.println("ATRAPE UN ERROR DE VALIDACION!: " + e.getMessage());
            responder(ex, 400, error(e.getMessage()));
        } catch (IllegalStateException e) {
            // Reglas de negocio, por ejemplo estado inválido
            responder(ex, 409, error(e.getMessage()));
        } catch (Throwable e) {
            e.printStackTrace();
            responder(ex, 500, error("Error interno: " + e.getClass().getSimpleName() + " - " + e.getMessage()));
        } finally {
            ex.close(); // se cierra DESPUÉS de responder (por eso va en finally y no en try-with-resources)
        }
    }

    protected void responder(HttpExchange ex, int codigo, Object cuerpo) throws IOException {
        byte[] bytes = Json.aJson(cuerpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream salida = ex.getResponseBody()) {
            salida.write(bytes);
        }
    }

    protected String leerCuerpo(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Lee el JSON del cuerpo y le agrega el id que viene en la URL. */
    protected Map<String, Object> leerCuerpoConId(HttpExchange ex, Object id) throws IOException {
        Map<String, Object> datos = new HashMap<>(Json.leerObjeto(leerCuerpo(ex)));
        datos.put("id", id);
        return datos;
    }

    /**
     * Separa los parámetros de una query string en un mapa clave/valor, ya
     * decodificados. Es el mismo comportamiento que tenía cada ruta que lo
     * necesitaba por su cuenta; vive acá para no repetirlo.
     */
    protected Map<String, String> query(String raw) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return resultado;
        }
        for (String par : raw.split("&")) {
            String[] trozos = par.split("=", 2);
            String clave = URLDecoder.decode(trozos[0], StandardCharsets.UTF_8);
            String valor = trozos.length > 1 ? URLDecoder.decode(trozos[1], StandardCharsets.UTF_8) : "";
            resultado.put(clave, valor);
        }
        return resultado;
    }

    protected Integer idEntero(String texto) {
        try {
            return Integer.valueOf(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El id debe ser un número entero");
        }
    }

    protected Long idLong(String texto) {
        try {
            return Long.valueOf(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El id debe ser un número entero");
        }
    }

    protected UUID idUuid(String texto) {
        try {
            return UUID.fromString(texto);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El id no tiene formato UUID válido");
        }
    }

    protected void noEncontrado(HttpExchange ex) throws IOException {
        responder(ex, 404, error("Ruta no encontrada"));
    }

    protected void noPermitido(HttpExchange ex) throws IOException {
        responder(ex, 405, error("Método no permitido"));
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", mensaje == null ? "Error" : mensaje);
        return cuerpo;
    }
}
