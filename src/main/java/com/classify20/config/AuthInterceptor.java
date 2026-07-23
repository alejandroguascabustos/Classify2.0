package com.classify20.config;

import com.classify20.service.PermisosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guarda de acceso de la app. Exige sesión iniciada y, para los módulos
 * administrados, valida el permiso efectivo del usuario (rol + excepciones)
 * resuelto por {@link PermisosService}. Ya no hay reglas de acceso
 * hardcodeadas: el administrador las configura desde /gestion-permisos.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final PermisosService permisosService;

    public AuthInterceptor(PermisosService permisosService) {
        this.permisosService = permisosService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        boolean autenticado = session != null && session.getAttribute("usuarioId") != null;

        if (!autenticado) {
            String loginUrl = request.getContextPath() + "/login";
            if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()) {
                response.sendRedirect(loginUrl + "?expired=1");
                return false;
            }
            response.sendRedirect(loginUrl);
            return false;
        }

        // Ruta relativa al contexto (normalmente el contexto es "/").
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        String clave = permisosService.claveDeRuta(uri);
        if (clave != null) {
            long usuarioId = leerUsuarioId(session);
            String rol = leerRol(session);
            if (!permisosService.puedeAcceder(usuarioId, rol, clave)) {
                response.sendRedirect(request.getContextPath() + "/menu?denegado=1");
                return false;
            }
        }
        return true;
    }

    private long leerUsuarioId(HttpSession session) {
        Object idObj = session.getAttribute("usuarioId");
        return (idObj instanceof Number n) ? n.longValue() : -1L;
    }

    private String leerRol(HttpSession session) {
        Object rolObj = session.getAttribute("tipoUsuario");
        return rolObj == null ? "" : rolObj.toString();
    }
}
