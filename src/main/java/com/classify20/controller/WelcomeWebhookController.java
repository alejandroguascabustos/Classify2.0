package com.classify20.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api")
public class WelcomeWebhookController {

    private final RestClient restClient;
    private final String webhookUrl;

    public WelcomeWebhookController(
            @Value("${classify.webhooks.welcome.url}") String webhookUrl) {
        this.restClient = RestClient.create();
        this.webhookUrl = webhookUrl;
    }

    /**
     * Endpoint para el formulario de bienvenida de la página de inicio.
     * Captura el correo y envía un JSON profesional al webhook de n8n.
     */
    @PostMapping("/welcome")
    public ResponseEntity<Map<String, String>> sendWelcome(@RequestBody WelcomeRequest request) {
        if (request == null || request.email() == null || request.email().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "El correo electrónico es requerido"));
        }

        String cleanEmail = request.email().trim().toLowerCase();

        // Construir payload profesional para n8n
        Map<String, Object> webhookPayload = new LinkedHashMap<>();
        webhookPayload.put("email", cleanEmail);
        webhookPayload.put("source", request.source() != null ? request.source() : "inicio");
        webhookPayload.put("nombre", request.nombre() != null ? request.nombre() : "");
        webhookPayload.put("platform", "Classify");
        webhookPayload.put("timestamp", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        webhookPayload.put("action", "welcome_email");

        try {
            restClient
                    .post()
                    .uri(webhookUrl)
                    .header("Content-Type", "application/json")
                    .body(webhookPayload)
                    .retrieve()
                    .toBodilessEntity();

            return ResponseEntity.ok(
                    Map.of("status", "success",
                           "message", "¡Correo de bienvenida enviado exitosamente!"));
        } catch (RestClientException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("status", "error",
                                 "message", "No pudimos enviar el correo. Intenta nuevamente."));
        }
    }

    /**
     * Record que representa la solicitud de bienvenida.
     * Captura email, origen del formulario y nombre opcional.
     */
    public record WelcomeRequest(String email, String source, String nombre) {
    }
}
