package com.classify20.controller;

import com.classify20.domain.Noticia;
import com.classify20.service.NoticiaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
import java.time.LocalDate;
import java.util.Optional;
 
@Controller
@RequestMapping("/noticias")
public class NoticiaController {
 
    @Autowired
    private NoticiaService noticiaService;
 
    // ─────────────────────────────────────────────
    // GET /noticias  →  vista pública de noticias
    // ─────────────────────────────────────────────
    @GetMapping
    public String verNoticias(Model model, HttpSession session) {
        if (session.getAttribute("nombre") == null) {
            return "redirect:/login";
        }
        model.addAttribute("noticias", noticiaService.listarParaVista());
        return "noticias";   // → templates/noticias.html
    }
 
    // ─────────────────────────────────────────────
    // GET /noticias/historial  →  historial con editar/eliminar
    // ─────────────────────────────────────────────
    @GetMapping("/historial")
    public String verHistorial(Model model, HttpSession session) {
        if (session.getAttribute("nombre") == null) {
            return "redirect:/login";
        }
        model.addAttribute("noticias", noticiaService.listarTodas());
        return "historialNoticia";   // → templates/historialNoticia.html
    }
 
    // ─────────────────────────────────────────────
    // GET /noticias/crear  →  formulario nueva noticia
    // ─────────────────────────────────────────────
    @GetMapping("/crear")
    public String mostrarFormCrear(HttpSession session) {
        if (session.getAttribute("nombre") == null) {
            return "redirect:/login";
        }
        return "crearNoticia";   // → templates/crearNoticia.html
    }
 
    // ─────────────────────────────────────────────
    // POST /noticias/guardar  →  persistir nueva noticia
    // ─────────────────────────────────────────────
    @PostMapping("/guardar")
    public String guardarNoticia(
            @RequestParam("titulo_noticia")    String tituloNoticia,
            @RequestParam("autor_noticia")     String autorNoticia,
            @RequestParam("fecha_noticia")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNoticia,
            @RequestParam("contenido_noticia") String contenidoNoticia,
            @RequestParam(value = "tipo_noticia", required = false) String tipoNoticia,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttrs) {
 
        if (session.getAttribute("nombre") == null) {
            return "redirect:/login";
        }
 
        try {
            Noticia noticia = new Noticia();
            noticia.setTituloNoticia(tituloNoticia);
            noticia.setAutorNoticia(autorNoticia);
            noticia.setFechaNoticia(fechaNoticia);
            noticia.setContenidoNoticia(contenidoNoticia);
            noticia.setTipoNoticia(tipoNoticia);
 
            noticiaService.guardar(noticia);
            redirectAttrs.addFlashAttribute("mensajeExito", "Noticia creada correctamente.");
            return "redirect:/noticias/historial";
 
        } catch (Exception e) {
            // Regresa al formulario conservando lo que escribió el usuario
            Noticia oldInput = new Noticia();
            oldInput.setTituloNoticia(tituloNoticia);
            oldInput.setAutorNoticia(autorNoticia);
            oldInput.setFechaNoticia(fechaNoticia);
            oldInput.setContenidoNoticia(contenidoNoticia);
            model.addAttribute("oldInput", oldInput);
            model.addAttribute("mensajeError", "Error al guardar la noticia. Intenta de nuevo.");
            return "crearNoticia";
        }
    }
 
    // ─────────────────────────────────────────────
    // GET /noticias/editar/{id}  →  formulario edición
    // ─────────────────────────────────────────────
    @GetMapping("/editar/{id}")
    public String mostrarFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("nombre") == null) {
            return "redirect:/login";
        }
 
        Optional<Noticia> noticiaOpt = noticiaService.buscarPorId(id);
        if (noticiaOpt.isEmpty()) {
            return "redirect:/noticias/historial";
        }
 
        model.addAttribute("noticia", noticiaOpt.get());
        return "editarNoticia";   // → templates/editarNoticia.html
    }
 
    // ─────────────────────────────────────────────
    // POST /noticias/actualizar/{id}  →  actualizar noticia
    // ─────────────────────────────────────────────
    @PostMapping("/actualizar/{id}")
    public String actualizarNoticia(
            @PathVariable Long id,
            @RequestParam("titulo_noticia")    String tituloNoticia,
            @RequestParam("autor_noticia")     String autorNoticia,
            @RequestParam("fecha_noticia")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNoticia,
            @RequestParam("contenido_noticia") String contenidoNoticia,
            @RequestParam(value = "tipo_noticia", required = false) String tipoNoticia,
            HttpSession session,
            RedirectAttributes redirectAttrs) {
 
        if (session.getAttribute("nombre") == null) {
            return "redirect:/login";
        }
 
        Noticia noticiaEditada = new Noticia();
        noticiaEditada.setTituloNoticia(tituloNoticia);
        noticiaEditada.setAutorNoticia(autorNoticia);
        noticiaEditada.setFechaNoticia(fechaNoticia);
        noticiaEditada.setContenidoNoticia(contenidoNoticia);
        noticiaEditada.setTipoNoticia(tipoNoticia);
 
        noticiaService.actualizar(id, noticiaEditada);
        redirectAttrs.addFlashAttribute("mensajeExito", "Noticia actualizada correctamente.");
        return "redirect:/noticias/historial";
    }
 
    // ─────────────────────────────────────────────
    // GET /noticias/eliminar/{id}  →  eliminar noticia
    // ─────────────────────────────────────────────
    @GetMapping("/eliminar/{id}")
    public String eliminarNoticia(@PathVariable Long id, HttpSession session,
                                  RedirectAttributes redirectAttrs) {
        if (session.getAttribute("nombre") == null) {
            return "redirect:/login";
        }
 
        noticiaService.eliminar(id);
        redirectAttrs.addFlashAttribute("mensajeExito", "Noticia eliminada correctamente.");
        return "redirect:/noticias/historial";
    }
}
