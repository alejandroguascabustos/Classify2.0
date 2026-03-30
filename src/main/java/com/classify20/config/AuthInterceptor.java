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
        if (session != null && session.getAttribute("usuarioId") != null) {
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
