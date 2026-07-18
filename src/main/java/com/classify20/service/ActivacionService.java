package com.classify20.service;

import com.classify20.model.ParametrosColegio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Flujo de bienvenida / activación de cuenta.
 * Para los usuarios cargados (usuarios_pendientes), crea su cuenta con una
 * contraseña temporal y les envía por n8n un correo de bienvenida con:
 * nombre del colegio, su nombre, su grado, la contraseña temporal y un enlace
 * de UN SOLO USO para fijar su contraseña definitiva.
 */
@Service
public class ActivacionService {

    private static final Logger logger = LoggerFactory.getLogger(ActivacionService.class);

    private static final int VIGENCIA_MINUTOS = 60 * 24 * 7; // 7 días
    private static final int MAX_INTENTOS = 10;
    private static final String ALFA_TOKEN = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    // Contraseña temporal legible (sin caracteres ambiguos)
    private static final String ALFA_PASS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final ClassifyDatabaseService databaseService;
    private final ParametrosColegioService parametrosService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final RestClient restClient = RestClient.builder().build();

    private final String baseUrl;
    private final String bienvenidaWebhookUrl;

    public ActivacionService(
            ClassifyDatabaseService databaseService,
            ParametrosColegioService parametrosService,
            @Value("${classify.base-url:https://www.classify.in.net}") String baseUrl,
            @Value("${classify.webhooks.bienvenida-activacion.url:}") String bienvenidaWebhookUrl) {
        this.databaseService = databaseService;
        this.parametrosService = parametrosService;
        this.baseUrl = baseUrl;
        this.bienvenidaWebhookUrl = bienvenidaWebhookUrl;
    }

    public record ResultadoMasivo(int enviados, int fallidos, int correosOk, List<String> detalles) {}

    /**
     * Para cada usuario autorizado pendiente: crea la cuenta con contraseña temporal,
     * genera el token de activación y envía el correo de bienvenida.
     */
    public ResultadoMasivo enviarBienvenidaMasiva() {
        ParametrosColegio params = parametrosService.obtener();
        List<Map<String, Object>> pendientes = pendientesAutorizados();

        if (pendientes.isEmpty()) {
            return new ResultadoMasivo(0, 0, 0,
                    List.of("No hay usuarios autorizados pendientes de bienvenida."));
        }

        int enviados = 0, fallidos = 0, correosOk = 0;
        List<String> detalles = new ArrayList<>();

        for (Map<String, Object> p : pendientes) {
            String correo = (String) p.get("correo");
            String nombreCompleto = ((String) p.get("nombre") + " " + (String) p.get("apellido")).trim();
            try (Connection conn = databaseService.openConnection()) {
                conn.setAutoCommit(false);
                try {
                    if (yaExisteCuenta(conn, p)) {
                        detalles.add("Omitido " + correo + ": ya tiene cuenta.");
                        conn.rollback();
                        continue;
                    }

                    String passwordTemporal = generar(ALFA_PASS, 8);
                    long usuarioId = crearCuenta(conn, p, passwordEncoder.encode(passwordTemporal));

                    String token = generar(ALFA_TOKEN, 40);
                    crearToken(conn, usuarioId, sha256(token));

                    marcarPendienteRegistrado(conn, (Long) p.get("id"));
                    conn.commit();

                    String enlace = baseUrl + "/activar?token=" + token;
                    boolean correoEnviado = enviarWebhook(params.nombreColegio(), nombreCompleto,
                            correo, gradoLegible(p), (String) p.get("tipo_usuario"), passwordTemporal, enlace);
                    if (correoEnviado) correosOk++;
                    enviados++;
                } catch (Exception e) {
                    conn.rollback();
                    fallidos++;
                    detalles.add("Error con " + correo + ": " + e.getMessage());
                }
            } catch (SQLException e) {
                fallidos++;
                detalles.add("Error de BD con " + correo + ": " + e.getMessage());
            }
        }

        return new ResultadoMasivo(enviados, fallidos, correosOk, detalles);
    }

