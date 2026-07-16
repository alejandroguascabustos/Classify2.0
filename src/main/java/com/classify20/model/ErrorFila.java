package com.classify20.model;

/** Un error detectado en una fila del Excel de carga masiva. */
public record ErrorFila(int fila, String usuario, String columna, String valor, String motivo) {
}
