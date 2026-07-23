package com.classify20.dao;

import com.classify20.domain.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // ─── Filtro combinado (tipo/desde/hasta, todos opcionales) ─────────
    // IMPORTANTE: se usa un query NATIVO con cast(... as ...) explícito de
    // PostgreSQL. PostgreSQL no puede inferir el tipo de un bind parameter
    // que solo se compara contra NULL (error: "could not determine data
    // type of parameter $N"), y depender de que Hibernate traduzca un
    // CAST en HQL es frágil entre versiones. Con SQL nativo el cast queda
    // garantizado tal cual se ve aquí.
    @Query(value = """
            SELECT n.* FROM noticias n
            WHERE (CAST(:tipo AS varchar) IS NULL OR n.tipo_noticia = CAST(:tipo AS varchar))
              AND (CAST(:desde AS timestamp) IS NULL OR n.fecha_noticia >= CAST(:desde AS timestamp))
              AND (CAST(:hasta AS timestamp) IS NULL OR n.fecha_noticia <= CAST(:hasta AS timestamp))
            ORDER BY n.fecha_noticia DESC
            """, nativeQuery = true)
    List<Noticia> filtrar(@Param("tipo") String tipo,
                           @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta);

    // Tipos existentes, para poblar el selector de filtros
    @Query("SELECT DISTINCT n.tipoNoticia FROM Noticia n " +
           "WHERE n.tipoNoticia IS NOT NULL AND n.tipoNoticia <> '' " +
           "ORDER BY n.tipoNoticia")
    List<String> findTiposDistintos();
}