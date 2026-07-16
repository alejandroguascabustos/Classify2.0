package com.classify20.controller;

import com.classify20.model.RegistroForm;
import com.classify20.model.RegistroResultado;
import com.classify20.model.TokenValidacion;
import com.classify20.service.RegistroService;
import com.classify20.service.InvitacionTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/registro")
public class RegistroController {

    private final RegistroService registroService;
    private final InvitacionTokenService invitacionTokenService;

    @Value("${classify.registro.solo-invitacion:false}")
    private boolean soloInvitacion;

    public RegistroController(RegistroService registroService,
                              InvitacionTokenService invitacionTokenService) {
        this.registroService = registroService;
        this.invitacionTokenService = invitacionTokenService;
    }

    @PostMapping("/procesarRegistro")
    @org.springframework.web.bind.annotation.ResponseBody
    public Object procesarRegistro(@ModelAttribute RegistroForm registroForm,
                                   @RequestParam(value = "token", required = false) String token,
                                   RedirectAttributes redirectAttributes,
                                   jakarta.servlet.http.HttpServletRequest request) {
        // Validación del token de invitación
        TokenValidacion tokenValidacion = null;
        boolean esAjax = esAjax(request);
        if (token != null && !token.isBlank()) {
            tokenValidacion = invitacionTokenService.validar(token);
            if (!tokenValidacion.valido()) {
                return respuestaError(tokenValidacion.motivo(), esAjax, redirectAttributes);
            }
        } else if (soloInvitacion) {
            return respuestaError(
                    "El registro es solo por invitación. Solicite acceso al administrador.",
                    esAjax, redirectAttributes);
        }

        RegistroResultado resultado = registroService.registrar(registroForm);

        // Si el registro fue exitoso y venía con token, se consume el token
        if (resultado.success() && tokenValidacion != null) {
            invitacionTokenService.marcarUtilizado(
                    tokenValidacion.tokenId(), tokenValidacion.usuarioPendienteId());
        }

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

    private boolean esAjax(jakarta.servlet.http.HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    private Object respuestaError(String mensaje, boolean esAjax, RedirectAttributes redirectAttributes) {
        if (esAjax) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", mensaje);
            return response;
        }
        redirectAttributes.addFlashAttribute("errorMessage", mensaje);
        return new org.springframework.web.servlet.view.RedirectView("/registro");
    }
}
