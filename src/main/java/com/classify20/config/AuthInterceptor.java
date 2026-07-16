package com.classify20.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        boolean autenticado = session != null && session.getAttribute("usuarioId") != null;

        if (autenticado) {
            // Restricción por perfil para el módulo Gestión de Registros:
            // solo Administrador (perfil 1) y Coordinador (perfil 2).
            String uri = request.getRequestURI();
            if (uri != null && uri.contains("/gestion-registros")) {
                Object perfilObj = session.getAttribute("perfil");
                int perfil = (perfilObj instanceof Integer p) ? p : -1;
                if (perfil != 1 && perfil != 2) {
                    response.sendRedirect(request.getContextPath() + "/menu");
                    return false;
                }
            }
            return true;
        }

        String loginUrl = request.getContextPath() + "/login";
        if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()) {
            response.sendRedirect(loginUrl + "?expired=1");
            return false;
        }

        response.sendRedirect(loginUrl);
        return false;
    }
}
