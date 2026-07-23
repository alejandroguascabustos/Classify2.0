package com.classify20.domain;

/**
 * Módulo administrable de la aplicación (una entrada del catálogo `modulos`).
 *
 * @param protegido módulos base que no se pueden bloquear desde la UI
 *                  (p. ej. el menú, siempre accesible; o la propia gestión
 *                  de permisos, exclusiva del administrador).
 */
public record Modulo(
        long id,
        String clave,
        String nombre,
        String ruta,
        String icono,
        int orden,
        boolean enMenu,
        boolean protegido) {
}
