package com.classify20.controller;

import com.classify20.domain.Modulo;
import com.classify20.service.PermisosService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Módulo de administración de permisos: el administrador define qué módulos
 * puede ver cada rol y aplica excepciones puntuales por usuario.
 * El acceso (solo administrador) lo garantiza {@code AuthInterceptor}.
 */
@Controller
@RequestMapping("/gestion-permisos")
public class PermisosController {

    private final PermisosService permisosService;

    public PermisosController(PermisosService permisosService) {
        this.permisosService = permisosService;
    }

    @GetMapping
    public String vista(@RequestParam(value = "usuarioId", required = false) Long usuarioId,
                        Model model) {
        List<Modulo> modulos = permisosService.getModulos();
        model.addAttribute("modulos", modulos);
        model.addAttribute("roles", permisosService.getRoles());

        // Conjunto "rol|moduloId" marcado, para pintar la matriz de checkboxes.
        Set<String> marcados = new LinkedHashSet<>();
        for (Modulo m : modulos) {
            for (String rol : permisosService.getRoles()) {
                boolean permite = PermisosService.ROL_ADMIN.equals(rol)
                        ? true
                        : permisosService.rolPermite(rol, m.id());
                if (permite) {
                    marcados.add(rol + "|" + m.id());
                }
            }
        }
        model.addAttribute("marcados", marcados);
        model.addAttribute("usuarios", permisosService.listarUsuarios());

        // Sección de overrides por usuario (si se seleccionó uno).
        if (usuarioId != null) {
            Map<String, Object> usuario = permisosService.buscarUsuario(usuarioId);
            if (usuario != null) {
                model.addAttribute("usuarioSel", usuario);
                Map<Long, Boolean> overrides = permisosService.overridesDeUsuario(usuarioId);
                Map<Long, String> estados = new LinkedHashMap<>();
                for (Modulo m : modulos) {
                    Boolean ov = overrides.get(m.id());
                    estados.put(m.id(), ov == null ? "heredar" : (ov ? "permitir" : "bloquear"));
                }
                model.addAttribute("estadosUsuario", estados);
            }
        }
        return "gestion-permisos/gestion-permisos";
    }

    @PostMapping("/roles")
    public String guardarRoles(@RequestParam(value = "permisos", required = false) List<String> permisos,
                               RedirectAttributes redirect) {
        Set<String> marcados = permisos == null ? Set.of() : new LinkedHashSet<>(permisos);
        permisosService.guardarMatrizRoles(marcados);
        redirect.addFlashAttribute("mensajeOk", "Permisos por rol actualizados correctamente.");
        return "redirect:/gestion-permisos";
    }

    @PostMapping("/usuario")
    public String guardarOverridesUsuario(@RequestParam("usuarioId") long usuarioId,
                                          @RequestParam Map<String, String> params,
                                          RedirectAttributes redirect) {
        for (Modulo m : permisosService.getModulos()) {
            String estado = params.get("estado_" + m.id());
            if (estado != null) {
                permisosService.guardarOverrideUsuario(usuarioId, m.id(), estado);
            }
        }
        redirect.addFlashAttribute("mensajeOk", "Excepciones del usuario actualizadas.");
        return "redirect:/gestion-permisos?usuarioId=" + usuarioId;
    }
}
