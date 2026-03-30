package com.classify20.dao;

import com.classify20.domain.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticiaDao extends JpaRepository<Noticia, Long> {
    
    List<Noticia> findAllByOrderByFechaNoticiaDesc();

    List<Noticia> findByTipoNoticiaOrderByFechaNoticiaDesc(String tipoNoticia);

    // ← NUEVO: trae la noticia más reciente para el menú
    Optional<Noticia> findTopByOrderByFechaNoticiaDesc();
}
