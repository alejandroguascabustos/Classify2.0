package com.classify20.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.classify20.model.Agenda;
import com.classify20.model.TokenValidacion;
import com.classify20.service.NoticiaService;
import com.classify20.service.AgendaService;
import com.classify20.service.InvitacionTokenService;
import com.classify20.service.ParametrosColegioService;

import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
public class VistasController {

    @Autowired
    private NoticiaService noticiaService;

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private InvitacionTokenService invitacionTokenService;

    @Autowired
    private ParametrosColegioService parametrosService;

    @Value("${classify.registro.solo-invitacion:false}")
    private boolean soloInvitacion;

    @Value("${classify.webhooks.contacta.url}")
    private String webhookContactaUrl;
 
    @Value("${classify.webhooks.contacto.url}")
    private String webhookContactoUrl;
 
    // ── Menú principal: pasa la noticia más reciente ──────────
    @GetMapping("/menu")
    public String mostrarMenu(Model model) {
        noticiaService.buscarMasReciente()
                      .ifPresent(n -> model.addAttribute("noticiaReciente", n));
        return "menu/menu";
    }

    @GetMapping("/agenda")
    public String mostrarAgenda(Model model) {
        model.addAttribute("agenda", new Agenda());
        return "agenda/agenda";
    }


    @GetMapping("/aprende")
    public String mostrarAprende(){
        return "aprende/aprende";
    }
    @GetMapping("/login")
    public String mostrarLogin(HttpSession session){
        if (session.getAttribute("usuarioId") != null) {
            return "redirect:/menu";
        }
        return "auth/login";
    }
    @GetMapping("/registro")
    public String mostrarRegisto(@RequestParam(value = "token", required = false) String token, Model model){
        // Grados, grupos y materias válidos según los parámetros del colegio
        model.addAttribute("parametros", parametrosService.obtener());

        if (token != null && !token.isBlank()) {
            TokenValidacion tv = invitacionTokenService.validar(token);
            if (tv.valido()) {
                model.addAttribute("tokenValido", true);
                model.addAttribute("token", token);
                model.addAttribute("correoToken", tv.correo());
            } else {
                model.addAttribute("tokenValido", false);
                model.addAttribute("mensajeToken", tv.motivo());
            }
        } else if (soloInvitacion) {
            model.addAttribute("tokenValido", false);
            model.addAttribute("mensajeToken",
                    "El registro es solo por invitación. Solicite acceso al administrador.");
        } else {
            model.addAttribute("tokenValido", true);
        }
        return "auth/registro";
    }
    @GetMapping("/califica")
    public String mostrarCalifica(){
        return "califica/califica";
    }
    @GetMapping("/conoce")
    public String mostrarConoce(){
        return "conoce/conoce";
    }
    @GetMapping("/contacta")
    public String mostrarContacta(Model model){
        model.addAttribute("webhookUrl", webhookContactaUrl);
        return "contacta/contacta";
    }
    @GetMapping("/contacto")
    public String mostrarContacto(Model model){
        model.addAttribute("webhookUrl", webhookContactoUrl);
        return "contacto/contacto";
    }
    @GetMapping("/inicio")
    public String mostrarInicio(){
        return "inicio/inicio";
    }
    @GetMapping("/nosotros")
    public String mostrarNosotros(){
        return "nosotros/nosotros";
    }
    @GetMapping("/notas")
    public String mostrarNotas(){
        return "notas/notas";
    }

    @GetMapping("/politicas")
    public String mostrarPoliticas(){
        return "politicas/politicas";
    }
    @GetMapping("/programacion")
    public String mostrarProgramacion(Model model){
        model.addAttribute("clases", agendaService.listarAgendas());
        return "programacion/programacion";
    }
    @GetMapping("/soporte")
    public String mostrarSoporte(){
        return "soporte/soporte";
    }
    @GetMapping("/terminos")
    public String mostrarTerminos(){
        return "terminos/terminos";
    }

    @GetMapping("/")
    public String mostrarRoot(){
        return "redirect:/inicio";
    }
}
