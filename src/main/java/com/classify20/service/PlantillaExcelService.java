package com.classify20.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Genera la plantilla oficial (.xlsx) para la carga masiva de usuarios.
 * Las columnas son exactamente las del formulario de registro.
 */
@Service
public class PlantillaExcelService {

    /** Columnas oficiales, en orden. Deben coincidir con {@link CargaExcelService}. */
    public static final String[] COLUMNAS = {
            "nombre", "apellido", "correo", "documento", "telefono",
            "nombre_usuario", "tipo_usuario", "materia", "grado", "grupo"
    };

    /** Índices de columna (para no usar números mágicos). */
    public static final int COL_NOMBRE = 0, COL_APELLIDO = 1, COL_CORREO = 2, COL_DOCUMENTO = 3,
            COL_TELEFONO = 4, COL_USUARIO = 5, COL_TIPO = 6, COL_MATERIA = 7, COL_GRADO = 8, COL_GRUPO = 9;

    /** Dos filas de ejemplo: un docente (con materia) y un estudiante (con grado y grupo). */
    private static final String[][] EJEMPLOS = {
            {"Ana", "García", "ana@colegio.com", "1234567890", "3221112233", "ana123", "docente", "Matematicas", "", ""},
            {"Jose", "Arteaga", "jose@colegio.com", "1098765432", "3229998877", "jose123", "estudiante", "", "10", "A"}
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

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
