package edu.unimag.ensamble;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor web con el HttpServer que ya trae el JDK.
 * - Entrega los archivos de la carpeta web (html, css, js, img).
 * - Permite registrar rutas de API (/api/...).
 */
public class ServidorWeb {

    private static final Map<String, String> TIPOS = Map.ofEntries(
            Map.entry("html", "text/html; charset=utf-8"),
            Map.entry("css", "text/css; charset=utf-8"),
            Map.entry("js", "text/javascript; charset=utf-8"),
            Map.entry("json", "application/json; charset=utf-8"),
            Map.entry("txt", "text/plain; charset=utf-8"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("ico", "image/x-icon")
    );

    private final HttpServer servidor;
    private final ExecutorService hilo;
    private final Path carpetaWeb;
    private final int puerto;

    public ServidorWeb(int puerto, Path carpetaWeb) throws IOException {
        this.puerto = puerto;
        this.carpetaWeb = carpetaWeb.toAbsolutePath().normalize();
        // Solo acepta conexiones de este mismo computador (localhost)
        this.servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        // Un solo hilo: los archivos .txt no soportan escrituras simultáneas
        this.hilo = Executors.newSingleThreadExecutor();
        this.servidor.setExecutor(hilo);
        this.servidor.createContext("/", this::servirArchivo);
    }

    public void registrar(String ruta, HttpHandler manejador) {
        servidor.createContext(ruta, manejador);
    }

    public void iniciar() {
        servidor.start();
        System.out.println("Servidor listo en http://localhost:" + puerto);
        System.out.println("Carpeta web: " + carpetaWeb);
    }

    public void detener() {
        servidor.stop(0);
        hilo.shutdown();
    }

    private void servirArchivo(HttpExchange ex) throws IOException {
        try {
            if (!ex.getRequestMethod().equals("GET")) {
                texto(ex, 405, "Método no permitido");
                return;
            }
            String ruta = ex.getRequestURI().getPath();
            if (ruta.equals("/")) {
                ruta = "/index.html";
            }
            Path archivo;
            try {
                archivo = carpetaWeb.resolve(ruta.substring(1)).normalize();
            } catch (InvalidPathException e) {
                texto(ex, 404, "No encontrado");
                return;
            }
            // Seguridad: no permitir salir de la carpeta web (../)
            if (!archivo.startsWith(carpetaWeb) || !Files.isRegularFile(archivo)) {
                texto(ex, 404, "No encontrado: " + ruta);
                return;
            }
            byte[] bytes = Files.readAllBytes(archivo);
            ex.getResponseHeaders().set("Content-Type", tipoDe(archivo));
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            if (bytes.length == 0) {
                ex.sendResponseHeaders(200, -1); // archivo vacío
            } else {
                ex.sendResponseHeaders(200, bytes.length);
                try (OutputStream salida = ex.getResponseBody()) {
                    salida.write(bytes);
                }
            }
        } finally {
            ex.close();
        }
    }

    private void texto(HttpExchange ex, int codigo, String mensaje) throws IOException {
        byte[] bytes = mensaje.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream salida = ex.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private String tipoDe(Path archivo) {
        String nombre = archivo.getFileName().toString().toLowerCase();
        int punto = nombre.lastIndexOf('.');
        String extension = punto >= 0 ? nombre.substring(punto + 1) : "";
        return TIPOS.getOrDefault(extension, "application/octet-stream");
    }
}
