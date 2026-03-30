package com.classify20.dao;

import com.classify20.domain.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticiaDao extends JpaRepository<Noticia, Long> {

    // Todas las noticias ordenadas por fecha descendente (más recientes primero)
    List<Noticia> findAllByOrderByFechaNoticiaDesc();

    // Buscar por tipo de noticia
    List<Noticia> findByTipoNoticiaOrderByFechaNoticiaDesc(String tipoNoticia);
}
