package com.classify20.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.List;
import java.util.Locale;

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
    public ResponseEntity<Map<String, Object>> chatWithAI(@RequestBody Map<String, Object> request,
                                                          HttpSession session) {
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "El servicio de Aprende no tiene configurada la clave de Groq."));
        }

        try {
            // Aseguramos el modelo premium y forzamos el formato JSON nativo para evitar errores de sintaxis
            request.put("model", "llama-3.3-70b-versatile");
            request.put("response_format", Map.of("type", "json_object"));

            String tipoUsuario = leerAtributoSesion(session, "tipoUsuario").toLowerCase(Locale.ROOT);
            String materiaUsuario = leerAtributoSesion(session, "materia");

            // Inyectamos el System Prompt según el perfil de la sesión.
            List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
            if (messages != null) {
                messages.add(0, Map.of(
                    "role", "system", 
                    "content", construirPromptSistema(tipoUsuario, materiaUsuario)
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

    private String construirPromptSistema(String tipoUsuario, String materiaUsuario) {
        if ("docente".equals(tipoUsuario)) {
            String contextoMateria = materiaUsuario == null || materiaUsuario.isBlank()
                    ? ""
                    : "Materia principal del docente: " + materiaUsuario + ". ";

            return "Eres el motor pedagógico de Classify 2.0 para docentes. " +
                    contextoMateria +
                    "Responde únicamente en JSON válido, sin bloques markdown ni texto adicional. " +
                    "Tu respuesta debe ayudar a organizar una clase clara, innovadora y lista para ejecutar en el aula. " +
                    "Prioriza planeación docente, ideas de apoyo, tareas de profundización y material de apoyo real. " +
                    "Evita respuestas superficiales o genéricas. No uses frases vacías como 'es importante' sin explicar cómo se aplica en clase. " +
                    "Mantén un tono profesional, accionable, concreto y útil para un docente que necesita llevar esto al aula hoy. " +
                    "Estructura obligatoria: " +
                    "{ " +
                    "\"breadcrumb\": \"materia > tema\", " +
                    "\"title\": \"Título breve de la clase\", " +
                    "\"introduction\": \"Resumen ejecutivo de 2 o 3 líneas que explique qué logrará la clase\", " +
                    "\"topic_summary\": \"Resumen desarrollado en 2 o 3 párrafos con conceptos clave, relaciones y ejemplos cotidianos\", " +
                    "\"class_organization\": { " +
                    "\"objective\": \"aprendizaje esperado en una frase\", " +
                    "\"inicio\": {\"time\": \"10 min\", \"teacher_action\": \"qué hace el docente\", \"student_action\": \"qué hacen los estudiantes\", \"resources\": [\"recurso\"], \"evidence\": \"cómo validar comprensión\"}, " +
                    "\"desarrollo\": {\"time\": \"25 min\", \"teacher_action\": \"qué hace el docente\", \"student_action\": \"qué hacen los estudiantes\", \"resources\": [\"recurso\"], \"evidence\": \"cómo validar comprensión\"}, " +
                    "\"cierre\": {\"time\": \"10 min\", \"teacher_action\": \"qué hace el docente\", \"student_action\": \"qué hacen los estudiantes\", \"resources\": [\"recurso\"], \"evidence\": \"cómo validar comprensión\"} " +
                    "}, " +
                    "\"innovation_ideas\": [" +
                    "{\"title\": \"nombre breve\", \"purpose\": \"para qué sirve\", \"execution\": \"cómo aplicarla en clase\"}, " +
                    "{\"title\": \"nombre breve\", \"purpose\": \"para qué sirve\", \"execution\": \"cómo aplicarla en clase\"}, " +
                    "{\"title\": \"nombre breve\", \"purpose\": \"para qué sirve\", \"execution\": \"cómo aplicarla en clase\"}" +
                    "], " +
                    "\"support_material\": [" +
                    "{\"resource\": \"material o recurso\", \"use\": \"cómo usarlo\", \"moment\": \"en qué momento de la clase\"}, " +
                    "{\"resource\": \"material o recurso\", \"use\": \"cómo usarlo\", \"moment\": \"en qué momento de la clase\"}, " +
                    "{\"resource\": \"material o recurso\", \"use\": \"cómo usarlo\", \"moment\": \"en qué momento de la clase\"}" +
                    "], " +
                    "\"homework_topics\": [" +
                    "{\"task\": \"actividad o tarea\", \"purpose\": \"qué refuerza\", \"delivery\": \"cómo entregarla o presentarla\"}, " +
                    "{\"task\": \"actividad o tarea\", \"purpose\": \"qué refuerza\", \"delivery\": \"cómo entregarla o presentarla\"}, " +
                    "{\"task\": \"actividad o tarea\", \"purpose\": \"qué refuerza\", \"delivery\": \"cómo entregarla o presentarla\"}" +
                    "], " +
                    "\"key_points\": [\"aprendizaje esencial\", \"otro aprendizaje esencial\"], " +
                    "\"references\": [{\"title\": \"Nombre de la fuente\", \"url\": \"enlace real\", \"type\": \"video/lectura\", \"source\": \"institución o portal\"}], " +
                    "\"success_tip\": \"Recomendación final concreta para que la enseñanza sea un éxito\", " +
                    "\"suggestions\": [\"tema relacionado para profundizar\", \"otro tema relacionado\"] " +
                    "} " +
                    "Incluye exactamente 4 referencias confiables y útiles para docentes. " +
                    "Prioriza fuentes educativas e institucionales y evita Wikipedia salvo que no exista una mejor alternativa.";
        }

        return "Eres el motor educativo de Classify 2.0, experto en pedagogía. " +
                "Responde únicamente en JSON válido, sin bloques markdown ni texto adicional. " +
                "Tu objetivo es proporcionar lecciones profesionales, claras y estructuradas para aprendizaje guiado. " +
                "Estructura obligatoria: " +
                "{ " +
                "\"breadcrumb\": \"materia > tema\", " +
                "\"title\": \"Título profesional\", " +
                "\"introduction\": \"Resumen ejecutivo\", " +
                "\"content\": \"Desarrollo conceptual profundo y claro\", " +
                "\"curiosity\": \"Dato de interés para reforzar el tema\", " +
                "\"pedagogical_application\": \"Cómo aplicar este contenido en actividades o explicaciones guiadas\", " +
                "\"vocabulary\": [{\"term\": \"término\", \"definition\": \"definición\"}], " +
                "\"key_points\": [\"puntos clave de aprendizaje\"], " +
                "\"references\": [{\"title\": \"Nombre fuente\", \"url\": \"link real o descriptivo\", \"type\": \"video/lectura\", \"source\": \"institución o portal\"}], " +
                "\"suggestions\": [\"temas para profundizar\"] " +
                "} " +
                "Incluye exactamente 4 referencias confiables.";
    }

    private String leerAtributoSesion(HttpSession session, String atributo) {
        Object value = session == null ? null : session.getAttribute(atributo);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
