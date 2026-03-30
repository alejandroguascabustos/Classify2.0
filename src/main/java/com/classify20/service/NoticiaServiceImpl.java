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
        // Misma consulta; si en el futuro agregas campo "activa" puedes filtrar aquí
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
        noticiaDao.findById(id).ifPresent(noticiaExistente -> {
            noticiaExistente.setTituloNoticia(noticiaEditada.getTituloNoticia());
            noticiaExistente.setAutorNoticia(noticiaEditada.getAutorNoticia());
            noticiaExistente.setFechaNoticia(noticiaEditada.getFechaNoticia());
            noticiaExistente.setContenidoNoticia(noticiaEditada.getContenidoNoticia());
            noticiaExistente.setTipoNoticia(noticiaEditada.getTipoNoticia());
            noticiaDao.save(noticiaExistente);
        });
    }

    @Override
    public void eliminar(Long id) {
        noticiaDao.deleteById(id);
    }
}
