package com.classify20.service;

import com.classify20.domain.Noticia;

import java.util.List;
import java.util.Optional;

public interface NoticiaService {

    List<Noticia> listarTodas();

    List<Noticia> listarParaVista();

    Optional<Noticia> buscarPorId(Long id);

    void guardar(Noticia noticia);

    void actualizar(Long id, Noticia noticiaEditada);

    void eliminar(Long id);

    // Noticia más reciente para el menú
    Optional<Noticia> buscarMasReciente();
}