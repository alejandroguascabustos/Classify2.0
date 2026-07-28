package com.classify20.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Protección frente a falsificación de petición en sitios cruzados (CSRF).
 *
 * <p>Hace dos cosas complementarias:
 *
 * <ol>
 *   <li><b>Valida</b> que toda petición que modifica estado (POST, PUT, PATCH,
 *       DELETE) traiga el token asociado a la sesión, ya sea en el parámetro
 *       {@code _csrf} o en la cabecera {@code X-CSRF-TOKEN}.</li>
 *   <li><b>Inyecta</b> el token en las respuestas HTML: un {@code <meta>} en la
 *       cabecera, un campo oculto dentro de cada formulario POST y un script que
 *       añade la cabecera a las llamadas AJAX.</li>
 * </ol>
 *
 * <p>La inyección automática evita tener que recordar el campo oculto en cada
 * plantilla nueva: cualquier formulario queda cubierto por construcción. La
 * evolución natural de esta pieza es adoptar {@code spring-boot-starter-security},
 * que ofrece lo mismo de forma nativa junto con la autorización por rol.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CsrfProtectionFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CsrfProtectionFilter.class);

    public static final String ATRIBUTO_SESION = "_csrfToken";
    public static final String NOMBRE_PARAMETRO = "_csrf";
    public static final String NOMBRE_CABECERA = "X-CSRF-TOKEN";

    /** Métodos que, por definición, no deberían alterar el estado del servidor. */
    private static final List<String> METODOS_SEGUROS = List.of("GET", "HEAD", "OPTIONS", "TRACE");

    /**
     * Rutas exentas de validación.
     *
     * <p>Está vacía a propósito. Las rutas bajo {@code /api/} tienen nombre de
     * webhook, pero no las invoca n8n: son el formulario de "Contacta" y la
     * llamada de bienvenida, ambos lanzados desde el propio sitio. Al exigirles
     * token dejan de poder invocarse desde fuera, que era la vía para abusar del
     * envío de correos.
     *
     * <p>Si en el futuro un sistema externo necesita publicar aquí, lo correcto
     * es añadir su ruta a esta lista y protegerla con un secreto compartido, no
     * dejarla abierta.
     */
    private static final List<String> RUTAS_EXENTAS = List.of();

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Pattern FIN_HEAD = Pattern.compile("</head>", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIN_BODY = Pattern.compile("</body>", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORMULARIO = Pattern.compile("<form\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern METODO_POST = Pattern.compile("method\\s*=\\s*[\"']?post", Pattern.CASE_INSENSITIVE);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (requiereValidacion(request)) {
            String esperado = tokenDeSesion(request, false);
            String recibido = tokenDePeticion(request);

            if (esperado == null || recibido == null || !coincide(esperado, recibido)) {
                logger.warn("Peticion {} {} rechazada por token CSRF ausente o invalido.",
                        request.getMethod(), request.getRequestURI());
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "La solicitud no incluye un token de seguridad valido. Recarga la pagina e intentalo de nuevo.");
                return;
            }
        }

        // Se garantiza el token antes de renderizar, para poder inyectarlo.
        String token = tokenDeSesion(request, true);

        RespuestaEnMemoria envoltura = new RespuestaEnMemoria(response);
        chain.doFilter(request, envoltura);
        envoltura.volcar(token);
    }

    private boolean requiereValidacion(HttpServletRequest request) {
        if (METODOS_SEGUROS.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            return false;
        }
        String uri = request.getRequestURI();
        return RUTAS_EXENTAS.stream().noneMatch(uri::startsWith);
    }

    private String tokenDePeticion(HttpServletRequest request) {
        String cabecera = request.getHeader(NOMBRE_CABECERA);
        if (cabecera != null && !cabecera.isBlank()) {
            return cabecera.trim();
        }
        String parametro = request.getParameter(NOMBRE_PARAMETRO);
        return (parametro == null || parametro.isBlank()) ? null : parametro.trim();
    }

    private String tokenDeSesion(HttpServletRequest request, boolean crear) {
        HttpSession session = request.getSession(crear);
        if (session == null) {
            return null;
        }
        Object actual = session.getAttribute(ATRIBUTO_SESION);
        if (actual instanceof String token && !token.isBlank()) {
            return token;
        }
        if (!crear) {
            return null;
        }
        byte[] material = new byte[32];
        RANDOM.nextBytes(material);
        String nuevo = Base64.getUrlEncoder().withoutPadding().encodeToString(material);
        session.setAttribute(ATRIBUTO_SESION, nuevo);
        return nuevo;
    }

    /** Comparación de tiempo constante: no debe filtrar cuántos caracteres coinciden. */
    private boolean coincide(String esperado, String recibido) {
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8));
    }

    private static String escaparHtml(String valor) {
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Retiene la respuesta para poder insertar el token cuando el contenido es
     * HTML. El resto de contenidos (JSON, imágenes, descargas) se reenvía tal cual.
     */
    private static final class RespuestaEnMemoria extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final StringWriter escritorTexto = new StringWriter();
        private ServletOutputStream flujo;
        private PrintWriter escritor;

        private RespuestaEnMemoria(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            if (flujo == null) {
                flujo = new ServletOutputStream() {
                    @Override
                    public void write(int b) {
                        buffer.write(b);
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                        // La respuesta se construye en memoria: no hay escritura asincrona.
                    }
                };
            }
            return flujo;
        }

        @Override
        public PrintWriter getWriter() {
            if (escritor == null) {
                escritor = new PrintWriter(escritorTexto);
            }
            return escritor;
        }

        @Override
        public void flushBuffer() {
            // El volcado se hace una sola vez, al terminar la cadena de filtros.
        }

        private boolean esHtml() {
            String tipo = getContentType();
            return tipo != null && tipo.toLowerCase(Locale.ROOT).contains("text/html");
        }

        private Charset codificacion() {
            try {
                String nombre = getCharacterEncoding();
                return nombre == null ? StandardCharsets.UTF_8 : Charset.forName(nombre);
            } catch (Exception e) {
                return StandardCharsets.UTF_8;
            }
        }

        private void volcar(String token) throws IOException {
            if (escritor != null) {
                escritor.flush();
            }

            Charset charset = codificacion();
            boolean usoEscritor = escritor != null;
            String contenido = usoEscritor
                    ? escritorTexto.toString()
                    : new String(buffer.toByteArray(), charset);

            if (!esHtml() || contenido.isEmpty()) {
                byte[] crudo = usoEscritor ? contenido.getBytes(charset) : buffer.toByteArray();
                escribirCrudo(crudo);
                return;
            }

            byte[] salida = inyectar(contenido, token).getBytes(charset);
            escribirCrudo(salida);
        }

        private void escribirCrudo(byte[] datos) throws IOException {
            HttpServletResponse original = (HttpServletResponse) getResponse();
            if (!original.isCommitted()) {
                original.setContentLength(datos.length);
            }
            original.getOutputStream().write(datos);
            original.getOutputStream().flush();
        }

        private String inyectar(String html, String token) {
            String seguro = escaparHtml(token);

            // 1. Meta en la cabecera, para que el script y cualquier codigo propio
            //    puedan leer el token.
            Matcher head = FIN_HEAD.matcher(html);
            if (head.find()) {
                String meta = "<meta name=\"_csrf\" content=\"" + seguro + "\">\n"
                        + "<meta name=\"_csrf_header\" content=\"" + NOMBRE_CABECERA + "\">\n";
                html = new StringBuilder(html).insert(head.start(), meta).toString();
            }

            // 2. Campo oculto en cada formulario que envie por POST.
            StringBuilder conFormularios = new StringBuilder();
            Matcher form = FORMULARIO.matcher(html);
            int desde = 0;
            while (form.find()) {
                conFormularios.append(html, desde, form.end());
                if (METODO_POST.matcher(form.group()).find()) {
                    conFormularios.append("<input type=\"hidden\" name=\"")
                            .append(NOMBRE_PARAMETRO).append("\" value=\"")
                            .append(seguro).append("\">");
                }
                desde = form.end();
            }
            conFormularios.append(html.substring(desde));
            html = conFormularios.toString();

            // 3. Script que añade la cabecera a fetch y XMLHttpRequest, de modo que
            //    las llamadas AJAX existentes no necesitan modificarse.
            Matcher body = FIN_BODY.matcher(html);
            if (body.find()) {
                html = new StringBuilder(html).insert(body.start(), script(seguro)).toString();
            }
            return html;
        }

        private String script(String token) {
            return """
                    <script>
                    (function () {
                      var token = "%s";
                      var cabecera = "%s";
                      function mismoOrigen(url) {
                        try { return new URL(url, window.location.href).origin === window.location.origin; }
                        catch (e) { return false; }
                      }
                      function modifica(metodo) {
                        return metodo && !/^(GET|HEAD|OPTIONS|TRACE)$/i.test(metodo);
                      }
                      var fetchOriginal = window.fetch;
                      if (fetchOriginal) {
                        window.fetch = function (recurso, opciones) {
                          opciones = opciones || {};
                          var url = (typeof recurso === "string") ? recurso : (recurso && recurso.url);
                          var metodo = opciones.method || (recurso && recurso.method) || "GET";
                          if (modifica(metodo) && mismoOrigen(url)) {
                            var cabeceras = new Headers(opciones.headers || (recurso && recurso.headers) || {});
                            if (!cabeceras.has(cabecera)) { cabeceras.set(cabecera, token); }
                            opciones.headers = cabeceras;
                          }
                          return fetchOriginal.call(this, recurso, opciones);
                        };
                      }
                      var abrirOriginal = XMLHttpRequest.prototype.open;
                      var enviarOriginal = XMLHttpRequest.prototype.send;
                      XMLHttpRequest.prototype.open = function (metodo, url) {
                        this.__csrfAplica = modifica(metodo) && mismoOrigen(url);
                        return abrirOriginal.apply(this, arguments);
                      };
                      XMLHttpRequest.prototype.send = function () {
                        if (this.__csrfAplica) { this.setRequestHeader(cabecera, token); }
                        return enviarOriginal.apply(this, arguments);
                      };
                    })();
                    </script>
                    """.formatted(token, NOMBRE_CABECERA);
        }
    }
}
