package com.classify20.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * Genera la plantilla oficial (.xlsx) para la carga masiva de usuarios.
 * Las columnas son exactamente las del formulario de registro.
 *
 * Reglas del formato (ver {@link CargaExcelService}):
 *  - Docente:    materia(s) que dicta (una o varias separadas por coma) + cursos(s)
 *                donde las dicta (uno o varios, ej. "5B, 6A"). No lleva grado/grupo.
 *  - Estudiante: grado + grupo (su curso), en dos columnas separadas. No lleva materia/cursos.
 *  - Acudiente / Coordinador: solo datos personales.
 */
@Service
public class PlantillaExcelService {

    /** Columnas oficiales, en orden. Deben coincidir con {@link CargaExcelService}. */
    public static final String[] COLUMNAS = {
            "nombre", "apellido", "correo", "documento", "telefono",
            "nombre_usuario", "tipo_usuario", "materia", "cursos", "grado", "grupo"
    };

    /** Índices de columna (para no usar números mágicos). */
    public static final int COL_NOMBRE = 0, COL_APELLIDO = 1, COL_CORREO = 2, COL_DOCUMENTO = 3,
            COL_TELEFONO = 4, COL_USUARIO = 5, COL_TIPO = 6, COL_MATERIA = 7, COL_CURSOS = 8,
            COL_GRADO = 9, COL_GRUPO = 10;

    /**
     * Filas de ejemplo (guía; el sistema las ignora al cargar).
     * Dos docentes (uno con varias materias/cursos, otro con una sola) y dos estudiantes.
     */
    private static final String[][] EJEMPLOS = {
            {"Ernesto", "Ríos", "ernesto@ejemplo.com", "1111111111", "3001112233", "ernesto",
                    "docente", "Sociales, Ciencias Politicas, Educación fisica, Religion", "5B, 6A, 7C", "", ""},
            {"Marta", "López", "marta@ejemplo.com", "2222222222", "3002223344", "marta",
                    "docente", "Español", "8A, 8B", "", ""},
            {"Jhon", "Pérez", "jhon@ejemplo.com", "3333333333", "3003334455", "jhon",
                    "estudiante", "", "", "5", "B"},
            {"Juan", "Gómez", "juan@ejemplo.com", "4444444444", "3004445566", "juan",
                    "estudiante", "", "", "2", "A"}
    };

    /** Correos de las filas de ejemplo: se ignoran al cargar aunque el usuario no las borre. */
    public static final Set<String> CORREOS_EJEMPLO = Set.of(
            "ernesto@ejemplo.com", "marta@ejemplo.com", "jhon@ejemplo.com", "juan@ejemplo.com");

    /** Instrucciones que se muestran en la segunda hoja de la plantilla. */
    private static final String[] INSTRUCCIONES = {
            "INSTRUCCIONES DE LLENADO",
            "",
            "1. No cambies ni borres la fila de encabezados (fila 1) de la hoja \"Usuarios\".",
            "2. Las filas de ejemplo (en gris) son solo guía; puedes borrarlas o dejarlas: el sistema las ignora.",
            "3. Cada persona va en su propia fila. Correo, documento y nombre_usuario no pueden repetirse.",
            "4. tipo_usuario debe ser exactamente uno de: estudiante, docente, acudiente, coordinador.",
            "",
            "DOCENTE:",
            "  - materia: una o varias materias que dicta, separadas por coma. Ej: Sociales, Religion.",
            "            Deben existir en los Parámetros del colegio.",
            "  - cursos:  uno o varios cursos donde dicta, separados por coma. Ej: 5B, 6A, 7C.",
            "            Cada curso es grado + grupo pegados (5B = grado 5, grupo B).",
            "  - grado y grupo: DÉJALOS VACÍOS para docentes.",
            "",
            "ESTUDIANTE:",
            "  - grado: el número de grado. Ej: 5.",
            "  - grupo: la letra del grupo. Ej: B.",
            "  - materia y cursos: DÉJALOS VACÍOS para estudiantes.",
            "",
            "ACUDIENTE y COORDINADOR:",
            "  - Solo datos personales. Deja vacías las columnas materia, cursos, grado y grupo.",
            "",
            "Si un dato es inválido, la carga se rechaza completa y se indica la fila y el motivo.",
            "Corrige y vuelve a subir el archivo."
    };

    public byte[] generarPlantilla() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Usuarios");

            // Estilo del encabezado (verde Classify, texto blanco en negrita)
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle ejemploStyle = workbook.createCellStyle();
            Font ejemploFont = workbook.createFont();
            ejemploFont.setItalic(true);
            ejemploFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            ejemploStyle.setFont(ejemploFont);

            // Fila 0: encabezados
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUMNAS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COLUMNAS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Filas de ejemplo (guía; el sistema las ignora al cargar)
            for (int r = 0; r < EJEMPLOS.length; r++) {
                Row ejemploRow = sheet.createRow(r + 1);
                for (int i = 0; i < EJEMPLOS[r].length; i++) {
                    Cell cell = ejemploRow.createCell(i);
                    cell.setCellValue(EJEMPLOS[r][i]);
                    cell.setCellStyle(ejemploStyle);
                }
            }

            for (int i = 0; i < COLUMNAS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Segunda hoja: instrucciones de llenado
            escribirInstrucciones(workbook);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void escribirInstrucciones(Workbook workbook) {
        Sheet hoja = workbook.createSheet("Instrucciones");

        CellStyle tituloStyle = workbook.createCellStyle();
        Font tituloFont = workbook.createFont();
        tituloFont.setBold(true);
        tituloFont.setColor(IndexedColors.DARK_GREEN.getIndex());
        tituloStyle.setFont(tituloFont);

        for (int r = 0; r < INSTRUCCIONES.length; r++) {
            Row fila = hoja.createRow(r);
            Cell celda = fila.createCell(0);
            celda.setCellValue(INSTRUCCIONES[r]);
            if (r == 0) celda.setCellStyle(tituloStyle);
        }
        hoja.setColumnWidth(0, 90 * 256);
    }
}
