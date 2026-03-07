package com.classify20.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller 
public class VistasController {
    
    @GetMapping("/inicio")
    public String mostrarHome(){
        return "inicio/inicio"; 
    }
}