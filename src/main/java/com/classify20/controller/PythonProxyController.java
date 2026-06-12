package com.classify20.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Reverse-proxy hacia el microservicio Python/Django (módulo Programación).
 *
 * Módulo aditivo: integra Java + Python bajo un MISMO localhost/puerto.
 * Todo lo que llegue a {@code /py/**} se reenvía de forma transparente al
 * servicio Django (por defecto http://localhost:9393), de modo que el cliente
 * solo habla con un origen (el de Spring Boot) y evita problemas de CORS.
 *
 * No modifica ningún controlador ni vista existente.
 *
 * Ejemplos (con Spring en :9191 y Django en :9393):
 *   GET  /py/                            -> Django  GET  /
 *   GET  /py/programacion/exportarExcel  -> Django  GET  /programacion/exportarExcel
 *   POST /py/guardar-agenda              -> Django  POST /guardar-agenda
 */
@RestController
public class PythonProxyController {

    /** Base del microservicio Python. Configurable por variable de entorno. */
    private static final String PY_BASE = System.getenv()
            .getOrDefault("PYTHON_SERVICE_URL", "http://localhost:9393");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @RequestMapping(value = "/py/**",
            method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        // Ruta tras el prefijo /py + querystring original.
        String path = request.getRequestURI().substring("/py".length());
        if (path.isEmpty()) {
            path = "/";
        }
        String query = request.getQueryString();
        String target = PY_BASE + path + (query != null ? "?" + query : "");

        try {
            HttpRequest.Builder fwd = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofSeconds(20));

            String contentType = request.getContentType();
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                byte[] payload = body != null ? body : new byte[0];
                fwd.POST(HttpRequest.BodyPublishers.ofByteArray(payload));
                if (contentType != null) {
                    fwd.header("Content-Type", contentType);
                }
                // Conserva el header que usa modalagenda.js para pedir JSON.
                String xrw = request.getHeader("X-Requested-With");
                if (xrw != null) {
                    fwd.header("X-Requested-With", xrw);
                }
            } else {
                fwd.GET();
            }

            HttpResponse<byte[]> resp =
                    client.send(fwd.build(), HttpResponse.BodyHandlers.ofByteArray());

            ResponseEntity.BodyBuilder out = ResponseEntity.status(resp.statusCode());
            resp.headers().firstValue("Content-Type")
                    .ifPresent(ct -> out.header("Content-Type", ct));
            resp.headers().firstValue("Content-Disposition")
                    .ifPresent(cd -> out.header("Content-Disposition", cd));
            return out.body(resp.body());

        } catch (IOException e) {
            String msg = "Servicio Python no disponible en " + PY_BASE
                    + " — inicia el microservicio Django. Detalle: " + e.getMessage();
            return ResponseEntity.status(502)
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body(msg.getBytes());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(504)
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body("Tiempo de espera agotado hacia el servicio Python".getBytes());
        }
    }
}
