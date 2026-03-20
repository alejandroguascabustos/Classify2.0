package com.classify20.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller 
public class VistasController {
    

    @GetMapping("/inicio")
    public String mostrarHome(){
        return "inicio/Inicio";
    }

    @GetMapping("/login")
    public String mostrarLogin(){
        return "auth/login";
    }
    @GetMapping("/registro")
    public String mostrarRegisto(){
        return "auth/registro";
    }
    @GetMapping("/contacto")
    public String mostrarContacto(){
        return "contacto/contacto";
    }

    @GetMapping("/")
    public String mostrarRoot(){
        return "redirect:/inicio";
    }
}
