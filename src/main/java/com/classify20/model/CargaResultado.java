package com.classify20.model;

import java.util.List;

/**
 * Resultado de procesar el Excel de carga masiva.
 * La carga es todo-o-nada: si hay errores, no se guarda nada y se devuelve el reporte.
 */
public record CargaResultado(
        boolean success,
        int guardados,
        int totalFilas,
        List<ErrorFila> errores,
        String mensaje) {
}
