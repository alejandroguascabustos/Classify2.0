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
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/noticias")
public class NoticiaController {

    @Autowired
    private NoticiaService noticiaService;

    // Ruta absoluta desde application.properties
    @Value("${classify.upload.path:C:/classify-uploads}")
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

    // ─── GET /noticias/form → formulario CREAR ────────────────
    @GetMapping("/form")
    public String mostrarFormulario(HttpSession session, Model model) {
        if (session.getAttribute("nombre") == null) return "redirect:/login";
        Noticia nueva = new Noticia();
        // Auto-rellenar autor con el nombre del usuario en sesión
        if (session.getAttribute("nombre") != null) {
            String nombre = session.getAttribute("nombre").toString();
            String apellido = session.getAttribute("apellido") != null
                    ? " " + session.getAttribute("apellido").toString() : "";
            nueva.setAutorNoticia(nombre + apellido);
        }
        // Auto-rellenar fecha con hoy
        nueva.setFechaNoticia(LocalDate.now());
        model.addAttribute("noticia", nueva);
        return "noticias/formularioNoticia";
    }

    // ─── GET /noticias/form/{id} → formulario EDITAR ─────────
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
            @RequestParam("fecha_noticia")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNoticia,
            @RequestParam("contenido_noticia") String contenidoNoticia,
            @RequestParam(value = "tipo_noticia", required = false) String tipoNoticia,
            @RequestParam(value = "imagen",    required = false) MultipartFile imagen,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        if (session.getAttribute("nombre") == null) return "redirect:/login";

        try {
            // ── Manejo de imagen ──────────────────────────────
            String rutaImagen = null;

            if (imagen != null && !imagen.isEmpty()) {
                // Subcarpeta noticias dentro del uploadPath
                Path dirPath = Paths.get(uploadPath, "noticias");
                Files.createDirectories(dirPath);

                // Nombre único para evitar colisiones
                String extension = "";
                String originalName = imagen.getOriginalFilename();
                if (originalName != null && originalName.contains(".")) {
                    extension = originalName.substring(originalName.lastIndexOf("."));
                }
                String nombreArchivo = UUID.randomUUID() + extension;

                Files.copy(imagen.getInputStream(),
                           dirPath.resolve(nombreArchivo),
                           StandardCopyOption.REPLACE_EXISTING);

                // URL pública que Spring servirá vía /uploads/**
                rutaImagen = "/uploads/noticias/" + nombreArchivo;
            }

            // ── Construir entidad ─────────────────────────────
            Noticia noticia = new Noticia();
            noticia.setTituloNoticia(tituloNoticia);
            noticia.setAutorNoticia(autorNoticia);
            noticia.setFechaNoticia(fechaNoticia);
            noticia.setContenidoNoticia(contenidoNoticia);
            noticia.setTipoNoticia(tipoNoticia);
            if (rutaImagen != null) noticia.setImagenNoticia(rutaImagen);

            if (idNoticia != null) {
                // EDITAR: conserva imagen anterior si no se subió una nueva
                noticiaService.actualizar(idNoticia, noticia);
                redirectAttrs.addFlashAttribute("mensajeExito", "Noticia actualizada correctamente.");
            } else {
                // CREAR
                noticiaService.guardar(noticia);
                redirectAttrs.addFlashAttribute("mensajeExito", "Noticia creada correctamente.");
            }

            return "redirect:/noticias/historial";

        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("mensajeError",
                    "Error al subir la imagen: " + e.getMessage());
            return "redirect:/noticias/form";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError",
                    "Error al guardar la noticia: " + e.getMessage());
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