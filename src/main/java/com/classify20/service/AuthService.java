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
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    /**
     * Se incluye debe_cambiar_password: sin este campo el cambio de
     * contrasena obligatorio (cuentas creadas con clave temporal por
     * ActivacionService) nunca se aplicaba porque el login no sabia que la
     * cuenta seguia con esa clave.
     */
    private static final String LOGIN_SQL = """
            SELECT id,
                   nombre,
                   apellido,
                   correo,
                   nombre_usuario,
                   pass_hash,
                   tipo_usuario,
                   materia,
                   debe_cambiar_password
            FROM registro_usuarios
            WHERE LOWER(nombre_usuario) = ? OR LOWER(correo) = ?
            LIMIT 1
            """;

    // ── Bloqueo de fuerza bruta ────────────────────────────────────
    /** Fallos consecutivos tolerados antes de bloquear la cuenta. */
    private static final int MAX_INTENTOS = 5;
    /** Cuanto dura el bloqueo una vez superado el limite. */
    private static final Duration BLOQUEO = Duration.ofMinutes(15);
    /** Tras este tiempo sin fallos, el contador se olvida solo. */
    private static final Duration VENTANA = Duration.ofMinutes(15);

    private record Intentos(int fallos, Instant ultimoFallo) {}

    /**
     * Contador en memoria por identificador de usuario. Suficiente para una
     * sola instancia como la actual. Si algun dia la app corre replicada
     * detras de un balanceador, esto debe moverse a la base de datos o a
     * Redis, porque cada replica tendria su propio mapa.
     */
    private final Map<String, Intentos> intentosPorUsuario = new ConcurrentHashMap<>();

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

        long minutosRestantes = minutosDeBloqueo(usuarioNormalizado);
        if (minutosRestantes > 0) {
            return new LoginResultado(false,
                    "Demasiados intentos fallidos. Intenta de nuevo en " + minutosRestantes + " minuto(s).",
                    null);
        }

        try (Connection connection = databaseService.openConnection();
             PreparedStatement statement = connection.prepareStatement(LOGIN_SQL)) {

            statement.setString(1, usuarioNormalizado);
            statement.setString(2, usuarioNormalizado);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    registrarFallo(usuarioNormalizado);
                    // Mensaje identico al de contrasena incorrecta: decir
                    // "usuario no registrado" permitiria enumerar cuentas
                    // validas probando correos uno por uno.
                    return new LoginResultado(false, "Usuario o contrasena incorrectos.", null);
                }

                String passwordHash = resultSet.getString("pass_hash");
                if (!passwordEncoder.matches(passwordNormalizada, passwordHash)) {
                    registrarFallo(usuarioNormalizado);
                    return new LoginResultado(false, "Usuario o contrasena incorrectos.", null);
                }

                // Login correcto: se limpia el contador de fallos.
                intentosPorUsuario.remove(usuarioNormalizado);

                String tipoUsuario = limpiar(resultSet.getString("tipo_usuario")).toLowerCase(Locale.ROOT);
                SesionUsuario usuario = new SesionUsuario(
                        resultSet.getLong("id"),
                        limpiar(resultSet.getString("nombre")),
                        limpiar(resultSet.getString("apellido")),
                        limpiar(resultSet.getString("correo")),
                        limpiar(resultSet.getString("nombre_usuario")),
                        tipoUsuario,
                        mapearPerfil(tipoUsuario),
                        limpiar(resultSet.getString("materia")),
                        resultSet.getBoolean("debe_cambiar_password")
                );

                return new LoginResultado(true, "Inicio de sesion correcto.", usuario);
            }
        } catch (SQLException exception) {
            // El detalle tecnico va al log, no a la pantalla: exponer el
            // mensaje de PostgreSQL revela nombres de tablas y columnas.
            org.slf4j.LoggerFactory.getLogger(AuthService.class)
                    .error("Fallo la consulta de login.", exception);
            return new LoginResultado(false,
                    "No fue posible validar el inicio de sesion. Intenta de nuevo en unos minutos.",
                    null);
        }
    }

    /**
     * Cambia la contrasena de un usuario autenticado y baja las banderas de
     * cambio obligatorio. Se usa desde el flujo de cambio forzoso.
     *
     * @return true si la contrasena actual coincidia y el cambio se aplico.
     */
    public boolean cambiarPassword(long usuarioId, String passwordActual, String passwordNueva) {
        String sqlLeer = "SELECT pass_hash FROM registro_usuarios WHERE id = ?";
        String sqlActualizar = """
                UPDATE registro_usuarios
                   SET pass_hash = ?,
                       debe_cambiar_password = FALSE,
                       password_temporal = FALSE
                 WHERE id = ?
                """;

        try (Connection conn = databaseService.openConnection()) {
            String hashActual;
            try (PreparedStatement ps = conn.prepareStatement(sqlLeer)) {
                ps.setLong(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    hashActual = rs.getString("pass_hash");
                }
            }

            if (!passwordEncoder.matches(passwordActual, hashActual)) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlActualizar)) {
                ps.setString(1, passwordEncoder.encode(passwordNueva));
                ps.setLong(2, usuarioId);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            org.slf4j.LoggerFactory.getLogger(AuthService.class)
                    .error("Fallo el cambio de contrasena del usuario {}.", usuarioId, e);
            return false;
        }
    }

    // ── Utilidades de bloqueo por fuerza bruta ─────────────────────

    private long minutosDeBloqueo(String usuario) {
        Intentos i = intentosPorUsuario.get(usuario);
        if (i == null || i.fallos() < MAX_INTENTOS) return 0;

        Duration transcurrido = Duration.between(i.ultimoFallo(), Instant.now());
        if (transcurrido.compareTo(BLOQUEO) >= 0) {
            intentosPorUsuario.remove(usuario);
            return 0;
        }
        return Math.max(1, BLOQUEO.minus(transcurrido).toMinutes());
    }

    private void registrarFallo(String usuario) {
        intentosPorUsuario.compute(usuario, (clave, previo) -> {
            Instant ahora = Instant.now();
            if (previo == null || Duration.between(previo.ultimoFallo(), ahora).compareTo(VENTANA) > 0) {
                return new Intentos(1, ahora);
            }
            return new Intentos(previo.fallos() + 1, ahora);
        });
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
