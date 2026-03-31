package com.classify20.service;

import com.classify20.config.ClassifyDatabaseProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ClassifyDatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(ClassifyDatabaseService.class);
    private static final String SCHEMA_SQL_RESOURCE = "classpath:db/classify_db.sql";

    private static final String FIND_BY_USERNAME_SQL = """
            SELECT id
            FROM registro_usuarios
            WHERE nombre_usuario = ?
            LIMIT 1
            """;

    private static final String INSERT_USER_SQL = """
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

    private static final String UPDATE_USER_SQL = """
            UPDATE registro_usuarios
            SET nombre = ?,
                apellido = ?,
                correo = ?,
                documento = ?,
                telefono = ?,
                pass_hash = ?,
                tipo_usuario = ?,
                curso = ?,
                materia = ?,
                nombre_estudiante = ?,
                codigo_docente_asignado = ?,
                codigo_docente_referencia = ?,
                creado_en = ?
            WHERE nombre_usuario = ?
            """;

    private static final List<SeedUser> TEST_USERS = List.of(
            new SeedUser("Admin", "Test", "admin@classify.local", "TEST-ADM-BASE", "3000000001",
                    "admin", "administrador", null, null, null, null, null),
            new SeedUser("Docente", "Test", "docente@classify.local", "TEST-DOC-BASE", "3000000002",
                    "docente", "docente", null, "Ciencias Naturales", null, "DOC-BASE01", null),
            new SeedUser("Estudiante", "Test", "estudiante@classify.local", "TEST-EST-BASE", "3000000003",
                    "estudiante", "estudiante", "7A", null, null, null, null),
            new SeedUser("Acudiente", "Test", "acudiente@classify.local", "TEST-ACU-BASE", "3000000004",
                    "acudiente", "acudiente", null, null, "Estudiante Demo", null, null)
    );

    private final ClassifyDatabaseProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final String testPasswordHash;
    private final List<String> schemaStatements;

    private volatile ConnectionSettings activeSettings;

    public ClassifyDatabaseService(
            ClassifyDatabaseProperties properties,
            ResourceLoader resourceLoader) {
        this.properties = properties;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.testPasswordHash = passwordEncoder.encode("12345");
        this.schemaStatements = cargarSentenciasSql(resourceLoader.getResource(SCHEMA_SQL_RESOURCE));
    }

    @PostConstruct
    void initialize() {
        try {
            ensureReady();
        } catch (SQLException exception) {
            logger.error("No fue posible inicializar la base de datos de Classify.", exception);
        }
    }

    public Connection openConnection() throws SQLException {
        try {
            ConnectionSettings settings = ensureReady();
            Connection connection = DriverManager.getConnection(settings.url(), settings.username(), settings.password());
            ensureSchemaAndSeed(connection);
            return connection;
        } catch (SQLException primaryException) {
            if (activeSettings == null || activeSettings.fallback()) {
                throw primaryException;
            }

            logger.warn("Fallo la conexion a la base primaria. Se intentara la base embebida.", primaryException);
            synchronized (this) {
                activeSettings = fallbackSettings();
            }

            ConnectionSettings fallback = ensureReady();
            Connection connection = DriverManager.getConnection(fallback.url(), fallback.username(), fallback.password());
            ensureSchemaAndSeed(connection);
            return connection;
        }
    }

    public synchronized ConnectionSettings ensureReady() throws SQLException {
        if (activeSettings != null) {
            return activeSettings;
        }

        ConnectionSettings primary = primarySettings();

        try (Connection connection = DriverManager.getConnection(primary.url(), primary.username(), primary.password())) {
            ensureSchemaAndSeed(connection);
            activeSettings = primary;
            logger.info("Classify usara la base de datos principal: {}", activeSettings.url());
            return activeSettings;
        } catch (SQLException exception) {
            logger.warn("No fue posible conectar con la base primaria {}. Se utilizara una base embebida local.",
                    primary.url());
            ConnectionSettings fallback = fallbackSettings();
            try (Connection connection = DriverManager.getConnection(
                    fallback.url(),
                    fallback.username(),
                    fallback.password())) {
                ensureSchemaAndSeed(connection);
                activeSettings = fallback;
                logger.info("Classify usara la base de datos embebida: {}", activeSettings.url());
                return activeSettings;
            }
        }
    }

    private void ensureSchemaAndSeed(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : schemaStatements) {
                statement.execute(sql);
            }
        }

        for (SeedUser user : TEST_USERS) {
            upsertUser(connection, user);
        }
    }

    private void upsertUser(Connection connection, SeedUser user) throws SQLException {
        boolean exists;

        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_USERNAME_SQL)) {
            statement.setString(1, user.username());
            try (ResultSet resultSet = statement.executeQuery()) {
                exists = resultSet.next();
            }
        }

        if (exists) {
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_USER_SQL)) {
                statement.setString(1, user.name());
                statement.setString(2, user.lastName());
                statement.setString(3, user.email());
                statement.setString(4, user.document());
                statement.setString(5, user.phone());
                statement.setString(6, testPasswordHash);
                statement.setString(7, user.type());
                statement.setString(8, user.course());
                statement.setString(9, user.subject());
                statement.setString(10, user.studentName());
                statement.setString(11, user.teacherCode());
                statement.setString(12, user.teacherReference());
                statement.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
                statement.setString(14, user.username());
                statement.executeUpdate();
            }
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_USER_SQL)) {
            statement.setString(1, user.name());
            statement.setString(2, user.lastName());
            statement.setString(3, user.email());
            statement.setString(4, user.document());
            statement.setString(5, user.phone());
            statement.setString(6, user.username());
            statement.setString(7, testPasswordHash);
            statement.setString(8, user.type());
            statement.setString(9, user.course());
            statement.setString(10, user.subject());
            statement.setString(11, user.studentName());
            statement.setString(12, user.teacherCode());
            statement.setString(13, user.teacherReference());
            statement.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        }
    }

    private ConnectionSettings primarySettings() {
        return new ConnectionSettings(
                properties.getUrl(),
                properties.getUsername(),
                properties.getPassword(),
                false
        );
    }

    private ConnectionSettings fallbackSettings() {
        return new ConnectionSettings(
                properties.getFallbackUrl(),
                properties.getFallbackUsername(),
                properties.getFallbackPassword(),
                true
        );
    }

    private List<String> cargarSentenciasSql(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return Arrays.stream(sql.split(";"))
                    .map(this::limpiarBloqueSql)
                    .filter(statement -> !statement.isBlank())
                    .filter(this::esSentenciaDeEsquema)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible cargar el esquema unificado de la base de datos.", exception);
        }
    }

    private String limpiarBloqueSql(String block) {
        return Arrays.stream(block.split("\\R"))
                .map(String::stripTrailing)
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private boolean esSentenciaDeEsquema(String statement) {
        String normalized = statement.stripLeading().toUpperCase(Locale.ROOT);
        return normalized.startsWith("CREATE TABLE")
                || normalized.startsWith("CREATE INDEX")
                || normalized.startsWith("ALTER TABLE");
    }

    public record ConnectionSettings(String url, String username, String password, boolean fallback) {
    }

    private record SeedUser(
            String name,
            String lastName,
            String email,
            String document,
            String phone,
            String username,
            String type,
            String course,
            String subject,
            String studentName,
            String teacherCode,
            String teacherReference) {
    }
}
