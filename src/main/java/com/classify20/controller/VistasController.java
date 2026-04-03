package com.classify20.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;

@Controller
public class VistasController {

    @Value("${classify.webhooks.contacta.url}")
    private String webhookContactaUrl;

    @Value("${classify.webhooks.contacto.url}")
    private String webhookContactoUrl;

    @GetMapping("/agenda")
    public String mostrarAgenda(){
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
    public String mostrarRegisto(){
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
    @GetMapping("/menu")
    public String mostrarMenu(){
        return "menu/menu";
    }
    @GetMapping("/nosotros")
    public String mostrarNosotros(){
        return "nosotros/nosotros";
    }
    @GetMapping("/notas")
    public String mostrarNotas(){
        return "notas/notas";
    }
    @GetMapping("/noticias")
    public String mostrarNoticias(){
        return "noticias/noticias";
    }
    @GetMapping("/politicas")
    public String mostrarPoliticas(){
        return "politicas/politicas";
    }
    @GetMapping("/programacion")
    public String mostrarProgramacion(){
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
