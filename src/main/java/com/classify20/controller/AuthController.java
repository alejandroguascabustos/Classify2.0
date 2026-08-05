package com.classify20.controller;

import com.classify20.model.LoginResultado;
import com.classify20.model.SesionUsuario;
import com.classify20.service.AuthService;
import com.classify20.service.PasswordRecoveryService;
import org.springframework.ui.Model;
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
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(AuthService authService, PasswordRecoveryService passwordRecoveryService) {
        this.authService = authService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @GetMapping("/recuperar-password")
    public String mostrarRecuperarPassword(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) {
            return "redirect:/menu";
        }
        return "auth/recuperar_password";
    }

    @PostMapping("/recuperar-password/enviar")
    public String recuperarPassword(@RequestParam("documento") String documento,
                                    @RequestParam("correo") String correo,
                                    @RequestParam(value = "nombre_usu", required = false) String nombreUsuario,
                                    RedirectAttributes redirectAttributes) {
        PasswordRecoveryService.RecuperacionPasswordResultado resultado =
                passwordRecoveryService.recuperarContrasena(documento, correo, nombreUsuario);

        if (resultado.success()) {
            redirectAttributes.addFlashAttribute("successMessage", resultado.message());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", resultado.message());
        }

        return "redirect:/recuperar-password";
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

        // Si la cuenta sigue con la clave temporal que envio n8n, no se
        // entra a la aplicacion hasta definir una propia. El
        // AuthInterceptor impide ademas saltarse esta pantalla escribiendo
        // /menu directamente en la barra de direcciones.
        if (resultado.usuario().debeCambiarPassword()) {
            return "redirect:/cambiar-password-obligatorio";
        }

        return "redirect:/menu";
    }

    // ── Cambio de contrasena obligatorio ────────────────────────────

    @GetMapping("/cambiar-password-obligatorio")
    public String mostrarCambioObligatorio(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) {
            return "redirect:/login";
        }
        // Si la bandera ya esta en false, esta pantalla no aplica.
        if (!Boolean.TRUE.equals(session.getAttribute("debeCambiarPassword"))) {
            return "redirect:/menu";
        }
        return "auth/cambiar_password_obligatorio";
    }

    @PostMapping("/cambiar-password-obligatorio/cambiar")
    public String procesarCambioObligatorio(@RequestParam("password_actual") String passwordActual,
                                            @RequestParam("password_nueva") String passwordNueva,
                                            @RequestParam("password_confirmar") String passwordConfirmar,
                                            HttpSession session,
                                            RedirectAttributes redirectAttributes) {

        Object idSesion = session.getAttribute("usuarioId");
        if (idSesion == null) {
            return "redirect:/login";
        }
        long usuarioId = ((Number) idSesion).longValue();

        if (passwordNueva == null || passwordNueva.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "La nueva contrasena debe tener al menos 6 caracteres.");
            return "redirect:/cambiar-password-obligatorio";
        }

        if (!passwordNueva.equals(passwordConfirmar)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Las contrasenas no coinciden.");
            return "redirect:/cambiar-password-obligatorio";
        }

        if (passwordNueva.equals(passwordActual)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "La nueva contrasena debe ser distinta de la temporal.");
            return "redirect:/cambiar-password-obligatorio";
        }

        boolean cambiada = authService.cambiarPassword(usuarioId, passwordActual, passwordNueva);
        if (!cambiada) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "La contrasena actual no es correcta.");
            return "redirect:/cambiar-password-obligatorio";
        }

        // La bandera baja tambien en la sesion viva, para que el
        // interceptor deje pasar de inmediato sin exigir un nuevo login.
        session.setAttribute("debeCambiarPassword", false);
        redirectAttributes.addFlashAttribute("successMessage",
                "Contrasena actualizada correctamente.");
        return "redirect:/menu";
    }

    @GetMapping("/recuperar-password/cambiar")
    public String mostrarRestablecerPassword(@RequestParam(value = "token", required = false) String token,
                                             Model model) {
        if (!model.containsAttribute("tokenRecibido")) {
            model.addAttribute("tokenRecibido", token == null ? "" : token.trim());
        }
        return "auth/restablecer_password";
    }

    @PostMapping("/recuperar-password/cambiar")
    public String cambiarPasswordConToken(@RequestParam("token_temporal") String tokenTemporal,
                                          @RequestParam("password_nueva") String passwordNueva,
                                          @RequestParam("password_confirmar") String passwordConfirmar,
                                          RedirectAttributes redirectAttributes) {
        PasswordRecoveryService.CambioPasswordResultado resultado =
                passwordRecoveryService.restablecerConToken(tokenTemporal, passwordNueva, passwordConfirmar);

        if (!resultado.success()) {
            redirectAttributes.addFlashAttribute("errorMessage", resultado.message());
            redirectAttributes.addFlashAttribute("tokenRecibido", tokenTemporal);
            return "redirect:/recuperar-password/cambiar";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                resultado.message() + " Inicia sesion con tu nueva contrasena.");
        return "redirect:/login";
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
        // El interceptor consulta este atributo en cada peticion.
        session.setAttribute("debeCambiarPassword", usuario.debeCambiarPassword());
    }
}
