package com.classify20.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller 
public class VistasController {
    
    @GetMapping("/login")
    public String mostrarLogin(){
        return "auth/login";
    }
    @GetMapping("/inicio")
    public String mostrarHome(){
        return "inicio/Inicio";
    }

    @GetMapping("/")
    public String mostrarRoot(){
        return "redirect:/inicio";
    }
}
