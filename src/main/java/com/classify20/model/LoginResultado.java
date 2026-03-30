package com.classify20.model;

public record LoginResultado(
        boolean success,
        String message,
        SesionUsuario usuario) {
}
