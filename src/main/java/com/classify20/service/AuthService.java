package com.classify20.service;

import com.classify20.model.LoginResultado;
import com.classify20.model.SesionUsuario;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

@Service
public class AuthService {

    private static final String LOGIN_SQL = """
            SELECT id,
                   nombre,
                   apellido,
                   correo,
                   nombre_usuario,
                   pass_hash,
                   tipo_usuario,
                   materia
            FROM registro_usuarios
            WHERE LOWER(nombre_usuario) = ? OR LOWER(correo) = ?
            LIMIT 1
            """;

    private final PasswordEncoder passwordEncoder;
    private final ClassifyDatabaseService databaseService;

    public AuthService(ClassifyDatabaseService databaseService) {
        this.databaseService = databaseService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public LoginResultado autenticar(String usuarioIngresado, String passwordIngresada) {
        String usuarioNormalizado = limpiar(usuarioIngresado).toLowerCase(Locale.ROOT);
        String passwordNormalizada = passwordIngresada == null ? "" : passwordIngresada.trim();

        if (usuarioNormalizado.isBlank() || passwordNormalizada.isBlank()) {
            return new LoginResultado(false, "Debes ingresar tu usuario y tu contrasena.", null);
        }

        try (Connection connection = databaseService.openConnection();
             PreparedStatement statement = connection.prepareStatement(LOGIN_SQL)) {

            statement.setString(1, usuarioNormalizado);
            statement.setString(2, usuarioNormalizado);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new LoginResultado(false, "El usuario no existe en la base de datos.", null);
                }

                String passwordHash = resultSet.getString("pass_hash");
                if (!passwordEncoder.matches(passwordNormalizada, passwordHash)) {
                    return new LoginResultado(false, "La contrasena es incorrecta.", null);
                }

                String tipoUsuario = limpiar(resultSet.getString("tipo_usuario")).toLowerCase(Locale.ROOT);
                SesionUsuario usuario = new SesionUsuario(
                        resultSet.getLong("id"),
                        limpiar(resultSet.getString("nombre")),
                        limpiar(resultSet.getString("apellido")),
                        limpiar(resultSet.getString("correo")),
                        limpiar(resultSet.getString("nombre_usuario")),
                        tipoUsuario,
                        mapearPerfil(tipoUsuario),
                        limpiar(resultSet.getString("materia"))
                );

                return new LoginResultado(true, "Inicio de sesion correcto.", usuario);
            }
        } catch (SQLException exception) {
            return new LoginResultado(false,
                    "No fue posible validar el inicio de sesion en PostgreSQL: " + exception.getMessage(),
                    null);
        }
    }

    private int mapearPerfil(String tipoUsuario) {
        return switch (tipoUsuario) {
            case "administrador" -> 1;
            case "coordinador" -> 2;
            case "docente" -> 3;
            case "acudiente" -> 4;
            case "estudiante" -> 5;
            default -> 0;
        };
    }

    private String limpiar(String value) {
        return value == null ? "" : value.trim();
    }
}
