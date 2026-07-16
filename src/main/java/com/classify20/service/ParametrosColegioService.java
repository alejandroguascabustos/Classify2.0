package com.classify20.service;

import com.classify20.model.ParametrosColegio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Lee y guarda los parámetros del colegio (una sola fila, id=1).
 * Son la fuente de verdad para el formulario de registro y la validación del Excel.
 */
@Service
public class ParametrosColegioService {

    private static final Logger logger = LoggerFactory.getLogger(ParametrosColegioService.class);

    private static final ParametrosColegio POR_DEFECTO = new ParametrosColegio(
            11, 4,
            "Matematicas,Español,Sociales,Historia,Ingles,Etica y valores,Educación fisica,Informatica");

    private final ClassifyDatabaseService databaseService;

    public ParametrosColegioService(ClassifyDatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public ParametrosColegio obtener() {
        String sql = "SELECT num_grados, num_grupos, materias FROM parametros_colegio WHERE id = 1";
        try (Connection conn = databaseService.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new ParametrosColegio(
                        rs.getInt("num_grados"),
                        rs.getInt("num_grupos"),
                        rs.getString("materias"));
            }
        } catch (SQLException e) {
            logger.warn("No se pudieron leer los parámetros del colegio, se usan los valores por defecto: {}", e.getMessage());
        }
        return POR_DEFECTO;
    }

    /** Resultado de guardar los parámetros. */
    public record Resultado(boolean exito, String mensaje) {}

    public Resultado guardar(int numGrados, int numGrupos, String materiasCsv, String actualizadoPor) {
        if (numGrados < 1 || numGrados > 20) {
            return new Resultado(false, "El número de grados debe estar entre 1 y 20.");
        }
        if (numGrupos < 1 || numGrupos > 26) {
            return new Resultado(false, "El número de grupos debe estar entre 1 y 26.");
        }
        String materias = materiasCsv == null ? "" : materiasCsv.trim();
        if (materias.isBlank()) {
            return new Resultado(false, "Debes indicar al menos una materia.");
        }

        // Upsert: crea la fila única (id=1) si aún no existe.
        String sql = "INSERT INTO parametros_colegio (id, num_grados, num_grupos, materias, actualizado_en, actualizado_por) " +
                "VALUES (1, ?, ?, ?, CURRENT_TIMESTAMP, ?) " +
                "ON CONFLICT (id) DO UPDATE SET num_grados = EXCLUDED.num_grados, num_grupos = EXCLUDED.num_grupos, " +
                "materias = EXCLUDED.materias, actualizado_en = CURRENT_TIMESTAMP, actualizado_por = EXCLUDED.actualizado_por";
        try (Connection conn = databaseService.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numGrados);
            ps.setInt(2, numGrupos);
            ps.setString(3, materias);
            ps.setString(4, actualizadoPor);
            ps.executeUpdate();
            return new Resultado(true, "Parámetros del colegio actualizados correctamente.");
        } catch (SQLException e) {
            logger.error("Error guardando parámetros", e);
            return new Resultado(false, "Error al guardar los parámetros: " + e.getMessage());
        }
    }
}
