package com.classify20.dao;

import com.classify20.domain.Modulo;
import com.classify20.service.ClassifyDatabaseService;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos (JDBC) del módulo de permisos. Sigue el mismo patrón que el
 * resto de la app: conexiones tomadas de {@link ClassifyDatabaseService}.
 */
@Repository
public class PermisosDao {

    private final ClassifyDatabaseService databaseService;

    public PermisosDao(ClassifyDatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    // ── Catálogo de módulos ──────────────────────────────────────────────

    public List<Modulo> listarModulos() {
        List<Modulo> modulos = new ArrayList<>();
        String sql = "SELECT id, clave, nombre, ruta, icono, orden, en_menu, protegido " +
                "FROM modulos ORDER BY orden ASC, id ASC";
        try (Connection conn = databaseService.openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                modulos.add(mapModulo(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible listar los módulos.", e);
        }
        return modulos;
    }

    public boolean existeModulo(String clave) {
        String sql = "SELECT 1 FROM modulos WHERE clave = ? LIMIT 1";
        try (Connection conn = databaseService.openConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, clave);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible verificar el módulo " + clave, e);
        }
    }

    public void insertarModulo(Modulo m) {
        String sql = "INSERT INTO modulos (clave, nombre, ruta, icono, orden, en_menu, protegido) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = databaseService.openConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, m.clave());
            st.setString(2, m.nombre());
            st.setString(3, m.ruta());
            st.setString(4, m.icono());
            st.setInt(5, m.orden());
            st.setBoolean(6, m.enMenu());
            st.setBoolean(7, m.protegido());
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible insertar el módulo " + m.clave(), e);
        }
    }

    // ── Permisos por rol ─────────────────────────────────────────────────

    /** Devuelve, por rol, el conjunto de ids de módulo permitidos. */
    public Map<String, java.util.Set<Long>> permisosPorRol() {
        Map<String, java.util.Set<Long>> mapa = new LinkedHashMap<>();
        String sql = "SELECT rol, modulo_id FROM rol_permiso WHERE permitido = true";
        try (Connection conn = databaseService.openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                mapa.computeIfAbsent(rs.getString("rol"), k -> new java.util.HashSet<>())
                    .add(rs.getLong("modulo_id"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible leer los permisos por rol.", e);
        }
        return mapa;
    }

    public boolean existePermisoRol(String rol, long moduloId) {
        String sql = "SELECT 1 FROM rol_permiso WHERE rol = ? AND modulo_id = ? LIMIT 1";
        try (Connection conn = databaseService.openConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, rol);
            st.setLong(2, moduloId);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible verificar el permiso de rol.", e);
        }
    }

    public void upsertPermisoRol(String rol, long moduloId, boolean permitido) {
        String sql = """
                INSERT INTO rol_permiso (rol, modulo_id, permitido)
                VALUES (?, ?, ?)
                ON CONFLICT (rol, modulo_id) DO UPDATE SET permitido = EXCLUDED.permitido
                """;
        try (Connection conn = databaseService.openConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, rol);
            st.setLong(2, moduloId);
            st.setBoolean(3, permitido);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible guardar el permiso de rol.", e);
        }
    }

    // ── Overrides por usuario ────────────────────────────────────────────

    /** Devuelve, por usuarioId, el mapa moduloId → permitido (override explícito). */
    public Map<Long, Map<Long, Boolean>> overridesPorUsuario() {
        Map<Long, Map<Long, Boolean>> mapa = new LinkedHashMap<>();
        String sql = "SELECT usuario_id, modulo_id, permitido FROM usuario_permiso";
        try (Connection conn = databaseService.openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                mapa.computeIfAbsent(rs.getLong("usuario_id"), k -> new LinkedHashMap<>())
                    .put(rs.getLong("modulo_id"), rs.getBoolean("permitido"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible leer los overrides por usuario.", e);
        }
        return mapa;
    }

    public Map<Long, Boolean> overridesDeUsuario(long usuarioId) {
        Map<Long, Boolean> mapa = new LinkedHashMap<>();
        String sql = "SELECT modulo_id, permitido FROM usuario_permiso WHERE usuario_id = ?";
        try (Connection conn = databaseService.openConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setLong(1, usuarioId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getLong("modulo_id"), rs.getBoolean("permitido"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible leer los overrides del usuario.", e);
        }
        return mapa;
    }

    public void upsertOverrideUsuario(long usuarioId, long moduloId, boolean permitido) {
        String sql = """
                INSERT INTO usuario_permiso (usuario_id, modulo_id, permitido)
                VALUES (?, ?, ?)
                ON CONFLICT (usuario_id, modulo_id) DO UPDATE SET permitido = EXCLUDED.permitido
                """;
        try (Connection conn = databaseService.openConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setLong(1, usuarioId);
            st.setLong(2, moduloId);
            st.setBoolean(3, permitido);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible guardar el override de usuario.", e);
        }
    }

    /** Elimina el override → el usuario vuelve a heredar el permiso de su rol. */
    public void eliminarOverrideUsuario(long usuarioId, long moduloId) {
        String sql = "DELETE FROM usuario_permiso WHERE usuario_id = ? AND modulo_id = ?";
        try (Connection conn = databaseService.openConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setLong(1, usuarioId);
            st.setLong(2, moduloId);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible eliminar el override de usuario.", e);
        }
    }

    // ── Usuarios (para el selector de overrides) ─────────────────────────

    public List<Map<String, Object>> listarUsuarios() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, apellido, correo, nombre_usuario, tipo_usuario " +
                "FROM registro_usuarios ORDER BY tipo_usuario ASC, nombre ASC, apellido ASC";
        try (Connection conn = databaseService.openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("nombre", (safe(rs.getString("nombre")) + " " + safe(rs.getString("apellido"))).trim());
                row.put("correo", safe(rs.getString("correo")));
                row.put("usuario", safe(rs.getString("nombre_usuario")));
                row.put("rol", safe(rs.getString("tipo_usuario")));
                lista.add(row);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible listar los usuarios.", e);
        }
        return lista;
    }

    public Map<String, Object> buscarUsuario(long usuarioId) {
        String sql = "SELECT id, nombre, apellido, correo, nombre_usuario, tipo_usuario " +
                "FROM registro_usuarios WHERE id = ? LIMIT 1";
        try (Connection conn = databaseService.openConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setLong(1, usuarioId);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("nombre", (safe(rs.getString("nombre")) + " " + safe(rs.getString("apellido"))).trim());
                row.put("correo", safe(rs.getString("correo")));
                row.put("usuario", safe(rs.getString("nombre_usuario")));
                row.put("rol", safe(rs.getString("tipo_usuario")));
                return row;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible buscar el usuario " + usuarioId, e);
        }
    }

    private Modulo mapModulo(ResultSet rs) throws SQLException {
        return new Modulo(
                rs.getLong("id"),
                rs.getString("clave"),
                rs.getString("nombre"),
                rs.getString("ruta"),
                rs.getString("icono"),
                rs.getInt("orden"),
                rs.getBoolean("en_menu"),
                rs.getBoolean("protegido"));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
