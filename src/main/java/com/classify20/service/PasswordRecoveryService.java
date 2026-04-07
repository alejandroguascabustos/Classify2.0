package com.classify20.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class PasswordRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordRecoveryService.class);

    private static final String FIND_USER_FOR_RECOVERY_SQL = """
            SELECT id, nombre, apellido, correo, nombre_usuario
            FROM registro_usuarios
            WHERE documento = ? AND LOWER(correo) = ?
            LIMIT 1
            """;

    private static final String INVALIDATE_ACTIVE_TOKENS_SQL = """
            UPDATE olvido_contrasenia_tokens
            SET usado = TRUE,
                usado_en = CURRENT_TIMESTAMP
            WHERE id_usuario = ?
              AND usado = FALSE
            """;

    private static final String INSERT_TOKEN_SQL = """
            INSERT INTO olvido_contrasenia_tokens (
                id_usuario,
                token,
                expira_en,
                usado,
                creado_en
            ) VALUES (?, ?, ?, FALSE, CURRENT_TIMESTAMP)
            """;

    private static final String FIND_TOKEN_SQL = """
            SELECT id, id_usuario, expira_en, usado
            FROM olvido_contrasenia_tokens
            WHERE token = ?
            LIMIT 1
            """;

    private static final String MARK_TOKEN_USED_SQL = """
            UPDATE olvido_contrasenia_tokens
            SET usado = TRUE,
                usado_en = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String INVALIDATE_OTHER_TOKENS_SQL = """
            UPDATE olvido_contrasenia_tokens
            SET usado = TRUE,
                usado_en = CURRENT_TIMESTAMP
            WHERE id_usuario = ?
              AND id <> ?
              AND usado = FALSE
            """;

    private static final String UPDATE_PASSWORD_SQL = """
            UPDATE registro_usuarios
            SET pass_hash = ?,
                password_temporal = FALSE
            WHERE id = ?
            """;

    private static final String TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TOKEN_SIZE = 8;

    private final ClassifyDatabaseService databaseService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final RestClient restClient;
    private final String passwordResetWebhookUrl;
    private final String passwordResetPageUrl;
    private final int tokenDurationMinutes;

    public PasswordRecoveryService(
            ClassifyDatabaseService databaseService,
            @Value("${classify.webhooks.password-reset.url:}") String passwordResetWebhookUrl,
            @Value("${classify.password-reset.page-url:http://localhost:8090/recuperar-password/cambiar}") String passwordResetPageUrl,
            @Value("${classify.password-reset.token-minutes:15}") int tokenDurationMinutes) {
        this.databaseService = databaseService;
        this.passwordResetWebhookUrl = passwordResetWebhookUrl == null ? "" : passwordResetWebhookUrl.trim();
        this.passwordResetPageUrl = passwordResetPageUrl == null ? "" : passwordResetPageUrl.trim();
        this.tokenDurationMinutes = tokenDurationMinutes <= 0 ? 15 : tokenDurationMinutes;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
        this.restClient = RestClient.builder().build();
    }

    public RecuperacionPasswordResultado recuperarContrasena(String documentoIngresado,
                                                             String correoIngresado,
                                                             String usuarioIngresado) {
        String documento = limpiar(documentoIngresado);
        String correo = limpiar(correoIngresado).toLowerCase(Locale.ROOT);
        String usuario = limpiar(usuarioIngresado).toLowerCase(Locale.ROOT);

        if (documento.isBlank() || correo.isBlank()) {
            return new RecuperacionPasswordResultado(false, "Debes completar el documento y el correo.");
        }

        try (Connection connection = databaseService.openConnection()) {
            connection.setAutoCommit(false);

            try {
                UsuarioRecuperacion usuarioRecuperacion = buscarUsuario(connection, documento, correo, usuario);
                if (usuarioRecuperacion == null) {
                    connection.rollback();
                    return new RecuperacionPasswordResultado(false, "No encontramos un usuario con esos datos.");
                }

                String token = generarToken();
                LocalDateTime expiracion = LocalDateTime.now().plusMinutes(tokenDurationMinutes);

                invalidarTokensActivos(connection, usuarioRecuperacion.id());
                guardarToken(connection, usuarioRecuperacion.id(), token, expiracion);
                connection.commit();

                if (enviarTokenRecuperacion(usuarioRecuperacion, token, expiracion)) {
                    return new RecuperacionPasswordResultado(
                            true,
                            "Verificamos tu identidad y enviamos un token temporal al correo registrado.");
                }

                return new RecuperacionPasswordResultado(
                        true,
                        "Verificamos tu identidad. Usa este token temporal valido por " + tokenDurationMinutes
                                + " minutos para cambiar tu contrasena: " + token);
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            logger.error("No fue posible generar el token de recuperacion.", exception);
            return new RecuperacionPasswordResultado(
                    false,
                    "No fue posible procesar la recuperacion de contrasena: " + exception.getMessage());
        }
    }

    public CambioPasswordResultado restablecerConToken(String tokenIngresado,
                                                       String passwordNuevaIngresada,
                                                       String passwordConfirmarIngresada) {
        String token = limpiar(tokenIngresado).toUpperCase(Locale.ROOT);
        String passwordNueva = limpiar(passwordNuevaIngresada);
        String passwordConfirmar = limpiar(passwordConfirmarIngresada);

        if (token.isBlank() || passwordNueva.isBlank() || passwordConfirmar.isBlank()) {
            return new CambioPasswordResultado(false, "Debes completar todos los campos.");
        }

        if (passwordNueva.length() < 6) {
            return new CambioPasswordResultado(false, "La nueva contrasena debe tener al menos 6 caracteres.");
        }

        if (!passwordNueva.equals(passwordConfirmar)) {
            return new CambioPasswordResultado(false, "La confirmacion de la contrasena no coincide.");
        }

        try (Connection connection = databaseService.openConnection()) {
            connection.setAutoCommit(false);

            try {
                TokenRecuperacion tokenRecuperacion = buscarToken(connection, token);
                if (tokenRecuperacion == null) {
                    connection.rollback();
                    return new CambioPasswordResultado(false, "El token temporal no existe.");
                }

                if (tokenRecuperacion.usado()) {
                    connection.rollback();
                    return new CambioPasswordResultado(false, "El token temporal ya fue utilizado.");
                }

                if (tokenRecuperacion.expiraEn().isBefore(LocalDateTime.now())) {
                    marcarTokenUsado(connection, tokenRecuperacion.id());
                    connection.commit();
                    return new CambioPasswordResultado(false, "El token temporal ya expiró. Solicita uno nuevo.");
                }

                actualizarPassword(connection, tokenRecuperacion.usuarioId(), passwordNueva);
                marcarTokenUsado(connection, tokenRecuperacion.id());
                invalidarOtrosTokens(connection, tokenRecuperacion.usuarioId(), tokenRecuperacion.id());
                connection.commit();

                return new CambioPasswordResultado(true, "Tu contrasena fue actualizada correctamente.");
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            logger.error("No fue posible restablecer la contrasena con token.", exception);
            return new CambioPasswordResultado(
                    false,
                    "No fue posible actualizar la contrasena: " + exception.getMessage());
        }
    }

    private UsuarioRecuperacion buscarUsuario(Connection connection,
                                              String documento,
                                              String correo,
                                              String usuarioIngresado) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_USER_FOR_RECOVERY_SQL)) {
            statement.setString(1, documento);
            statement.setString(2, correo);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                String usuarioRegistrado = limpiar(resultSet.getString("nombre_usuario")).toLowerCase(Locale.ROOT);
                if (!usuarioIngresado.isBlank() && !usuarioIngresado.equals(usuarioRegistrado)) {
                    return null;
                }

                return new UsuarioRecuperacion(
                        resultSet.getLong("id"),
                        limpiar(resultSet.getString("nombre")),
                        limpiar(resultSet.getString("apellido")),
                        limpiar(resultSet.getString("correo")),
                        limpiar(resultSet.getString("nombre_usuario"))
                );
            }
        }
    }

    private void invalidarTokensActivos(Connection connection, long usuarioId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INVALIDATE_ACTIVE_TOKENS_SQL)) {
            statement.setLong(1, usuarioId);
            statement.executeUpdate();
        }
    }

    private void guardarToken(Connection connection,
                              long usuarioId,
                              String token,
                              LocalDateTime expiracion) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_TOKEN_SQL)) {
            statement.setLong(1, usuarioId);
            statement.setString(2, token);
            statement.setTimestamp(3, Timestamp.valueOf(expiracion));
            statement.executeUpdate();
        }
    }

    private TokenRecuperacion buscarToken(Connection connection, String token) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_TOKEN_SQL)) {
            statement.setString(1, token);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new TokenRecuperacion(
                        resultSet.getLong("id"),
                        resultSet.getLong("id_usuario"),
                        resultSet.getTimestamp("expira_en").toLocalDateTime(),
                        resultSet.getBoolean("usado")
                );
            }
        }
    }

    private void actualizarPassword(Connection connection, long usuarioId, String passwordNueva) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_PASSWORD_SQL)) {
            statement.setString(1, passwordEncoder.encode(passwordNueva));
            statement.setLong(2, usuarioId);
            statement.executeUpdate();
        }
    }

    private void marcarTokenUsado(Connection connection, long tokenId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(MARK_TOKEN_USED_SQL)) {
            statement.setLong(1, tokenId);
            statement.executeUpdate();
        }
    }

    private void invalidarOtrosTokens(Connection connection, long usuarioId, long tokenId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INVALIDATE_OTHER_TOKENS_SQL)) {
            statement.setLong(1, usuarioId);
            statement.setLong(2, tokenId);
            statement.executeUpdate();
        }
    }

    private boolean enviarTokenRecuperacion(UsuarioRecuperacion usuario,
                                            String token,
                                            LocalDateTime expiracion) {
        if (passwordResetWebhookUrl.isBlank()) {
            return false;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "password_reset_token");
        payload.put("platform", "Classify");
        payload.put("nombre", usuario.nombre());
        payload.put("apellido", usuario.apellido());
        payload.put("nombre_completo", usuario.nombre() + " " + usuario.apellido());
        payload.put("correo", usuario.correo());
        payload.put("usuario", usuario.nombreUsuario());
        payload.put("token", token);
        payload.put("token_temporal", token);
        payload.put("duracion_minutos", tokenDurationMinutes);
        payload.put("expira_en", expiracion.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        payload.put("reset_url", passwordResetPageUrl);
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        try {
            restClient.post()
                    .uri(passwordResetWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            logger.warn(
                    "El webhook de recuperacion rechazo POST con estado {} y respuesta: {}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString());
        } catch (Exception exception) {
            logger.warn("Fallo el envio POST del webhook de recuperacion de contrasena.", exception);
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(passwordResetWebhookUrl);
            payload.forEach(builder::queryParam);

            restClient.get()
                    .uri(builder.encode().build().toUri())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            logger.warn(
                    "El webhook de recuperacion rechazo GET con estado {} y respuesta: {}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString());
        } catch (Exception exception) {
            logger.warn("Fallo el envio GET del webhook de recuperacion de contrasena.", exception);
        }

        return false;
    }

    private String generarToken() {
        StringBuilder builder = new StringBuilder(TOKEN_SIZE);
        for (int index = 0; index < TOKEN_SIZE; index++) {
            int position = secureRandom.nextInt(TOKEN_ALPHABET.length());
            builder.append(TOKEN_ALPHABET.charAt(position));
        }
        return builder.toString();
    }

    private String limpiar(String value) {
        return value == null ? "" : value.trim();
    }

    public record RecuperacionPasswordResultado(boolean success, String message) {
    }

    public record CambioPasswordResultado(boolean success, String message) {
    }

    private record UsuarioRecuperacion(
            long id,
            String nombre,
            String apellido,
            String correo,
            String nombreUsuario) {
    }

    private record TokenRecuperacion(
            long id,
            long usuarioId,
            LocalDateTime expiraEn,
            boolean usado) {
    }
}
