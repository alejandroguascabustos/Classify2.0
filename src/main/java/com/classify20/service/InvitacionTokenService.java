package com.classify20.service;

import com.classify20.model.TokenValidacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Genera y valida tokens de invitación de un solo uso.
 * - Token: 32 bytes aleatorios (Base64 URL-safe). Solo viaja en el enlace.
 * - En BD se guarda únicamente el hash SHA-256 del token.
 * - Vigencia máxima: 10 minutos. Un solo uso. Estados: pendiente | utilizado | expirado.
 * - Límite de intentos para frenar fuerza bruta.
 */
@Service
public class InvitacionTokenService {

    private static final Logger logger = LoggerFactory.getLogger(InvitacionTokenService.class);

    private static final int VIGENCIA_MINUTOS = 10;
    private static final int MAX_INTENTOS = 10;

    private final ClassifyDatabaseService databaseService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RestClient restClient = RestClient.builder().build();

    private final String baseUrl;
    private final String invitacionWebhookUrl;

    public InvitacionTokenService(
            ClassifyDatabaseService databaseService,
            @Value("${classify.base-url:https://www.classify.in.net}") String baseUrl,
            @Value("${classify.webhooks.invitacion.url:}") String invitacionWebhookUrl) {
        this.databaseService = databaseService;
        this.baseUrl = baseUrl;
        this.invitacionWebhookUrl = invitacionWebhookUrl;
    }

    /** Resultado de crear una invitación. */
    public record Invitacion(boolean exito, String mensaje, String enlace, boolean correoEnviado) {}

    /**
     * Crea un token para un usuario pendiente, guarda su hash y arma el enlace.
     * Si hay webhook de n8n configurado, dispara el correo; si no, devuelve el enlace
     * para que el administrador lo copie.
     */
    public Invitacion crearInvitacion(String correo) {
        String correoNorm = correo == null ? "" : correo.trim().toLowerCase();
        if (correoNorm.isBlank()) {
            return new Invitacion(false, "Debes indicar un correo.", null, false);
        }

        try (Connection conn = databaseService.openConnection()) {
            // Busca el usuario pendiente por correo (autorizado y aún no registrado)
            Long pendienteId = null;
            String nombre = "";
            String sqlPend = "SELECT id, nombre FROM usuarios_pendientes WHERE LOWER(correo) = ? AND estado = 'autorizado' LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlPend)) {
                ps.setString(1, correoNorm);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        pendienteId = rs.getLong("id");
                        nombre = rs.getString("nombre");
                    }
                }
            }
            if (pendienteId == null) {
                return new Invitacion(false,
                        "No hay un usuario autorizado con ese correo pendiente de registro. Cárgalo primero (Excel) o revisa el correo.",
                        null, false);
            }

            // Invalida invitaciones previas pendientes de ese usuario (una activa a la vez)
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE registro_tokens SET estado = 'expirado' WHERE usuario_pendiente_id = ? AND estado = 'pendiente'")) {
                ps.setLong(1, pendienteId);
                ps.executeUpdate();
            }

            String token = generarToken();
            String hash = sha256(token);
            LocalDateTime ahora = LocalDateTime.now();

            String sqlIns = "INSERT INTO registro_tokens (token_hash, correo, usuario_pendiente_id, estado, creado_en, expira_en) " +
                    "VALUES (?, ?, ?, 'pendiente', ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlIns)) {
                ps.setString(1, hash);
                ps.setString(2, correoNorm);
                ps.setLong(3, pendienteId);
                ps.setTimestamp(4, Timestamp.valueOf(ahora));
                ps.setTimestamp(5, Timestamp.valueOf(ahora.plusMinutes(VIGENCIA_MINUTOS)));
                ps.executeUpdate();
            }

            String enlace = baseUrl + "/registro?token=" + token;
            boolean correoEnviado = enviarWebhook(nombre, correoNorm, enlace);

            String msg = correoEnviado
                    ? "Invitación enviada al correo " + correoNorm + " (vence en 10 minutos)."
                    : "Invitación generada (vence en 10 minutos). Copia el enlace y envíalo al usuario.";
            return new Invitacion(true, msg, enlace, correoEnviado);

        } catch (SQLException e) {
            logger.error("Error creando invitación", e);
            return new Invitacion(false, "Error al generar la invitación: " + e.getMessage(), null, false);
        }
    }

    /**
     * Valida un token recibido en la URL. Incrementa intentos y marca expirados.
     * No marca como utilizado (eso ocurre al crear la cuenta).
     */
    public TokenValidacion validar(String token) {
        if (token == null || token.isBlank()) {
            return TokenValidacion.invalido("El enlace no incluye un token.");
        }
        String hash = sha256(token.trim());

        try (Connection conn = databaseService.openConnection()) {
            String sql = "SELECT id, correo, usuario_pendiente_id, estado, expira_en, intentos FROM registro_tokens WHERE token_hash = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, hash);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return TokenValidacion.invalido("El enlace ya no es válido. Solicite una nueva invitación al administrador.");
                    }
                    long id = rs.getLong("id");
                    String estado = rs.getString("estado");
                    Timestamp expira = rs.getTimestamp("expira_en");
                    int intentos = rs.getInt("intentos");

                    // Cuenta el intento
                    actualizarIntentos(conn, id, intentos + 1);

                    if (intentos + 1 > MAX_INTENTOS) {
                        marcarEstado(conn, id, "expirado");
                        return TokenValidacion.invalido("El enlace ya no es válido. Solicite una nueva invitación al administrador.");
                    }
                    if (!"pendiente".equals(estado)) {
                        return TokenValidacion.invalido("El enlace ya no es válido. Solicite una nueva invitación al administrador.");
                    }
                    if (expira == null || expira.toLocalDateTime().isBefore(LocalDateTime.now())) {
                        marcarEstado(conn, id, "expirado");
                        return TokenValidacion.invalido("El enlace ya no es válido. Solicite una nueva invitación al administrador.");
                    }
                    return new TokenValidacion(true, "ok", id, rs.getString("correo"), rs.getLong("usuario_pendiente_id"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error validando token", e);
            return TokenValidacion.invalido("No fue posible validar el enlace.");
        }
    }

    /** Marca el token como utilizado y el usuario pendiente como registrado (tras crear la cuenta). */
    public void marcarUtilizado(long tokenId, Long usuarioPendienteId) {
        try (Connection conn = databaseService.openConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE registro_tokens SET estado = 'utilizado', usado_en = ? WHERE id = ?")) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(2, tokenId);
                ps.executeUpdate();
            }
            if (usuarioPendienteId != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE usuarios_pendientes SET estado = 'registrado' WHERE id = ?")) {
                    ps.setLong(1, usuarioPendienteId);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            logger.error("Error marcando token utilizado", e);
        }
    }

    private boolean enviarWebhook(String nombre, String correo, String enlace) {
        if (invitacionWebhookUrl == null || invitacionWebhookUrl.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("nombre", nombre);
            payload.put("correo", correo);
            payload.put("enlace", enlace);
            payload.put("asunto", "Invitación para completar su registro");
            restClient.post()
                    .uri(invitacionWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            logger.warn("No se pudo enviar el webhook de invitación: {}", e.getMessage());
            return false;
        }
    }

    private void actualizarIntentos(Connection conn, long id, int intentos) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE registro_tokens SET intentos = ? WHERE id = ?")) {
            ps.setInt(1, intentos);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private void marcarEstado(Connection conn, long id, String estado) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE registro_tokens SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
