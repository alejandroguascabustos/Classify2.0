package com.classify20.controller;

import com.classify20.domain.Noticia;
import com.classify20.service.NoticiaService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/noticias")
public class NoticiaController {

    @Autowired
    private NoticiaService noticiaService;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${classify.upload.path:/Users/macbookair/Classify2/Classify2.0/src/main/resources/static/uploads}")
    private String uploadPath;

    // Prefijo con el que se guardan las rutas en BD, ej: "/uploads/noticias/uuid.png"
    private static final String PREFIJO_UPLOADS = "/uploads/";

    // ─── GET /noticias → vista pública (con filtros opcionales) ──
    @GetMapping
    public String verNoticias(
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model, HttpSession session) {

        if (session.getAttribute("nombre") == null) return "redirect:/login";

        boolean hayFiltros = (tipo != null && !tipo.isBlank()) || desde != null || hasta != null;
        List<Noticia> noticias = hayFiltros
                ? noticiaService.filtrar(tipo, desde, hasta)
                : noticiaService.listarParaVista();

        model.addAttribute("noticias", noticias);
        model.addAttribute("tiposDisponibles", noticiaService.listarTipos());
        model.addAttribute("filtroTipo", tipo);
        model.addAttribute("filtroDesde", desde);
        model.addAttribute("filtroHasta", hasta);
        return "noticias/noticias";
    }

    // ─── GET /noticias/pdf → descarga en PDF de TODAS las noticias filtradas ──
    @GetMapping("/pdf")
    public void descargarPdf(
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpSession session, HttpServletResponse response) throws IOException {

        if (session.getAttribute("nombre") == null) {
            response.sendRedirect("/login");
            return;
        }

        boolean hayFiltros = (tipo != null && !tipo.isBlank()) || desde != null || hasta != null;
        List<Noticia> noticias = hayFiltros
                ? noticiaService.filtrar(tipo, desde, hasta)
                : noticiaService.listarParaVista();

        generarPdf(noticias, response, "noticias-classify.pdf", tipo, desde, hasta);
    }

    // ─── GET /noticias/pdf/{id} → descarga en PDF de UNA sola noticia ──
    @GetMapping("/pdf/{id}")
    public void descargarPdfIndividual(@PathVariable Long id,
                                       HttpSession session,
                                       HttpServletResponse response) throws IOException {

        if (session.getAttribute("nombre") == null) {
            response.sendRedirect("/login");
            return;
        }

        Optional<Noticia> opt = noticiaService.buscarPorId(id);
        if (opt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Noticia no encontrada.");
            return;
        }

        Noticia noticia = opt.get();
        String archivo = "noticia-" + id + ".pdf";
        generarPdf(List.of(noticia), response, archivo, null, null, null);
    }

    // ─── Helper reutilizado por ambos endpoints de PDF ────────────────
    private void generarPdf(List<Noticia> noticias,
                            HttpServletResponse response,
                            String nombreArchivo,
                            String filtroTipo,
                            LocalDate filtroDesde,
                            LocalDate filtroHasta) throws IOException {

        // Convertimos cada noticia a una vista liviana con la ruta de imagen
        // resuelta a una URI file:/// absoluta, porque Flying Saucer no puede
        // resolver rutas relativas tipo "/uploads/noticias/xxx.png" (esas solo
        // existen como endpoint HTTP servido por Spring, no como recurso de disco).
        List<NoticiaPdfView> vistas = noticias.stream()
                .map(n -> new NoticiaPdfView(
                        n.getTituloNoticia(),
                        n.getAutorNoticia(),
                        n.getFechaNoticia(),
                        n.getContenidoNoticia(),
                        n.getTipoNoticia(),
                        resolverImagenParaPdf(n.getImagenNoticia())
                ))
                .toList();

        Context contexto = new Context();
        contexto.setVariable("noticias", vistas);
        contexto.setVariable("filtroTipo", filtroTipo);
        contexto.setVariable("filtroDesde", filtroDesde);
        contexto.setVariable("filtroHasta", filtroHasta);
        contexto.setVariable("fechaGeneracion", LocalDateTime.now());

        // Plantilla independiente (src/main/resources/templates/noticias/noticiasPdf.html)
        String html = templateEngine.process("noticias/noticiasPdf", contexto);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");

        try {
            ITextRenderer renderer = new ITextRenderer();
            // Si los acentos/ñ no se ven bien en el PDF, registra una fuente TTF que los
            // soporte (ej. DejaVu Sans) antes del layout():
            //
            //   renderer.getFontResolver().addFont(
            //       "src/main/resources/static/fonts/DejaVuSans.ttf",
            //       com.lowagie.text.pdf.BaseFont.IDENTITY_H, true);
            //
            // y usa "font-family: 'DejaVu Sans', sans-serif;" en noticiasPdf.html.
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(response.getOutputStream());
        } catch (Exception e) {
            throw new IOException("Error generando el PDF de noticias: " + e.getMessage(), e);
        }
    }

    // Convierte "/uploads/noticias/uuid.png" (ruta guardada en BD) en una
    // URI file:/// absoluta apuntando al archivo real en disco, usando la
    // misma base (uploadPath) con la que se guardó en guardarNoticia().
    // Devuelve null si la noticia no tiene imagen o el archivo no existe.
    private String resolverImagenParaPdf(String rutaGuardada) {
        if (rutaGuardada == null || rutaGuardada.isBlank()) {
            return null;
        }

        String relativo = rutaGuardada.startsWith(PREFIJO_UPLOADS)
                ? rutaGuardada.substring(PREFIJO_UPLOADS.length())
                : rutaGuardada;

        Path absoluto = Paths.get(uploadPath).resolve(relativo).normalize();

        if (!Files.exists(absoluto)) {
            return null;
        }

        return absoluto.toUri().toString();
    }

    // Vista liviana usada solo para renderizar la plantilla del PDF
    // (evita mutar la entidad Noticia y desacopla la imagen resuelta en disco).
    private record NoticiaPdfView(
            String tituloNoticia,
            String autorNoticia,
            LocalDateTime fechaNoticia,
            String contenidoNoticia,
            String tipoNoticia,
            String imagenUrl) {
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