package com.classify20.repository;

import com.classify20.model.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AgendaRepository extends JpaRepository<Agenda, Long> {

    /** Todas las agendas de un grado en una fecha (para detectar conflictos de salón) */
    List<Agenda> findByFechaAndGrado(LocalDate fecha, Integer grado);

    /** Todas las agendas de un profesor en una fecha (para detectar conflictos de profesor) */
    List<Agenda> findByFechaAndProfesor(LocalDate fecha, String profesor);
}