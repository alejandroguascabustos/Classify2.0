package com.classify20.controller;

import com.classify20.model.LoginResultado;
import com.classify20.model.SesionUsuario;
import com.classify20.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/auth")
    public String iniciarSesion(@RequestParam("usuario") String usuario,
                                @RequestParam("password") String password,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        LoginResultado resultado = authService.autenticar(usuario, password);

        if (!resultado.success() || resultado.usuario() == null) {
            redirectAttributes.addFlashAttribute("mensaje", resultado.message());
            return "redirect:/login";
        }

        HttpSession sessionActual = request.getSession(false);
        if (sessionActual != null) {
            sessionActual.invalidate();
        }

        HttpSession session = request.getSession(true);
        guardarSesion(session, resultado.usuario());

        return "redirect:/menu";
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("successMessage", "Sesion cerrada correctamente.");
        return "redirect:/login";
    }

    private void guardarSesion(HttpSession session, SesionUsuario usuario) {
        session.setMaxInactiveInterval(10 * 60);
        session.setAttribute("usuarioId", usuario.id());
        session.setAttribute("nombre", usuario.nombre());
        session.setAttribute("apellido", usuario.apellido());
        session.setAttribute("correo", usuario.correo());
        session.setAttribute("usuario", usuario.nombreUsuario());
        session.setAttribute("tipoUsuario", usuario.tipoUsuario());
        session.setAttribute("perfil", usuario.perfil());
        session.setAttribute("materia", usuario.materia());
        session.setAttribute("autenticado", true);
    }
}
