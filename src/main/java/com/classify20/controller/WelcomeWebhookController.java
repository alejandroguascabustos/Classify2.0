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

    @PostMapping("/welcome")
    public ResponseEntity<String> sendWelcome(@RequestBody WelcomeRequest request) {
        if (request == null || request.email() == null || request.email().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email requerido");
        }

        try {
            restClient
                    .post()
                    .uri(webhookUrl)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            return ResponseEntity.ok("OK");
        } catch (RestClientException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Webhook error");
        }
    }

    public record WelcomeRequest(String email) {
    }
}
