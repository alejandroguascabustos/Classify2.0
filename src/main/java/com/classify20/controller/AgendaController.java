package com.classify20.controller;

import com.classify20.model.Agenda;
import com.classify20.service.AgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;

    @PostMapping("/guardar-agenda")
    public Object guardarAgenda(
            @ModelAttribute Agenda agenda,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            Model model) {
        try {
            agendaService.guardarAgenda(agenda);
            if ("XMLHttpRequest".equals(requestedWith)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Agenda guardada!");
                return ResponseEntity.ok(response);
            }
            model.addAttribute("mensaje", "Agenda guardada exitosamente!");
            model.addAttribute("agenda", new Agenda());
            return "agenda/agenda";
        } catch (IllegalStateException e) {
            // Conflicto de horario detectado
            if ("XMLHttpRequest".equals(requestedWith)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("conflicto", true);
                response.put("message", e.getMessage());
                return ResponseEntity.status(409).body(response);
            }
            model.addAttribute("errorConflicto", e.getMessage());
            model.addAttribute("agenda", agenda);
            return "agenda/agenda";
        } catch (Exception e) {
            if ("XMLHttpRequest".equals(requestedWith)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error al guardar: " + e.getMessage());
                return ResponseEntity.status(500).body(response);
            }
            model.addAttribute("error", "Error inesperado al guardar la agenda.");
            model.addAttribute("agenda", agenda);
            return "agenda/agenda";
        }
    }

    /** Listado que consume agenda.js para validar conflictos en el navegador. */
    @GetMapping("/api/agendas")
    @ResponseBody
    public ResponseEntity<List<Agenda>> listarAgendasJson() {
        return ResponseEntity.ok(agendaService.listarAgendas());
    }
}
