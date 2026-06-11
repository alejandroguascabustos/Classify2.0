package com.classify20.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Proxy del webhook "Contacta un Profe".
 *
 * El formulario de /contacta enviaba el POST directamente desde el navegador
 * al webhook de n8n, lo que falla por CORS (n8n no responde el preflight).
 * Este controlador recibe el formulario en el mismo origen (/api/contacta)
 * y reenvía el payload a n8n desde el servidor, donde CORS no aplica.
 */
@RestController
@RequestMapping("/api")
public class ContactaWebhookController {

    private final RestClient restClient;
    private final String webhookUrl;

    public ContactaWebhookController(
            @Value("${classify.webhooks.contacta.url}") String webhookUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(15_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.webhookUrl = webhookUrl;
    }

    @PostMapping("/contacta")
    public ResponseEntity<Map<String, String>> enviar(@RequestBody Map<String, Object> datos) {
        if (datos == null || datos.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Datos del formulario requeridos"));
        }

        Map<String, Object> payload = new LinkedHashMap<>(datos);
        payload.putIfAbsent("action", "contacta_profe");
        payload.putIfAbsent("platform", "Classify");
        payload.putIfAbsent("timestamp", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return ResponseEntity.ok(
                    Map.of("status", "success", "message", "Mensaje enviado correctamente"));
        } catch (RestClientException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("status", "error",
                            "message", "No pudimos enviar el mensaje. Intenta nuevamente."));
        }
    }
}
