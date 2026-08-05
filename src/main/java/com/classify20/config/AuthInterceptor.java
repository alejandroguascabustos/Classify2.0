package com.classify20.config;

import com.classify20.service.PermisosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Guarda de acceso de la app. Exige sesión iniciada y, para los módulos
 * administrados, valida el permiso efectivo del usuario (rol + excepciones)
 * resuelto por {@link PermisosService}. Ya no hay reglas de acceso
 * hardcodeadas: el administrador las configura desde /gestion-permisos.
 *
 * <p>Ademas retiene al usuario en la pantalla de cambio de contrasena
 * obligatorio mientras la bandera siga activa. Sin este bloqueo, bastaba
 * escribir /menu en la barra de direcciones para saltarse el cambio, dejando
 * la cuenta indefinidamente con la clave temporal enviada por correo.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** Rutas alcanzables aun con el cambio de contrasena pendiente. */
    private static final Set<String> RUTAS_PERMITIDAS_SIN_CAMBIAR = Set.of(
            "/cambiar-password-obligatorio",
            "/cambiar-password-obligatorio/cambiar",
            "/logout"
    );

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

        // Mientras la bandera este activa, cualquier ruta protegida devuelve
        // al usuario a la pantalla de cambio. Se comprueba antes que los
        // permisos por modulo: la cuenta no deberia poder hacer nada mas.
        if (Boolean.TRUE.equals(session.getAttribute("debeCambiarPassword"))
                && !RUTAS_PERMITIDAS_SIN_CAMBIAR.contains(uri)) {
            response.sendRedirect(request.getContextPath() + "/cambiar-password-obligatorio");
            return false;
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
