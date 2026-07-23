package com.classify20.web;

import com.classify20.service.PermisosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;

/**
 * Expone a todas las vistas el conjunto de módulos que el usuario en sesión
 * puede ver (por su rol y sus excepciones). Las plantillas lo usan para
 * mostrar u ocultar los enlaces del menú lateral:
 * <pre>th:if="${modulosPermitidos == null or modulosPermitidos.contains('agenda')}"</pre>
 * El valor {@code null} (usuario no autenticado, páginas públicas) deja el menú
 * visible por defecto; el acceso real siempre lo valida el AuthInterceptor.
 */
@ControllerAdvice
public class PermisosModelAdvice {

    private final PermisosService permisosService;

    public PermisosModelAdvice(PermisosService permisosService) {
        this.permisosService = permisosService;
    }

    @ModelAttribute("modulosPermitidos")
    public Set<String> modulosPermitidos(HttpSession session) {
        if (session == null || session.getAttribute("usuarioId") == null) {
            return null;
        }
        Object idObj = session.getAttribute("usuarioId");
        Object rolObj = session.getAttribute("tipoUsuario");
        long usuarioId = (idObj instanceof Number n) ? n.longValue() : -1L;
        String rol = rolObj == null ? "" : rolObj.toString();
        return permisosService.modulosPermitidos(usuarioId, rol);
    }
}
