package com.classify20.service;

import com.classify20.dao.NoticiaDao;
import com.classify20.domain.Noticia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class NoticiaServiceImpl implements NoticiaService {

    @Autowired
    private NoticiaDao noticiaDao;

    @Override
    public List<Noticia> listarTodas() {
        return noticiaDao.findAllByOrderByFechaNoticiaDesc();
    }

    @Override
    public List<Noticia> listarParaVista() {
        return noticiaDao.findAllByOrderByFechaNoticiaDesc();
    }

    @Override
    public Optional<Noticia> buscarPorId(Long id) {
        return noticiaDao.findById(id);
    }

    @Override
    public void guardar(Noticia noticia) {
        noticiaDao.save(noticia);
    }

    @Override
    public void actualizar(Long id, Noticia noticiaEditada) {
        noticiaDao.findById(id).ifPresent(existente -> {
            existente.setTituloNoticia(noticiaEditada.getTituloNoticia());
            existente.setAutorNoticia(noticiaEditada.getAutorNoticia());
            existente.setFechaNoticia(noticiaEditada.getFechaNoticia());
            existente.setContenidoNoticia(noticiaEditada.getContenidoNoticia());
            existente.setTipoNoticia(noticiaEditada.getTipoNoticia());
            // Solo actualiza imagen si viene una nueva
            if (noticiaEditada.getImagenNoticia() != null && !noticiaEditada.getImagenNoticia().isBlank()) {
                existente.setImagenNoticia(noticiaEditada.getImagenNoticia());
            }
            noticiaDao.save(existente);
        });
    }

    @Override
    public void eliminar(Long id) {
        noticiaDao.deleteById(id);
    }

    @Override
    public Optional<Noticia> buscarMasReciente() {
        return noticiaDao.findTopByOrderByFechaNoticiaDesc();
    }
}
