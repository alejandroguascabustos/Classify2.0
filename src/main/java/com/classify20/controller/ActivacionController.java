package com.classify20.controller;

import com.classify20.service.ActivacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Activación de cuenta: el usuario abre el enlace de un solo uso del correo de
 * bienvenida y fija su contraseña definitiva.
 */
@Controller
public class ActivacionController {

    private final ActivacionService activacionService;

    public ActivacionController(ActivacionService activacionService) {
        this.activacionService = activacionService;
    }

    @GetMapping("/activar")
    public String mostrar(@RequestParam(value = "token", required = false) String token, Model model) {
        ActivacionService.Validacion v = activacionService.validar(token);
        if (v.valido()) {
            model.addAttribute("tokenValido", true);
            model.addAttribute("token", token);
            model.addAttribute("nombre", v.nombre());
        } else {
            model.addAttribute("tokenValido", false);
            model.addAttribute("mensaje", v.motivo());
        }
        return "auth/activar";
    }

    @PostMapping("/activar")
    public String procesar(@RequestParam("token") String token,
                           @RequestParam("password") String password,
                           @RequestParam("password2") String password2,
                           Model model, RedirectAttributes redirect) {
        if (!password.equals(password2)) {
            reponerFormulario(token, model, "Las contraseñas no coinciden.");
            return "auth/activar";
        }
        String error = activacionService.cambiarPassword(token, password);
        if (error != null) {
            reponerFormulario(token, model, error);
            return "auth/activar";
        }
        // Contraseña creada → al login para que pruebe su nuevo acceso
        redirect.addFlashAttribute("activado", true);
        return "redirect:/login";
    }

    private void reponerFormulario(String token, Model model, String mensajeError) {
        ActivacionService.Validacion v = activacionService.validar(token);
        if (v.valido()) {
            model.addAttribute("tokenValido", true);
            model.addAttribute("token", token);
            model.addAttribute("nombre", v.nombre());
            model.addAttribute("error", mensajeError);
        } else {
            model.addAttribute("tokenValido", false);
            model.addAttribute("mensaje", v.motivo());
        }
    }
}
