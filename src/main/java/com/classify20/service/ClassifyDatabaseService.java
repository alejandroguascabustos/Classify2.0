package com.classify20.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
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
                password_temporal,
                creado_en
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                password_temporal = ?,
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

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final String testPasswordHash;
    private final List<String> schemaStatements;

    public ClassifyDatabaseService(
            DataSource dataSource,
            ResourceLoader resourceLoader) {
        this.dataSource = dataSource;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.testPasswordHash = passwordEncoder.encode("12345");
        this.schemaStatements = cargarSentenciasSql(resourceLoader.getResource(SCHEMA_SQL_RESOURCE));
    }

    @PostConstruct
    void initialize() {
        try {
            ensureSchemaAndSeed();
        } catch (SQLException exception) {
            logger.error("No fue posible inicializar el esquema de la base de datos.", exception);
        }
    }

    public Connection openConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void ensureSchemaAndSeed() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : schemaStatements) {
                statement.execute(sql);
            }
            
            for (SeedUser user : TEST_USERS) {
                upsertUser(connection, user);
            }
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
                statement.setBoolean(13, false);
                statement.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
                statement.setString(15, user.username());
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
            statement.setBoolean(14, false);
            statement.setTimestamp(15, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        }
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
