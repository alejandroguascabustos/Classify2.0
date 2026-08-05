package com.classify20.model;

/**
 * Datos del usuario que viajan a la sesion tras un login correcto.
 *
 * <p>{@code debeCambiarPassword} indica si la cuenta sigue usando la clave
 * temporal enviada por correo (n8n) y debe pasar por la pantalla de cambio
 * obligatorio antes de entrar al resto de la aplicacion.
 */
public record SesionUsuario(
        long id,
        String nombre,
        String apellido,
        String correo,
        String nombreUsuario,
        String tipoUsuario,
        int perfil,
        String materia,
        boolean debeCambiarPassword) {
}
