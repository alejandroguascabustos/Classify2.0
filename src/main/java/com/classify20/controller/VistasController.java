package com.classify20.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller 
public class VistasController {
    

    @GetMapping("/agenda")
    public String mostrarAgenda(){
        return "agenda/agenda";
    }
    @GetMapping("/aprende")
    public String mostrarAprende(){
        return "aprende/aprende";
    }
    @GetMapping("/login")
    public String mostrarLogin(){
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
    public String mostrarContacta(){
        return "contacta/contacta";
    }
    @GetMapping("/contacto")
    public String mostrarContacto(){
        return "contacto/contacto";
    }
    @GetMapping("/inicio")
    public String mostrarInicio(){
        return "inicio/inicio";
    }
    @GetMapping("/materiales")
    public String mostrarMateriales(){
        return "materiales/materiales";
    }
    @GetMapping("/menu")
    public String mostrarMenu(){
        return "menu/menu";
    }
    @GetMapping("/mismateriales")
    public String mostrarMismateriales(){
        return "mismateriales/mismateriales";
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
