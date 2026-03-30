package com.classify20.controller;

import com.classify20.model.RegistroForm;
import com.classify20.model.RegistroResultado;
import com.classify20.service.RegistroService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/registro")
public class RegistroController {

    private final RegistroService registroService;

    public RegistroController(RegistroService registroService) {
        this.registroService = registroService;
    }

    @PostMapping("/procesarRegistro")
    public String procesarRegistro(@ModelAttribute RegistroForm registroForm,
                                   RedirectAttributes redirectAttributes) {
        RegistroResultado resultado = registroService.registrar(registroForm);

        if (resultado.success()) {
            redirectAttributes.addFlashAttribute("successMessage", resultado.message());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", resultado.message());
        }

        return "redirect:/registro";
    }
}
