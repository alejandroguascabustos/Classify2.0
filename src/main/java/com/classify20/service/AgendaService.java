package com.classify20.service;

import com.classify20.model.Agenda;
import com.classify20.repository.AgendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgendaService {

    private final AgendaRepository agendaRepository;

    /** Guardar una nueva agenda */
    public Agenda guardarAgenda(Agenda agenda) {
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