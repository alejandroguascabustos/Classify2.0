package com.classify20.controller;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de estado del sistema (módulo aditivo, no altera vistas existentes).
 *
 * Expone información de salud y metadatos de la aplicación en formato JSON,
 * útil para monitoreo, health-checks y para el panel del microservicio Django.
 *
 * Rutas nuevas bajo el prefijo /api — no colisionan con las vistas de
 * {@code VistasController} ni con los webhooks existentes.
 */
@RestController
@RequestMapping("/api")
public class StatusApiController {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${spring.application.name:classify2.0}")
    private String appName;

    /** Estado general: nombre, hora del servidor y uptime legible. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("aplicacion", appName);
        body.put("estado", "OPERATIVO");
        body.put("horaServidor", LocalDateTime.now().format(FMT));
        body.put("uptime", formatearUptime(uptimeMs));
        body.put("uptimeMillis", uptimeMs);
        return body;
    }

    /** Listado de módulos disponibles en la plataforma. */
    @GetMapping("/modulos")
    public Map<String, Object> modulos() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("aplicacion", appName);
        body.put("modulos", List.of(
                "Usuarios", "Agenda", "Programacion", "Aprende",
                "Noticias", "Material", "Contacta", "Soporte"));
        return body;
    }

    /** Health-check minimal para sondas externas. */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", LocalDateTime.now().format(FMT));
        return body;
    }

    private String formatearUptime(long millis) {
        Duration d = Duration.ofMillis(millis);
        return String.format("%dd %02dh %02dm %02ds",
                d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart());
    }
}
