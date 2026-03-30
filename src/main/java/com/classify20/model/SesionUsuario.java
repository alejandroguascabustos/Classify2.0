package com.classify20.model;

public record SesionUsuario(
        long id,
        String nombre,
        String apellido,
        String correo,
        String nombreUsuario,
        String tipoUsuario,
        int perfil,
        String materia) {
}
