package com.classify20.controller;

import com.classify20.model.CargaResultado;
import com.classify20.service.CargaExcelService;
import com.classify20.service.ClassifyDatabaseService;
import com.classify20.service.ActivacionService;
import com.classify20.service.InvitacionTokenService;
import com.classify20.service.ParametrosColegioService;
import com.classify20.service.PlantillaExcelService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulo Gestión de Registros. Solo perfiles 1 (Administrador) y 2 (Coordinador).
 * El control de acceso por perfil lo aplica AuthInterceptor sobre /gestion-registros/**.
 */
@Controller
@RequestMapping("/gestion-registros")
public class GestionRegistrosController {

    private final PlantillaExcelService plantillaExcelService;
    private final CargaExcelService cargaExcelService;
    private final ClassifyDatabaseService databaseService;
    private final InvitacionTokenService invitacionTokenService;
    private final ParametrosColegioService parametrosService;
    private final ActivacionService activacionService;

    public GestionRegistrosController(PlantillaExcelService plantillaExcelService,
                                      CargaExcelService cargaExcelService,
                                      ClassifyDatabaseService databaseService,
                                      InvitacionTokenService invitacionTokenService,
                                      ParametrosColegioService parametrosService,
                                      ActivacionService activacionService) {
        this.plantillaExcelService = plantillaExcelService;
        this.cargaExcelService = cargaExcelService;
        this.databaseService = databaseService;
        this.invitacionTokenService = invitacionTokenService;
        this.parametrosService = parametrosService;
        this.activacionService = activacionService;
    }

    @GetMapping
    public String vista(Model model, HttpSession session) {
        model.addAttribute("pendientes", listarPendientes());
        model.addAttribute("parametros", parametrosService.obtener());
        return "gestion-registros/gestion-registros";
    }

    @PostMapping("/parametros")
    public String guardarParametros(@RequestParam("numGrados") int numGrados,
                                    @RequestParam("numGrupos") int numGrupos,
                                    @RequestParam("materias") String materias,
                                    @RequestParam("nombreColegio") String nombreColegio,
                                    HttpSession session,
                                    RedirectAttributes redirect) {
        Object usuario = session.getAttribute("usuario");
        ParametrosColegioService.Resultado r =
                parametrosService.guardar(numGrados, numGrupos, materias, nombreColegio,
                        usuario == null ? "desconocido" : usuario.toString());
        redirect.addFlashAttribute("parametrosResultado", r);
        return "redirect:/gestion-registros";
    }

    @GetMapping("/plantilla")
    public void descargarPlantilla(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=plantilla_usuarios_classify.xlsx");
        response.getOutputStream().write(plantillaExcelService.generarPlantilla());
        response.flushBuffer();
    }

    @PostMapping("/cargar")
    public String cargar(@RequestParam("archivo") MultipartFile archivo,
                         RedirectAttributes redirect) {
        CargaResultado resultado = cargaExcelService.procesar(archivo);
        redirect.addFlashAttribute("resultado", resultado);
        return "redirect:/gestion-registros";
    }

    @PostMapping("/invitar")
    public String invitar(@RequestParam("correo") String correo,
                          RedirectAttributes redirect) {
        InvitacionTokenService.Invitacion inv = invitacionTokenService.crearInvitacion(correo);
        redirect.addFlashAttribute("invitacion", inv);
        return "redirect:/gestion-registros";
    }

    @PostMapping("/enviar-bienvenida")
    public String enviarBienvenida(RedirectAttributes redirect) {
        ActivacionService.ResultadoMasivo r = activacionService.enviarBienvenidaMasiva();
        redirect.addFlashAttribute("bienvenida", r);
        return "redirect:/gestion-registros";
    }

    /** Lista los usuarios autorizados pendientes de registro para mostrarlos en la tabla. */
    private List<Map<String, Object>> listarPendientes() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, apellido, correo, documento, nombre_usuario, tipo_usuario, materia, grado, grupo, estado, origen, creado_en " +
                "FROM usuarios_pendientes ORDER BY creado_en DESC, id DESC";
        try (Connection conn = databaseService.openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("nombre", (rs.getString("nombre") + " " + rs.getString("apellido")).trim());
                row.put("correo", rs.getString("correo"));
                row.put("documento", rs.getString("documento"));
                row.put("usuario", rs.getString("nombre_usuario"));
                row.put("tipo", rs.getString("tipo_usuario"));
                row.put("materia", rs.getString("materia"));
                row.put("grado", rs.getString("grado"));
                row.put("grupo", rs.getString("grupo"));
                row.put("estado", rs.getString("estado"));
                row.put("origen", rs.getString("origen"));
                lista.add(row);
            }
        } catch (SQLException ignored) {
        }
        return lista;
    }
}
