package com.classify20.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba el comportamiento del filtro CSRF: que deja pasar lo legítimo,
 * que rechaza lo que no trae token y que el token llega a las vistas.
 */
class CsrfProtectionFilterTest {

    private final CsrfProtectionFilter filtro = new CsrfProtectionFilter();

    @Test
    @DisplayName("Un POST sin token es rechazado con 403")
    void postSinTokenEsRechazado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/guardar-agenda");
        request.setSession(new MockHttpSession());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }

    @Test
    @DisplayName("Un POST con el token de la sesión pasa")
    void postConTokenValidoPasa() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CsrfProtectionFilter.ATRIBUTO_SESION, "token-de-prueba");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/guardar-agenda");
        request.setSession(session);
        request.addParameter(CsrfProtectionFilter.NOMBRE_PARAMETRO, "token-de-prueba");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @DisplayName("Un POST con un token que no es el de la sesión es rechazado")
    void postConTokenAjenoEsRechazado() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CsrfProtectionFilter.ATRIBUTO_SESION, "token-de-la-sesion");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/gestion-permisos/roles");
        request.setSession(session);
        request.addParameter(CsrfProtectionFilter.NOMBRE_PARAMETRO, "token-inventado");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }

    @Test
    @DisplayName("El token también se acepta por cabecera, para las llamadas AJAX")
    void postConTokenEnCabeceraPasa() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CsrfProtectionFilter.ATRIBUTO_SESION, "token-ajax");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/aprende/chat");
        request.setSession(session);
        request.addHeader(CsrfProtectionFilter.NOMBRE_CABECERA, "token-ajax");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @DisplayName("Un GET nunca se bloquea")
    void getNoRequiereToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/menu");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @DisplayName("El formulario de contacto tampoco se puede invocar desde fuera")
    void rutaApiSinTokenEsRechazada() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/contacta");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }

    @Test
    @DisplayName("El formulario de contacto sí funciona desde el propio sitio")
    void rutaApiConTokenPasa() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CsrfProtectionFilter.ATRIBUTO_SESION, "token-contacta");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/contacta");
        request.setSession(session);
        request.addParameter(CsrfProtectionFilter.NOMBRE_PARAMETRO, "token-contacta");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @DisplayName("El HTML servido recibe el meta, el campo oculto y el script")
    void seInyectaElTokenEnElHtml() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        request.setSession(new MockHttpSession());
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain cadena = (req, res) -> {
            res.setContentType("text/html;charset=UTF-8");
            res.getWriter().write("""
                    <html><head><title>Classify</title></head>
                    <body><form method="post" action="/login/auth">
                    <input name="usuario"></form></body></html>
                    """);
        };

        filtro.doFilter(request, response, cadena);

        String html = response.getContentAsString();
        assertTrue(html.contains("name=\"_csrf\" content="), "falta el meta con el token");
        assertTrue(html.contains("<input type=\"hidden\" name=\"_csrf\""), "falta el campo oculto en el formulario");
        assertTrue(html.contains("XMLHttpRequest.prototype.send"), "falta el script para AJAX");
        assertNotNull(request.getSession(false).getAttribute(CsrfProtectionFilter.ATRIBUTO_SESION));
    }

    @Test
    @DisplayName("Una respuesta JSON se entrega intacta")
    void elJsonNoSeToca() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/aprende/historial");
        request.setSession(new MockHttpSession());
        MockHttpServletResponse response = new MockHttpServletResponse();

        String json = "{\"respuesta\":\"hola\"}";
        FilterChain cadena = (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write(json);
        };

        filtro.doFilter(request, response, cadena);

        assertEquals(json, response.getContentAsString());
    }
}
