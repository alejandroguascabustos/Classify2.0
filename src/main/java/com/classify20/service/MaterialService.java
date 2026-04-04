package com.classify20.service;

import com.classify20.domain.Material;

import java.util.List;
import java.util.Optional;

public interface MaterialService {

    List<Material> listarPorUsuario(Long idUsuario);

    List<Material> listarTodos();

    Optional<Material> buscarPorId(Long id);

    void guardar(Material material);

    void actualizar(Long id, String nuevoNombre, String nuevaRuta);

    void eliminar(Long id);
}
