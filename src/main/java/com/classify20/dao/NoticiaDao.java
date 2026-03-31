package com.classify20.dao;

import com.classify20.domain.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticiaDao extends JpaRepository<Noticia, Long> {

    // Todas ordenadas de más reciente a más antigua (por datetime)
    List<Noticia> findAllByOrderByFechaNoticiaDesc();

    // Por tipo, ordenadas desc
    List<Noticia> findByTipoNoticiaOrderByFechaNoticiaDesc(String tipoNoticia);

    // La más reciente para el menú — LIMIT 1 compatible con PostgreSQL y H2
    @Query("SELECT n FROM Noticia n ORDER BY n.fechaNoticia DESC LIMIT 1")
    Optional<Noticia> findMasReciente();
}