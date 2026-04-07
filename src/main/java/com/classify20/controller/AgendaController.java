package com.classify20.controller;

import com.classify20.model.Agenda;
import com.classify20.service.AgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;

    //Muestra formulario vacio

    // POST: recibe los datos del formulario y los guarda
    @PostMapping("/guardar-agenda")
    public String guardarAgenda(@ModelAttribute Agenda agenda, Model model) {
        agendaService.guardarAgenda(agenda);
        model.addAttribute("mensaje", "¡Agenda guardada exitosamente!");
        model.addAttribute("agenda", new Agenda()); // limpia el formulario
        return "agenda/agenda";
    }
}