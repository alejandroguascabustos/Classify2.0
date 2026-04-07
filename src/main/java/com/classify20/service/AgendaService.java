package com.classify20.service;

import com.classify20.model.Agenda;
import com.classify20.repository.AgendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor  // Lombok: inyección de dependencias por constructor
public class AgendaService {

    private final AgendaRepository agendaRepository;

    // Guardar una nueva agenda
    public Agenda guardarAgenda(Agenda agenda) {
        return agendaRepository.save(agenda);
    }

    // Listar todas (útil después para ver registros)
    public List<Agenda> listarAgendas() {
        return agendaRepository.findAll();
    }
}