package com.classify20.service;

import com.classify20.domain.Noticia;

import java.util.List;
import java.util.Optional;

public interface NoticiaService {

    // Obtener todas las noticias (para historial, con editar/eliminar)
    List<Noticia> listarTodas();

    // Obtener noticias activas ordenadas por fecha (para vista pública)
    List<Noticia> listarParaVista();

    // Buscar una noticia por ID
    Optional<Noticia> buscarPorId(Long id);

    // Guardar nueva noticia
    void guardar(Noticia noticia);

    // Actualizar noticia existente
    void actualizar(Long id, Noticia noticiaEditada);

    // Eliminar noticia por ID
    void eliminar(Long id);
}
