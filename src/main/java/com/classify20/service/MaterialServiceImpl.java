package com.classify20.service;

import com.classify20.dao.MaterialDao;
import com.classify20.domain.Material;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MaterialServiceImpl implements MaterialService {

    @Autowired
    private MaterialDao materialDao;

    @Override
    public List<Material> listarPorUsuario(Long idUsuario) {
        return materialDao.findByIdUsuarioOrderByFechaSubidaDesc(idUsuario);
    }

    @Override
    public List<Material> listarTodos() {
        return materialDao.findAllByOrderByFechaSubidaDesc();
    }

    @Override
    public Optional<Material> buscarPorId(Long id) {
        return materialDao.findById(id);
    }

    @Override
    public void guardar(Material material) {
        materialDao.save(material);
    }

    @Override
    public void actualizar(Long id, String nuevoNombre, String nuevaRuta) {
        materialDao.findById(id).ifPresent(existente -> {
            existente.setNombreArchivo(nuevoNombre);
            if (nuevaRuta != null && !nuevaRuta.isBlank()) {
                existente.setRutaArchivo(nuevaRuta);
            }
            materialDao.save(existente);
        });
    }

    @Override
    public void eliminar(Long id) {
        materialDao.deleteById(id);
    }
}
