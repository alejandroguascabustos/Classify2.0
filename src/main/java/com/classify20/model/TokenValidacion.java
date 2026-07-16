package com.classify20.model;

/** Resultado de validar un token de invitación. */
public record TokenValidacion(
        boolean valido,
        String motivo,
        Long tokenId,
        String correo,
        Long usuarioPendienteId) {

    public static TokenValidacion invalido(String motivo) {
        return new TokenValidacion(false, motivo, null, null, null);
    }
}
