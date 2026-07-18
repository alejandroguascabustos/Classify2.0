package com.classify20.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parámetros del colegio configurados por el coordinador/administrador.
 * Definen los valores válidos del formulario de registro y de la carga por Excel.
 */
public record ParametrosColegio(int numGrados, int numGrupos, String materiasCsv, String nombreColegio) {

    /** Grados válidos: 1..numGrados (ej. 11 → "1".."11"). */
    public List<String> grados() {
        List<String> lista = new ArrayList<>();
        for (int i = 1; i <= numGrados; i++) {
            lista.add(String.valueOf(i));
        }
        return lista;
    }

    /** Grupos válidos: primeras numGrupos letras (ej. 4 → A, B, C, D). */
    public List<String> grupos() {
        List<String> lista = new ArrayList<>();
        for (int i = 0; i < numGrupos && i < 26; i++) {
            lista.add(String.valueOf((char) ('A' + i)));
        }
        return lista;
    }

    /** Materias válidas, a partir de la lista separada por comas. */
    public List<String> materias() {
        if (materiasCsv == null || materiasCsv.isBlank()) return List.of();
        return Arrays.stream(materiasCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