    // ── Validación y cambio de contraseña por token ──────────────────

    public record Validacion(boolean valido, String motivo, Long tokenId, Long usuarioId, String nombre) {
        static Validacion invalida(String motivo) { return new Validacion(false, motivo, null, null, null); }
    }

    public Validacion validar(String token) {
        if (token == null || token.isBlank()) {
            return Validacion.invalida("El enlace no incluye un token.");
        }
        String hash = sha256(token.trim());
        String sql = "SELECT t.id, t.usuario_id, t.estado, t.expira_en, t.intentos, u.nombre " +
                "FROM activacion_tokens t JOIN registro_usuarios u ON u.id = t.usuario_id WHERE t.token_hash = ?";
        try (Connection conn = databaseService.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Validacion.invalida("El enlace ya no es válido. Solicite uno nuevo al administrador.");
                }
                long id = rs.getLong("id");
                String estado = rs.getString("estado");
                Timestamp expira = rs.getTimestamp("expira_en");
                int intentos = rs.getInt("intentos");
                actualizarIntentos(conn, id, intentos + 1);

                if (intentos + 1 > MAX_INTENTOS) {
                    marcarEstado(conn, id, "expirado");
                    return Validacion.invalida("El enlace ya no es válido. Solicite uno nuevo al administrador.");
                }
                if (!"pendiente".equals(estado)) {
                    return Validacion.invalida("Este enlace ya fue utilizado. Si ya activaste tu cuenta, inicia sesión.");
                }
                if (expira == null || expira.toLocalDateTime().isBefore(LocalDateTime.now())) {
                    marcarEstado(conn, id, "expirado");
                    return Validacion.invalida("El enlace venció. Solicite uno nuevo al administrador.");
                }
                return new Validacion(true, "ok", id, rs.getLong("usuario_id"), rs.getString("nombre"));
            }
        } catch (SQLException e) {
            logger.error("Error validando token de activación", e);
            return Validacion.invalida("No fue posible validar el enlace.");
        }
    }

    /** Fija la contraseña definitiva y consume el token (un solo uso). */
    public String cambiarPassword(String token, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.length() < 6) {
            return "La contraseña debe tener mínimo 6 caracteres.";
        }
        Validacion v = validar(token);
        if (!v.valido()) {
            return v.motivo();
        }
        try (Connection conn = databaseService.openConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE registro_usuarios SET pass_hash = ?, debe_cambiar_password = false WHERE id = ?")) {
                ps.setString(1, passwordEncoder.encode(nuevaPassword));
                ps.setLong(2, v.usuarioId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE activacion_tokens SET estado = 'utilizado', usado_en = ? WHERE id = ?")) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(2, v.tokenId());
                ps.executeUpdate();
            }
            conn.commit();
            return null; // sin error
        } catch (SQLException e) {
            logger.error("Error cambiando contraseña", e);
            return "No fue posible guardar la nueva contraseña.";
        }
    }

    // ── Helpers de BD ────────────────────────────────────────────────

    private List<Map<String, Object>> pendientesAutorizados() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, apellido, correo, documento, telefono, nombre_usuario, " +
                "tipo_usuario, curso, materia, grado, grupo FROM usuarios_pendientes WHERE estado = 'autorizado' ORDER BY id";
        try (Connection conn = databaseService.openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getLong("id"));
                for (String c : new String[]{"nombre", "apellido", "correo", "documento", "telefono",
                        "nombre_usuario", "tipo_usuario", "curso", "materia", "grado", "grupo"}) {
                    m.put(c, rs.getString(c));
                }
                lista.add(m);
            }
        } catch (SQLException e) {
            logger.error("Error listando pendientes", e);
        }
        return lista;
    }

    private boolean yaExisteCuenta(Connection conn, Map<String, Object> p) throws SQLException {
        String sql = "SELECT 1 FROM registro_usuarios WHERE LOWER(correo) = LOWER(?) " +
                "OR LOWER(documento) = LOWER(?) OR LOWER(nombre_usuario) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (String) p.get("correo"));
            ps.setString(2, (String) p.get("documento"));
            ps.setString(3, (String) p.get("nombre_usuario"));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private long crearCuenta(Connection conn, Map<String, Object> p, String passHash) throws SQLException {
        String telefono = (String) p.get("telefono");
        if (telefono == null || telefono.isBlank()) telefono = "0000000";
        String sql = "INSERT INTO registro_usuarios " +
                "(nombre, apellido, correo, documento, telefono, nombre_usuario, pass_hash, tipo_usuario, curso, materia, debe_cambiar_password) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (String) p.get("nombre"));
            ps.setString(2, (String) p.get("apellido"));
            ps.setString(3, (String) p.get("correo"));
            ps.setString(4, (String) p.get("documento"));
            ps.setString(5, telefono);
            ps.setString(6, (String) p.get("nombre_usuario"));
            ps.setString(7, passHash);
            ps.setString(8, (String) p.get("tipo_usuario"));
            ps.setString(9, (String) p.get("curso"));
            ps.setString(10, (String) p.get("materia"));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void crearToken(Connection conn, long usuarioId, String hash) throws SQLException {
        String sql = "INSERT INTO activacion_tokens (token_hash, usuario_id, estado, creado_en, expira_en) " +
                "VALUES (?, ?, 'pendiente', ?, ?)";
        LocalDateTime ahora = LocalDateTime.now();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setLong(2, usuarioId);
            ps.setTimestamp(3, Timestamp.valueOf(ahora));
            ps.setTimestamp(4, Timestamp.valueOf(ahora.plusMinutes(VIGENCIA_MINUTOS)));
            ps.executeUpdate();
        }
    }

    private void marcarPendienteRegistrado(Connection conn, long pendienteId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE usuarios_pendientes SET estado = 'registrado' WHERE id = ?")) {
            ps.setLong(1, pendienteId);
            ps.executeUpdate();
        }
    }

    private void actualizarIntentos(Connection conn, long id, int intentos) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE activacion_tokens SET intentos = ? WHERE id = ?")) {
            ps.setInt(1, intentos);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private void marcarEstado(Connection conn, long id, String estado) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE activacion_tokens SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    // ── Webhook y utilidades ─────────────────────────────────────────

    private boolean enviarWebhook(String colegio, String nombre, String correo, String grado,
                                  String rol, String passwordTemporal, String enlace) {
        if (bienvenidaWebhookUrl == null || bienvenidaWebhookUrl.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("colegio", colegio);
            payload.put("nombre", nombre);
            payload.put("correo", correo);
            payload.put("grado", grado);
            payload.put("rol", rol);
            payload.put("passwordTemporal", passwordTemporal);
            payload.put("enlace", enlace);
            payload.put("asunto", "Bienvenido(a) a " + colegio + " en Classify");
            restClient.post().uri(bienvenidaWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload).retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            logger.warn("No se pudo enviar el webhook de bienvenida a {}: {}", correo, e.getMessage());
            return false;
        }
    }

    /** Grado legible: para estudiante su curso; para otros, su rol. */
    private String gradoLegible(Map<String, Object> p) {
        String tipo = (String) p.get("tipo_usuario");
        if ("estudiante".equalsIgnoreCase(tipo)) {
            String grado = (String) p.get("grado");
            String grupo = (String) p.get("grupo");
            String curso = (String) p.get("curso");
            if (grado != null && !grado.isBlank()) {
                return grado + "°" + (grupo != null && !grupo.isBlank() ? " " + grupo : "");
            }
            return curso != null ? curso : "";
        }
        return tipo == null ? "" : tipo.substring(0, 1).toUpperCase() + tipo.substring(1);
    }

    private String generar(String alfabeto, int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(alfabeto.charAt(secureRandom.nextInt(alfabeto.length())));
        }
        return sb.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
