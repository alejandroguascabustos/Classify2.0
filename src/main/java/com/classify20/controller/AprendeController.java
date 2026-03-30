package com.classify20.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.List;

/**
 * Motor de IA de Aprende - Classify (Versión Groq ultra-rápida)
 * Se encarga de procesar las consultas educativas utilizando Llama 3.3.
 */
@RestController
@RequestMapping("/aprende")
public class AprendeController {

    private final RestClient restClient;
    private final String apiKey;

    public AprendeController(@Value("${groq.api.key:}") String apiKey) {
        this.apiKey = apiKey;
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000); // 5 segundos para conectar
        factory.setReadTimeout(40_000);   // 40 segundos para leer respuesta
        
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chatWithAI(@RequestBody Map<String, Object> request) {
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "El servicio de Aprende no tiene configurada la clave de Groq."));
        }

        try {
            // Aseguramos el modelo premium y forzamos el formato JSON nativo para evitar errores de sintaxis
            request.put("model", "llama-3.3-70b-versatile");
            request.put("response_format", Map.of("type", "json_object"));

            // Inyectamos el System Prompt Estándar Classify 2.0 (MODERNO)
            List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
            if (messages != null) {
                messages.add(0, Map.of(
                    "role", "system", 
                    "content", "Eres el motor educativo de Classify 2.0, experto en pedagogía. Tu objetivo es proporcionar lecciones profesionales, minimalistas y estructuradas para docentes. " +
                               "RESPONDE ÚNICAMENTE EN FORMATO JSON. Estructura obligatoria: " +
                               "{ \"breadcrumb\": \"materia > tema\", \"title\": \"Título Profesional\", \"introduction\": \"Resumen ejecutivo\", " +
                               "\"content\": \"Desarrollo conceptual profundo y claro\", \"curiosity\": \"Dato de interés para el aula\", " +
                               "\"pedagogical_application\": \"Guía para el docente: cómo aplicar esto en clase (actividades, debates, dinámicas)\", " +
                               "\"vocabulary\": [{\"term\": \"término\", \"definition\": \"definición\"}], " +
                               "\"key_points\": [\"puntos clave de aprendizaje\"], " +
                               "\"references\": [{\"title\": \"Nombre fuente\", \"url\": \"link real o descriptivo\", \"type\": \"video/lectura\"}], " +
                               "\"suggestions\": [\"temas para profundizar\"] }"
                ));
            }

            Map<String, Object> groqResponse = restClient.post()
                    .uri("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (groqResponse == null || !groqResponse.containsKey("choices")) {
                return ResponseEntity.internalServerError().build();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) groqResponse.get("choices");
            
            if (choices == null || choices.isEmpty()) {
                return ResponseEntity.internalServerError().build();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> choice = choices.get(0);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            
            if (message == null) {
                return ResponseEntity.internalServerError().build();
            }
            
            return ResponseEntity.ok(Map.of("content", message.get("content")));

        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .body(Map.of("error", "Error conectando con Groq: " + e.getMessage()));
        }
    }

    @PostMapping("/guardarClase")
    public ResponseEntity<Map<String, Object>> guardarClase(
            @RequestParam String materia, 
            @RequestParam String tema, 
            @RequestParam(required = false) String observaciones) {
        return ResponseEntity.ok(Map.of("success", true, "message", "Clase procesada instantáneamente por Groq"));
    }
}
