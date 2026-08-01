package com.classify20.controller;

import com.classify20.service.ChatbotService;
import com.classify20.service.ChatbotService.ChatbotException;
import com.classify20.service.ChatbotService.TurnoChat;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * API del asistente conversacional (chatbot.js). Requiere sesión iniciada:
 * la ruta /api/chatbot está en los patrones de AuthInterceptor, y el POST
 * pasa por CsrfProtectionFilter como cualquier otra escritura.
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    /** Máximo de consultas por sesión dentro de la ventana. */
    private static final int MAX_CONSULTAS = 10;
    private static final long VENTANA_MS = 60_000;
    private static final String ATTR_MARCAS = "chatbotMarcas";

    private final ChatbotService chatbotService;

    public record PeticionChat(String mensaje, List<TurnoChat> historial) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> conversar(@RequestBody PeticionChat peticion,
                                                         HttpSession session) {
        String mensaje = peticion.mensaje() != null ? peticion.mensaje().trim() : "";
        if (mensaje.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Escribe un mensaje para el asistente."));
        }
        if (mensaje.length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "El mensaje es demasiado largo (máximo 1000 caracteres)."));
        }

        if (!dentroDelLimite(session)) {
            return ResponseEntity.status(429).body(Map.of(
                    "success", false,
                    "message", "Has hecho muchas consultas seguidas. Espera un momento e inténtalo de nuevo."));
        }

        if (!chatbotService.estaConfigurado()) {
            return ResponseEntity.status(503).body(Map.of(
                    "success", false,
                    "message", "El asistente inteligente no está disponible por ahora. Usa las opciones del menú."));
        }

        String nombre = atributo(session, "nombre");
        String rol = atributo(session, "rol");
        try {
            String respuesta = chatbotService.responder(mensaje, peticion.historial(), nombre, rol);
            return ResponseEntity.ok(Map.of("success", true, "respuesta", respuesta));
        } catch (ChatbotException e) {
            return ResponseEntity.status(502).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Ventana deslizante simple guardada en la sesión: MAX_CONSULTAS por minuto. */
    @SuppressWarnings("unchecked")
    private static synchronized boolean dentroDelLimite(HttpSession session) {
        Object crudo = session.getAttribute(ATTR_MARCAS);
        Deque<Long> marcas = crudo instanceof Deque ? (Deque<Long>) crudo : new ArrayDeque<>();
        long ahora = System.currentTimeMillis();
        while (!marcas.isEmpty() && ahora - marcas.peekFirst() > VENTANA_MS) {
            marcas.pollFirst();
        }
        if (marcas.size() >= MAX_CONSULTAS) {
            session.setAttribute(ATTR_MARCAS, marcas);
            return false;
        }
        marcas.addLast(ahora);
        session.setAttribute(ATTR_MARCAS, marcas);
        return true;
    }

    private static String atributo(HttpSession session, String nombre) {
        Object v = session.getAttribute(nombre);
        return v != null ? v.toString() : null;
    }
}
