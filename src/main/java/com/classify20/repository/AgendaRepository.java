package com.classify20.repository;

import com.classify20.model.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgendaRepository extends JpaRepository<Agenda, Long> {
    // JpaRepository ya incluye: save(), findAll(), findById(), delete(), etc.
    // Aquí se pueden agregar consultas personalizadas después si se desean realizar
}