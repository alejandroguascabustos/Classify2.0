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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class MaterialController {

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
            for (MultipartFile archivo : archivos) {
                if (archivo == null || archivo.isEmpty()) continue;

                String originalName = archivo.getOriginalFilename();
                String ext = (originalName != null && originalName.contains("."))
                        ? originalName.substring(originalName.lastIndexOf(".")) : "";
                String nombreGuardado = UUID.randomUUID() + ext;

                Files.copy(archivo.getInputStream(),
                        dirPath.resolve(nombreGuardado),
                        StandardCopyOption.REPLACE_EXISTING);

                Material material = new Material();
                material.setNombreArchivo(originalName != null ? originalName : nombreGuardado);
                material.setRutaArchivo("/uploads/materiales/" + nombreGuardado);
                material.setFechaSubida(LocalDateTime.now());
                material.setIdUsuario(idUsuario);
                materialService.guardar(material);
                subidos++;
            }

            if (subidos > 0) {
                redirectAttrs.addFlashAttribute("mensajeExito",
                        subidos == 1 ? "Archivo subido correctamente." : subidos + " archivos subidos correctamente.");
            } else {
                redirectAttrs.addFlashAttribute("mensajeError", "No se selecciono ningun archivo.");
            }
        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al subir el archivo: " + e.getMessage());
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

        try {
            String nuevaRuta = null;
            if (archivo != null && !archivo.isEmpty()) {
                Path dirPath = uploadStorageResolver.resolveSubdirectory("materiales");
                String originalName = archivo.getOriginalFilename();
                String ext = (originalName != null && originalName.contains("."))
                        ? originalName.substring(originalName.lastIndexOf(".")) : "";
                String nombreGuardado = UUID.randomUUID() + ext;
                Files.copy(archivo.getInputStream(),
                        dirPath.resolve(nombreGuardado),
                        StandardCopyOption.REPLACE_EXISTING);
                nuevaRuta = "/uploads/materiales/" + nombreGuardado;
            }

            materialService.actualizar(idMaterial, nombreArchivo, nuevaRuta);
            redirectAttrs.addFlashAttribute("mensajeExito", "Material actualizado correctamente.");
        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("mensajeError", "Error al actualizar: " + e.getMessage());
        }

        return "redirect:/materiales";
    }

    // ─── GET /materiales/eliminar/{id} ────────────────────────────
    @GetMapping("/materiales/eliminar/{id}")
    public String eliminarMaterial(@PathVariable Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
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
        // rutaArchivo tiene forma "/uploads/materiales/uuid.ext"
        String relativePath = material.getRutaArchivo().replace("/uploads/", "");
        Path filePath = uploadStorageResolver.resolveRootPath().resolve(relativePath).normalize();

        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Archivo no encontrado en el servidor.");
            return;
        }

        String contentType = Files.probeContentType(filePath);
        response.setContentType(contentType != null ? contentType : "application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + material.getNombreArchivo() + "\"");
        response.setContentLengthLong(Files.size(filePath));

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(filePath, out);
        }
    }
}
