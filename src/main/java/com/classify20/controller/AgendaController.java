package com.classify20.controller;

import com.classify20.model.Agenda;
import com.classify20.service.AgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;

    //Muestra formulario vacio

    // POST: recibe los datos del formulario y los guarda
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
                response.put("message", "¡Agenda guardada!");
                return ResponseEntity.ok(response);
            }
            
            model.addAttribute("mensaje", "¡Agenda guardada exitosamente!");
            model.addAttribute("agenda", new Agenda());
            return "agenda/agenda";
            
        } catch (Exception e) {
            if ("XMLHttpRequest".equals(requestedWith)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error al guardar: " + e.getMessage());
                return ResponseEntity.status(500).body(response);
            }
            model.addAttribute("error", "Error al guardar la agenda.");
            return "agenda/agenda";
        }
    }
}