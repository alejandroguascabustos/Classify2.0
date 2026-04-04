package com.classify20.controller;

import com.classify20.domain.Noticia;
import com.classify20.service.NoticiaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/noticias")
public class NoticiaController {

    @Autowired
    private NoticiaService noticiaService;

    @Value("${classify.upload.path=C:/classify-uploads}")
    private String uploadPath;

    // ─── GET /noticias → vista pública ───────────────────────
    @GetMapping
    public String verNoticias(Model model, HttpSession session) {
        if (session.getAttribute("nombre") == null) return "redirect:/login";
        model.addAttribute("noticias", noticiaService.listarParaVista());
        return "noticias/noticias";
    }

    // ─── GET /noticias/historial ──────────────────────────────
    @GetMapping("/historial")
    public String verHistorial(Model model, HttpSession session) {
        if (session.getAttribute("nombre") == null) return "redirect:/login";
        model.addAttribute("noticias", noticiaService.listarTodas());
        return "noticias/historialNoticia";
    }

    // ─── GET /noticias/form → CREAR ───────────────────────────
    @GetMapping("/form")
    public String mostrarFormulario(HttpSession session, Model model) {
        if (session.getAttribute("nombre") == null) return "redirect:/login";
        Noticia nueva = new Noticia();
        // Auto-rellenar autor con nombre + apellido de sesión
        String nombre   = session.getAttribute("nombre")   != null ? session.getAttribute("nombre").toString()   : "";
        String apellido = session.getAttribute("apellido") != null ? " " + session.getAttribute("apellido").toString() : "";
        nueva.setAutorNoticia((nombre + apellido).trim());
        // Auto-rellenar fecha con ahora mismo
        nueva.setFechaNoticia(LocalDateTime.now());
        model.addAttribute("noticia", nueva);
        return "noticias/formularioNoticia";
    }

    // ─── GET /noticias/form/{id} → EDITAR ────────────────────
    @GetMapping("/form/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id,
                                          HttpSession session, Model model) {
        if (session.getAttribute("nombre") == null) return "redirect:/login";
        Optional<Noticia> opt = noticiaService.buscarPorId(id);
        if (opt.isEmpty()) return "redirect:/noticias/historial";
        model.addAttribute("noticia", opt.get());
        return "noticias/formularioNoticia";
    }

    // ─── POST /noticias/guardar → crear o actualizar ──────────
    @PostMapping("/guardar")
    public String guardarNoticia(
            @RequestParam(value = "idNoticia", required = false) Long idNoticia,
            @RequestParam("titulo_noticia")    String tituloNoticia,
            @RequestParam("autor_noticia")     String autorNoticia,
            // ISO datetime-local: "2026-03-31T14:30"
            @RequestParam("fecha_noticia")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaNoticia,
            @RequestParam("contenido_noticia") String contenidoNoticia,
            @RequestParam(value = "tipo_noticia", required = false) String tipoNoticia,
            @RequestParam(value = "imagen",    required = false) MultipartFile imagen,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        if (session.getAttribute("nombre") == null) return "redirect:/login";

        try {
            // ── Imagen ────────────────────────────────────────
            String rutaImagen = null;
            if (imagen != null && !imagen.isEmpty()) {
                Path dirPath = Paths.get(uploadPath, "noticias");
                Files.createDirectories(dirPath);
                String originalName = imagen.getOriginalFilename();
                String ext = (originalName != null && originalName.contains("."))
                        ? originalName.substring(originalName.lastIndexOf(".")) : "";
                String nombreArchivo = UUID.randomUUID() + ext;
                Files.copy(imagen.getInputStream(),
                           dirPath.resolve(nombreArchivo),
                           StandardCopyOption.REPLACE_EXISTING);
                rutaImagen = "/uploads/noticias/" + nombreArchivo;
            }

            // ── Entidad ───────────────────────────────────────
            Noticia noticia = new Noticia();
            noticia.setTituloNoticia(tituloNoticia);
            noticia.setAutorNoticia(autorNoticia);
            noticia.setFechaNoticia(fechaNoticia);
            noticia.setContenidoNoticia(contenidoNoticia);
            noticia.setTipoNoticia(tipoNoticia);
            if (rutaImagen != null) noticia.setImagenNoticia(rutaImagen);

            if (idNoticia != null) {
                noticiaService.actualizar(idNoticia, noticia);
                redirectAttrs.addFlashAttribute("mensajeExito", "Noticia actualizada correctamente.");
            } else {
                noticiaService.guardar(noticia);
                redirectAttrs.addFlashAttribute("mensajeExito", "Noticia creada correctamente.");
            }
            return "redirect:/noticias/historial";

        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al subir la imagen: " + e.getMessage());
            return "redirect:/noticias/form";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al guardar: " + e.getMessage());
            return "redirect:/noticias/form";
        }
    }

    // ─── GET /noticias/eliminar/{id} ──────────────────────────
    @GetMapping("/eliminar/{id}")
    public String eliminarNoticia(@PathVariable Long id, HttpSession session,
                                  RedirectAttributes redirectAttrs) {
        if (session.getAttribute("nombre") == null) return "redirect:/login";
        noticiaService.eliminar(id);
        redirectAttrs.addFlashAttribute("mensajeExito", "Noticia eliminada correctamente.");
        return "redirect:/noticias/historial";
    }
}