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
    @org.springframework.web.bind.annotation.ResponseBody
    public Object procesarRegistro(@ModelAttribute RegistroForm registroForm,
                                   RedirectAttributes redirectAttributes,
                                   jakarta.servlet.http.HttpServletRequest request) {
        RegistroResultado resultado = registroService.registrar(registroForm);

        // Si es una petición AJAX (Fetch), devolvemos JSON
        String accept = request.getHeader("Accept");
        if (accept != null && (accept.contains("application/json") || "XMLHttpRequest".equals(request.getHeader("X-Requested-With")))) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", resultado.success());
            response.put("message", resultado.message());
            return response;
        }

        if (resultado.success()) {
            redirectAttributes.addFlashAttribute("successMessage", resultado.message());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", resultado.message());
        }

        return new org.springframework.web.servlet.view.RedirectView("/registro");
    }
}
