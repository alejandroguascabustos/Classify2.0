package com.classify20.dao;

import com.classify20.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialDao extends JpaRepository<Material, Long> {

    List<Material> findByIdUsuarioOrderByFechaSubidaDesc(Long idUsuario);

    List<Material> findAllByOrderByFechaSubidaDesc();
}
