package com.classify20.controller;

import com.classify20.config.UploadStorageResolver;
import com.classify20.domain.Material;
import com.classify20.service.MaterialService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Controller
public class MaterialController {

    /**
     * Lista blanca de extensiones.
     *
     * <p>Antes se conservaba la extension original sin filtrar. Un .html o un
     * .svg subido y luego servido desde /uploads/** se ejecuta en el dominio
     * de Classify: es XSS almacenado con acceso a la sesion de quien lo abra.
     * Con lista blanca solo entran formatos que el navegador no ejecuta como
     * documento propio.
     */
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".odt", ".ods", ".odp", ".txt", ".csv", ".rtf",
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp",
            ".zip", ".rar", ".7z", ".mp3", ".mp4"
    );

    @Autowired
    private MaterialService materialService;

    @Autowired
    private UploadStorageResolver uploadStorageResolver;

    // ─── GET /materiales → vista docente ─────────────────────────
    @GetMapping("/materiales")
    public String verMateriales(Model model, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        Long idUsuario = (Long) session.getAttribute("usuarioId");
        model.addAttribute("materiales", materialService.listarPorUsuario(idUsuario));
        return "materiales/materiales";
    }

    // ─── GET /mismateriales → vista estudiante ────────────────────
    @GetMapping("/mismateriales")
    public String verMisMateriales(Model model, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        model.addAttribute("materiales", materialService.listarTodos());
        return "mismateriales/mismateriales";
    }

    // ─── POST /materiales/subir ───────────────────────────────────
    @PostMapping("/materiales/subir")
    public String subirArchivos(
            @RequestParam("archivos") List<MultipartFile> archivos,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        Long idUsuario = (Long) session.getAttribute("usuarioId");

        try {
            Path dirPath = uploadStorageResolver.resolveSubdirectory("materiales");

            int subidos = 0;
            int rechazados = 0;
            for (MultipartFile archivo : archivos) {
                if (archivo == null || archivo.isEmpty()) continue;

                String originalName = archivo.getOriginalFilename();
                String ext = extensionDe(originalName);

                // Extension no permitida -> no se guarda nada.
                if (!EXTENSIONES_PERMITIDAS.contains(ext)) {
                    rechazados++;
                    continue;
                }

                String nombreGuardado = UUID.randomUUID() + ext;

                Files.copy(archivo.getInputStream(),
                        dirPath.resolve(nombreGuardado),
                        StandardCopyOption.REPLACE_EXISTING);

                Material material = new Material();
                // El nombre visible tambien se depura. Se muestra en tablas
                // HTML y viaja en la cabecera Content-Disposition.
                material.setNombreArchivo(limpiarNombre(originalName, nombreGuardado));
                material.setRutaArchivo("/uploads/materiales/" + nombreGuardado);
                material.setFechaSubida(LocalDateTime.now());
                material.setIdUsuario(idUsuario);
                materialService.guardar(material);
                subidos++;
            }

            if (subidos > 0 && rechazados == 0) {
                redirectAttrs.addFlashAttribute("mensajeExito",
                        subidos == 1 ? "Archivo subido correctamente." : subidos + " archivos subidos correctamente.");
            } else if (subidos > 0) {
                redirectAttrs.addFlashAttribute("mensajeExito",
                        subidos + " archivo(s) subido(s). " + rechazados + " rechazado(s) por tipo no permitido.");
            } else if (rechazados > 0) {
                redirectAttrs.addFlashAttribute("mensajeError",
                        "Tipo de archivo no permitido. Formatos aceptados: documentos, imagenes, audio, video y comprimidos.");
            } else {
                redirectAttrs.addFlashAttribute("mensajeError", "No se selecciono ningun archivo.");
            }
        } catch (IOException e) {
            // No se expone e.getMessage(): puede revelar rutas del servidor.
            redirectAttrs.addFlashAttribute("mensajeError", "Error al subir el archivo.");
        }

        return "redirect:/materiales";
    }

    // ─── POST /materiales/actualizar ─────────────────────────────
    @PostMapping("/materiales/actualizar")
    public String actualizarMaterial(
            @RequestParam("id_material") Long idMaterial,
            @RequestParam("nombre_archivo") String nombreArchivo,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        // Sin esta comprobacion, cualquier usuario autenticado podia
        // renombrar o reemplazar el archivo de otro pasando su id (IDOR).
        if (!esPropietarioOAdmin(idMaterial, session)) {
            redirectAttrs.addFlashAttribute("mensajeError", "No tienes permiso sobre ese material.");
            return "redirect:/materiales";
        }

        try {
            String nuevaRuta = null;
            if (archivo != null && !archivo.isEmpty()) {
                String originalName = archivo.getOriginalFilename();
                String ext = extensionDe(originalName);

                if (!EXTENSIONES_PERMITIDAS.contains(ext)) {
                    redirectAttrs.addFlashAttribute("mensajeError", "Tipo de archivo no permitido.");
                    return "redirect:/materiales";
                }

                Path dirPath = uploadStorageResolver.resolveSubdirectory("materiales");
                String nombreGuardado = UUID.randomUUID() + ext;
                Files.copy(archivo.getInputStream(),
                        dirPath.resolve(nombreGuardado),
                        StandardCopyOption.REPLACE_EXISTING);
                nuevaRuta = "/uploads/materiales/" + nombreGuardado;
            }

            materialService.actualizar(idMaterial, limpiarNombre(nombreArchivo, "material"), nuevaRuta);
            redirectAttrs.addFlashAttribute("mensajeExito", "Material actualizado correctamente.");
        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al actualizar el material.");
        }

        return "redirect:/materiales";
    }

    // ─── POST /materiales/eliminar/{id} ───────────────────────────
    /**
     * Era @GetMapping. El filtro CSRF exime los metodos seguros (GET, HEAD,
     * OPTIONS, TRACE) por definicion, asi que un borrado por GET quedaba sin
     * proteccion: bastaba con que la victima cargara una pagina con
     * {@code <img src=".../materiales/eliminar/42">} para borrar el
     * material. Tambien lo disparaban prefetch de navegador y rastreadores.
     *
     * <p>Ademas se comprueba la propiedad del material. Antes solo se exigia
     * sesion, de modo que cualquier usuario podia recorrer ids y borrar el
     * material de todos los docentes (IDOR).
     */
    @PostMapping("/materiales/eliminar/{id}")
    public String eliminarMaterial(@PathVariable Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        if (!esPropietarioOAdmin(id, session)) {
            redirectAttrs.addFlashAttribute("mensajeError", "No tienes permiso para eliminar ese material.");
            return "redirect:/materiales";
        }

        materialService.eliminar(id);
        redirectAttrs.addFlashAttribute("mensajeExito", "Material eliminado correctamente.");
        return "redirect:/materiales";
    }

    // ─── GET /materiales/descargar/{id} ──────────────────────────
    @GetMapping("/materiales/descargar/{id}")
    public void descargarMaterial(@PathVariable Long id,
                                  HttpSession session,
                                  HttpServletResponse response) throws IOException {

        if (session.getAttribute("usuarioId") == null) {
            response.sendRedirect("/login");
            return;
        }

        Optional<Material> opt = materialService.buscarPorId(id);
        if (opt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Material no encontrado.");
            return;
        }

        Material material = opt.get();
        Path raiz = uploadStorageResolver.resolveRootPath().normalize();

        // rutaArchivo tiene forma "/uploads/materiales/uuid.ext"
        String relativePath = material.getRutaArchivo().replace("/uploads/", "");
        Path filePath = raiz.resolve(relativePath).normalize();

        // La ruta procede de base de datos, pero no debe poder apuntar fuera del
        // directorio de subidas bajo ninguna circunstancia.
        if (!filePath.startsWith(raiz)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Material no encontrado.");
            return;
        }

        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Archivo no encontrado en el servidor.");
            return;
        }

        // Se fuerza octet-stream. Servir el Content-Type real haria que un
        // .svg o un .html guardado antes de la lista blanca se renderizara
        // en el dominio de Classify.
        response.setContentType("application/octet-stream");

        // El nombre lo eligio quien subio el archivo: se depuran comillas y saltos
        // de linea para que no pueda alterar la cabecera.
        String nombreDescarga = material.getNombreArchivo() == null
                ? "material"
                : material.getNombreArchivo().replaceAll("[\\r\\n\"\\\\]", "_");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + nombreDescarga + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLengthLong(Files.size(filePath));

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(filePath, out);
        }
    }

    // ─── utilidades ────────────────────────────────────────────────

    /**
     * Propiedad del material. Administrador y coordinador conservan acceso
     * total; el resto solo puede tocar lo suyo.
     */
    private boolean esPropietarioOAdmin(Long idMaterial, HttpSession session) {
        Optional<Material> opt = materialService.buscarPorId(idMaterial);
        if (opt.isEmpty()) return false;

        Object idSesion = session.getAttribute("usuarioId");
        if (idSesion == null) return false;
        long usuarioId = ((Number) idSesion).longValue();

        Long propietario = opt.get().getIdUsuario();
        if (propietario != null && propietario == usuarioId) return true;

        Object rol = session.getAttribute("tipoUsuario");
        String tipo = rol == null ? "" : rol.toString().toLowerCase(Locale.ROOT);
        return tipo.equals("administrador") || tipo.equals("coordinador");
    }

    /** Extension en minusculas, con punto. Cadena vacia si no tiene. */
    private String extensionDe(String nombre) {
        if (nombre == null) return "";
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) return "";
        return nombre.substring(punto).toLowerCase(Locale.ROOT);
    }

    /**
     * Quita separadores de ruta y caracteres de control del nombre visible.
     * Evita que un nombre como {@code ../../x} o con saltos de linea acabe en
     * una cabecera HTTP o en la tabla de materiales.
     */
    private String limpiarNombre(String nombre, String porDefecto) {
        if (nombre == null || nombre.isBlank()) return porDefecto;
        String limpio = nombre
                .replaceAll("[\\r\\n\\t]", " ")
                .replaceAll("[/\\\\]", "_")
                .trim();
        if (limpio.length() > 200) limpio = limpio.substring(0, 200);
        return limpio.isBlank() ? porDefecto : limpio;
    }
}
