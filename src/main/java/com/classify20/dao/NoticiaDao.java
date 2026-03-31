package com.classify20.dao;

import com.classify20.domain.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticiaDao extends JpaRepository<Noticia, Long> {

    // Todas las noticias ordenadas por fecha desc (más recientes primero)
    List<Noticia> findAllByOrderByFechaNoticiaDesc();

    // Buscar por tipo ordenadas por fecha
    List<Noticia> findByTipoNoticiaOrderByFechaNoticiaDesc(String tipoNoticia);

    // Noticia más reciente para el menú
    // Usamos @Query explícita para garantizar compatibilidad con PostgreSQL
    @Query("SELECT n FROM Noticia n ORDER BY n.fechaNoticia DESC LIMIT 1")
    Optional<Noticia> findMasReciente();
}