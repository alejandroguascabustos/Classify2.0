package com.classify20.service;

import com.classify20.model.Agenda;
import com.classify20.repository.AgendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    /**
     * Actualizar una agenda existente.
     * Solo sobreescribe los campos editables; el ID no cambia.
     * Retorna null si no existe.
     */
    public Agenda actualizarAgenda(Long id, Agenda datos) {
        Optional<Agenda> optional = agendaRepository.findById(id);
        if (optional.isEmpty()) return null;

        Agenda existente = optional.get();

        // Campos editables desde programacion.html
        if (datos.getMateria()       != null) existente.setMateria(datos.getMateria());
        if (datos.getProfesor()      != null) existente.setProfesor(datos.getProfesor());
        if (datos.getFecha()         != null) existente.setFecha(datos.getFecha());
        if (datos.getHoraInicio()    != null) existente.setHoraInicio(datos.getHoraInicio());
        if (datos.getDuracion()      != null) existente.setDuracion(datos.getDuracion());
        if (datos.getGrado()         != null) existente.setGrado(datos.getGrado());
        if (datos.getGrupo()         != null) existente.setGrupo(datos.getGrupo());
        if (datos.getModalidad()     != null) existente.setModalidad(datos.getModalidad());
        if (datos.getTemaPrincipal() != null) existente.setTemaPrincipal(datos.getTemaPrincipal());

        validarConflictos(existente, id);
        return agendaRepository.save(existente);
    }

    /**
     * Eliminar una agenda por ID.
     * Retorna true si existía y fue eliminada, false si no existía.
     */
    public boolean eliminarAgenda(Long id) {
        if (!agendaRepository.existsById(id)) return false;
        agendaRepository.deleteById(id);
        return true;
    }
}