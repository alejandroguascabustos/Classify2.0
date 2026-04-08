package com.classify20.service;

import com.classify20.model.RegistroForm;
import com.classify20.model.RegistroResultado;
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
public class RegistroService {

    private static final Logger logger = LoggerFactory.getLogger(RegistroService.class);

    private static final String CODE_PREFIX = "DOC-";
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_SIZE = 8;
    private static final String TELEFONO_REGEX = "\\d{7,10}";

    private final RestClient restClient;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final ClassifyDatabaseService databaseService;
    private final String docenteWebhookUrl;
    private final String welcomeWebhookUrl;

    public RegistroService(
            ClassifyDatabaseService databaseService,
            @Value("${classify.webhooks.registro-docente.url:https://n8n.classify.in.net/webhook/bbab4100-3e6e-44cd-98e6-f62e6d3f65af}") String docenteWebhookUrl,
            @Value("${classify.webhooks.welcome.url:https://n8n.classify.in.net/webhook/a54058de-22a2-488b-956c-21bc5520d6a0}") String welcomeWebhookUrl) {
        this.databaseService = databaseService;
        this.docenteWebhookUrl = docenteWebhookUrl;
        this.welcomeWebhookUrl = welcomeWebhookUrl;
        this.restClient = RestClient.builder().build();
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
    }

