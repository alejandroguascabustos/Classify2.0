package com.classify20.service;

import com.classify20.model.Agenda;
import com.classify20.repository.AgendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AgendaService {

    private final AgendaRepository agendaRepository;

    /**
     * Verifica que la nueva agenda no choque con ninguna existente.
     * Reglas:
     *   1. Un salón (grado + grupo) no puede tener dos clases superpuestas.
     *   2. Un profesor no puede estar en dos salones al mismo tiempo.
     *
     * @param nueva     agenda a guardar/actualizar
     * @param excludeId ID de la agenda que se está editando (null para nueva)
     */
    private void validarConflictos(Agenda nueva, Long excludeId) {
        LocalTime nuevaInicio = nueva.getHoraInicio();
        int durMin = nueva.getDuracion() != null ? nueva.getDuracion() : 60;
        LocalTime nuevaFin = nuevaInicio.plusMinutes(durMin);

        String grupoNuevo = nueva.getGrupo() != null ? nueva.getGrupo().trim() : "";

        // ── Conflicto 1: mismo salón (grado + grupo) ──────────────────────
        List<Agenda> mismoGrado = agendaRepository.findByFechaAndGrado(nueva.getFecha(), nueva.getGrado());
        for (Agenda ex : mismoGrado) {
            if (excludeId != null && Objects.equals(ex.getId(), excludeId)) continue;

            String grupoEx = ex.getGrupo() != null ? ex.getGrupo().trim() : "";
            if (!grupoNuevo.equalsIgnoreCase(grupoEx)) continue; // diferente grupo → no hay conflicto de salón

            LocalTime exFin = ex.getHoraInicio().plusMinutes(ex.getDuracion() != null ? ex.getDuracion() : 60);
            if (nuevaInicio.isBefore(exFin) && nuevaFin.isAfter(ex.getHoraInicio())) {
                String salon = nueva.getGrado() + "°" + (grupoNuevo.isEmpty() ? "" : " " + grupoNuevo.toUpperCase());
                throw new IllegalStateException(
                    "El salón " + salon + " ya tiene una clase programada de " +
                    ex.getHoraInicio() + " a " + exFin +
                    " (" + ex.getMateria() + " – " + ex.getProfesor() + ")."
                );
            }
        }

        // ── Conflicto 2: mismo profesor en cualquier salón ─────────────────
        List<Agenda> mismoProfesor = agendaRepository.findByFechaAndProfesor(nueva.getFecha(), nueva.getProfesor());
        for (Agenda ex : mismoProfesor) {
            if (excludeId != null && Objects.equals(ex.getId(), excludeId)) continue;

            LocalTime exFin = ex.getHoraInicio().plusMinutes(ex.getDuracion() != null ? ex.getDuracion() : 60);
            if (nuevaInicio.isBefore(exFin) && nuevaFin.isAfter(ex.getHoraInicio())) {
                String salonEx = ex.getGrado() + "°" + (ex.getGrupo() != null && !ex.getGrupo().isBlank() ? " " + ex.getGrupo().toUpperCase() : "");
                throw new IllegalStateException(
                    "El profesor " + nueva.getProfesor() + " ya tiene clase en el salón " + salonEx +
                    " de " + ex.getHoraInicio() + " a " + exFin + "."
                );
            }
        }
    }

    /** Guardar una nueva agenda (con validación de conflictos) */
    public Agenda guardarAgenda(Agenda agenda) {
        validarConflictos(agenda, null);
        return agendaRepository.save(agenda);
    }

    /** Listar todas las agendas */
    public List<Agenda> listarAgendas() {
        return agendaRepository.findAll();
    }

    // ── Vista de clases agendadas (CLS-122) ─────────────────────────────

    /** Opción del desplegable de curso: valor "grado|grupo" y etiqueta "3° A". */
    public record CursoOption(String valor, String etiqueta) {}

    /** Grupo normalizado para comparar/agrupar ("a " y "A" son el mismo curso). */
    private static String normalizarGrupo(String grupo) {
        return grupo == null ? "" : grupo.trim().toUpperCase();
    }

    /**
     * Clases agendadas que cumplen los filtros seleccionados, ordenadas
     * cronológicamente. Cualquier filtro vacío o nulo se ignora.
     *
     * @param curso    formato "grado|grupo" (grupo puede ser vacío), tal como
     *                 lo produce {@link #listarCursos()}
     * @param profesor nombre exacto del profesor
     * @param materia  nombre exacto de la materia
     */
    public List<Agenda> filtrarClases(String curso, String profesor, String materia) {
        Integer grado = null;
        String grupo = null;
        if (curso != null && !curso.isBlank()) {
            String[] partes = curso.split("\\|", -1);
            try {
                grado = Integer.valueOf(partes[0].trim());
            } catch (NumberFormatException e) {
                // curso mal formado → se ignora el filtro
            }
            grupo = partes.length > 1 ? normalizarGrupo(partes[1]) : "";
        }
        final Integer fGrado = grado;
        final String fGrupo = grupo;

        return agendaRepository.findAll().stream()
                .filter(a -> fGrado == null
                        || (Objects.equals(a.getGrado(), fGrado) && normalizarGrupo(a.getGrupo()).equals(fGrupo)))
                .filter(a -> profesor == null || profesor.isBlank()
                        || (a.getProfesor() != null && profesor.trim().equalsIgnoreCase(a.getProfesor().trim())))
                .filter(a -> materia == null || materia.isBlank()
                        || (a.getMateria() != null && materia.trim().equalsIgnoreCase(a.getMateria().trim())))
                .sorted(Comparator.comparing(Agenda::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Agenda::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Cursos (grado + grupo) con al menos una clase agendada, sin duplicados. */
    public List<CursoOption> listarCursos() {
        Map<String, CursoOption> unicos = new LinkedHashMap<>();
        agendaRepository.findAll().stream()
                .filter(a -> a.getGrado() != null)
                .sorted(Comparator.comparing(Agenda::getGrado)
                        .thenComparing(a -> normalizarGrupo(a.getGrupo())))
                .forEach(a -> {
                    String grupo = normalizarGrupo(a.getGrupo());
                    String valor = a.getGrado() + "|" + grupo;
                    String etiqueta = a.getGrado() + "°" + (grupo.isEmpty() ? "" : " " + grupo);
                    unicos.putIfAbsent(valor, new CursoOption(valor, etiqueta));
                });
        return List.copyOf(unicos.values());
    }

    /** Profesores con clases agendadas, sin duplicados y en orden alfabético. */
    public List<String> listarProfesores() {
        return listarDistintos(Agenda::getProfesor);
    }

    /** Materias con clases agendadas, sin duplicados y en orden alfabético. */
    public List<String> listarMaterias() {
        return listarDistintos(Agenda::getMateria);
    }

    /** Clases dentro de un rango de fechas, en orden cronológico. */
    public List<Agenda> clasesEntre(java.time.LocalDate desde, java.time.LocalDate hasta) {
        return agendaRepository.findByFechaBetweenOrderByFechaAscHoraInicioAsc(desde, hasta);
    }

    /** Etiqueta de curso de una agenda (ej. "3° A"), o cadena vacía si no tiene grado. */
    public static String etiquetaCursoDe(Agenda a) {
        if (a.getGrado() == null) return "";
        String grupo = normalizarGrupo(a.getGrupo());
        return a.getGrado() + "°" + (grupo.isEmpty() ? "" : " " + grupo);
    }

    /**
     * Cuenta las clases por la dimensión indicada ("curso", "profesor" o
     * "materia"), en orden descendente por cantidad. Las clases sin valor en la
     * dimensión se agrupan como "Sin dato".
     */
    public Map<String, Long> contarPorDimension(List<Agenda> clases, String dimension) {
        java.util.function.Function<Agenda, String> etiqueta = switch (dimension) {
            case "profesor" -> a -> a.getProfesor() != null && !a.getProfesor().isBlank()
                    ? a.getProfesor().trim() : "Sin dato";
            case "materia" -> a -> a.getMateria() != null && !a.getMateria().isBlank()
                    ? a.getMateria().trim() : "Sin dato";
            default -> a -> {
                String c = etiquetaCursoDe(a);
                return c.isEmpty() ? "Sin dato" : c;
            };
        };
        Map<String, Long> conteo = new LinkedHashMap<>();
        clases.stream()
                .collect(java.util.stream.Collectors.groupingBy(etiqueta, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> conteo.put(e.getKey(), e.getValue()));
        return conteo;
    }

    /** Etiqueta legible de un filtro de curso "grado|grupo" (ej. "3° A"), o null si no hay filtro. */
    public String etiquetaCurso(String curso) {
        if (curso == null || curso.isBlank()) return null;
        String[] partes = curso.split("\\|", -1);
        String grupo = partes.length > 1 ? normalizarGrupo(partes[1]) : "";
        return partes[0].trim() + "°" + (grupo.isEmpty() ? "" : " " + grupo);
    }

    private List<String> listarDistintos(java.util.function.Function<Agenda, String> campo) {
        return agendaRepository.findAll().stream()
                .map(campo)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}