    public RegistroResultado registrar(RegistroForm form) {
        RegistroNormalizado registro = normalizar(form);
        validar(registro);

        String codigoDocenteAsignado = null;
        String materiaFinal = registro.materia();
        WebhookResultado webhookResultado = null;

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            try {
                validarDuplicados(connection, registro);

                if ("docente".equals(registro.tipoUsuario())) {
                    codigoDocenteAsignado = generarCodigoDocente(connection);
                }

                insertarRegistro(connection, registro, materiaFinal, codigoDocenteAsignado);

                if ("docente".equals(registro.tipoUsuario())) {
                    webhookResultado = enviarCodigoDocente(registro, codigoDocenteAsignado, materiaFinal);
                } else {
                    enviarWebhookBienvenida(registro); 
                }

                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (RegistroException exception) {
            return new RegistroResultado(false, exception.getMessage());
        } catch (SQLException exception) {
            return new RegistroResultado(false, "No fue posible guardar el registro en la base de datos de Classify: " + exception.getMessage());
        }

        if ("docente".equals(registro.tipoUsuario())) {
            String detalleMetodo = webhookResultado == null || webhookResultado.metodo().isBlank()
                    ? ""
                    : " mediante " + webhookResultado.metodo();
            return new RegistroResultado(true,
                    "Docente registrado correctamente. Se envio un codigo unico al correo institucional" + detalleMetodo + ".");
        }

        if ("estudiante".equals(registro.tipoUsuario())) {
            return new RegistroResultado(true, "Estudiante registrado correctamente.");
        }

        return new RegistroResultado(true, "Registro completado correctamente.");
    }

    private Connection openConnection() throws SQLException {
        return databaseService.openConnection();
    }

    private void validarDuplicados(Connection connection, RegistroNormalizado registro) throws SQLException {
        validarCampoUnico(connection, "correo", registro.correo(), "Ya existe un usuario registrado con ese correo.");
        validarCampoUnico(connection, "documento", registro.documento(), "Ya existe un usuario registrado con ese documento.");
        validarCampoUnico(connection, "nombre_usuario", registro.nombreUsuario(), "Ese nombre de usuario ya esta en uso.");
    }

    private void validarCampoUnico(Connection connection, String column, String value, String message) throws SQLException {
        String sql = "SELECT 1 FROM registro_usuarios WHERE " + column + " = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new RegistroException(message);
                }
            }
        }
    }

    private DocenteRelacionado buscarDocentePorCodigo(Connection connection, String codigoDocente) throws SQLException {
        String sql = """
                SELECT materia
                FROM registro_usuarios
                WHERE tipo_usuario = 'docente' AND codigo_docente_asignado = ?
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, codigoDocente);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new DocenteRelacionado(resultSet.getString("materia"));
            }
        }
    }

    private void insertarRegistro(Connection connection,
                                  RegistroNormalizado registro,
                                  String materiaFinal,
                                  String codigoDocenteAsignado) throws SQLException {
        String sql = """
                INSERT INTO registro_usuarios (
                    nombre,
                    apellido,
                    correo,
                    documento,
                    telefono,
                    nombre_usuario,
                    pass_hash,
                    tipo_usuario,
                    curso,
                    materia,
                    nombre_estudiante,
                    codigo_docente_asignado,
                    codigo_docente_referencia,
                    creado_en
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, registro.nombre());
            statement.setString(2, registro.apellido());
            statement.setString(3, registro.correo());
            statement.setString(4, registro.documento());
            statement.setString(5, registro.telefono());
            statement.setString(6, registro.nombreUsuario());
            statement.setString(7, passwordEncoder.encode(registro.password()));
            statement.setString(8, registro.tipoUsuario());
            statement.setString(9, vacioANull(registro.curso()));
            statement.setString(10, vacioANull(materiaFinal));
            statement.setString(11, vacioANull(registro.nombreEstudiante()));
            statement.setString(12, vacioANull(codigoDocenteAsignado));
            statement.setString(13, vacioANull(registro.codigoDocente()));
            statement.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        }
    }

    private String generarCodigoDocente(Connection connection) throws SQLException {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = CODE_PREFIX + randomChunk(CODE_SIZE);
            if (!codigoExiste(connection, code)) {
                return code;
            }
        }
        throw new RegistroException("No fue posible generar un codigo unico para el docente.");
    }

    private boolean codigoExiste(Connection connection, String code) throws SQLException {
        String sql = "SELECT 1 FROM registro_usuarios WHERE codigo_docente_asignado = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String randomChunk(int size) {
        StringBuilder builder = new StringBuilder(size);
        for (int index = 0; index < size; index++) {
            int position = secureRandom.nextInt(CODE_ALPHABET.length());
            builder.append(CODE_ALPHABET.charAt(position));
        }
        return builder.toString();
    }

    private void enviarWebhookBienvenida(RegistroNormalizado registro) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "welcome_email");
        payload.put("platform", "Classify");
        payload.put("tipo_usuario", registro.tipoUsuario());
        payload.put("nombre", registro.nombre());
        payload.put("apellido", registro.apellido());
        payload.put("nombre_completo", registro.nombre() + " " + registro.apellido());
        payload.put("correo", registro.correo());
        payload.put("usuario", registro.nombreUsuario());
        payload.put("source", "registro_app");
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            restClient.post()
                    .uri(docenteWebhookUrl) // Usamos la URL de registro proporcionada por el usuario
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            logger.warn("Fallo el envio al webhook de prueba durante el registro.", exception);
        }
    }

    private WebhookResultado enviarCodigoDocente(RegistroNormalizado registro, String codigoDocente, String materiaFinal) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "registro_docente_codigo");
        payload.put("platform", "Classify");
        payload.put("tipo_usuario", registro.tipoUsuario());
        payload.put("tipoUsuario", registro.tipoUsuario());
        payload.put("nombre", registro.nombre());
        payload.put("apellido", registro.apellido());
        payload.put("nombre_completo", registro.nombre() + " " + registro.apellido());
        payload.put("correo", registro.correo());
        payload.put("usuario", registro.nombreUsuario());
        payload.put("materia", materiaFinal);
        payload.put("codigo_docente", codigoDocente);
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        try {
            restClient.post()
                    .uri(docenteWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return new WebhookResultado(true, "POST");
        } catch (RestClientResponseException exception) {
            logger.warn("El webhook de registro docente rechazo POST con estado {} y respuesta: {}",
                    exception.getStatusCode(), exception.getResponseBodyAsString());
        } catch (Exception exception) {
            logger.warn("Fallo el envio POST al webhook de registro docente.", exception);
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(docenteWebhookUrl);
            payload.forEach(builder::queryParam);

            restClient.get()
                    .uri(builder.encode().build().toUri())
                    .retrieve()
                    .toBodilessEntity();
            return new WebhookResultado(true, "GET");
        } catch (RestClientResponseException exception) {
            logger.error("El webhook de registro docente rechazo GET con estado {} y respuesta: {}",
                    exception.getStatusCode(), exception.getResponseBodyAsString());
        } catch (Exception exception) {
            logger.error("Fallo el envio GET al webhook de registro docente.", exception);
        }

        return new WebhookResultado(false, "");
    }

    private RegistroNormalizado normalizar(RegistroForm form) {
        return new RegistroNormalizado(
                limpiar(form.getNombre()),
                limpiar(form.getApellido()),
                limpiar(form.getCorreo()).toLowerCase(Locale.ROOT),
                limpiar(form.getDocumento()),
                limpiar(form.getTelefono()),
                limpiar(form.getNombre_usu()),
                form.getPass_usuario() == null ? "" : form.getPass_usuario().trim(),
                limpiar(form.getTipo_usuario()).toLowerCase(Locale.ROOT),
                limpiar(form.getCurso()),
                limpiar(form.getMateria()),
                limpiar(form.getNombre_estudiante()),
                limpiar(form.getCodigo_docente()).toUpperCase(Locale.ROOT)
        );
    }

    private void validar(RegistroNormalizado registro) {
        if (registro.nombre().isBlank() || registro.apellido().isBlank() || registro.correo().isBlank()
                || registro.documento().isBlank() || registro.telefono().isBlank()
                || registro.nombreUsuario().isBlank() || registro.password().isBlank()
                || registro.tipoUsuario().isBlank()) {
            throw new RegistroException("Debes completar todos los campos obligatorios del formulario.");
        }

        if (registro.password().length() < 6) {
            throw new RegistroException("La contrasena debe tener minimo 6 caracteres.");
        }

        if (!registro.correo().contains("@")) {
            throw new RegistroException("Debes ingresar un correo valido.");
        }

        if (!registro.telefono().matches(TELEFONO_REGEX)) {
            throw new RegistroException("El telefono debe tener entre 7 y 10 digitos numericos.");
        }

        switch (registro.tipoUsuario()) {
            case "estudiante" -> {
                if (registro.curso().isBlank()) {
                    throw new RegistroException("El curso es obligatorio para estudiantes.");
                }
            }
            case "docente" -> {
                if (registro.materia().isBlank()) {
                    throw new RegistroException("La materia es obligatoria para docentes.");
                }
            }
            case "acudiente" -> {
                if (registro.nombreEstudiante().isBlank()) {
                    throw new RegistroException("El nombre del estudiante es obligatorio para acudientes.");
                }
            }
            default -> throw new RegistroException("Selecciona un tipo de usuario valido.");
        }
    }

    private String limpiar(String value) {
        return value == null ? "" : value.trim();
    }

    private String vacioANull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record RegistroNormalizado(
            String nombre,
            String apellido,
            String correo,
            String documento,
            String telefono,
            String nombreUsuario,
            String password,
            String tipoUsuario,
            String curso,
            String materia,
            String nombreEstudiante,
            String codigoDocente) {
    }

    private record DocenteRelacionado(String materia) {
    }

    private record WebhookResultado(boolean enviado, String metodo) {
    }

    private static class RegistroException extends RuntimeException {
        RegistroException(String message) {
            super(message);
        }
    }
}
