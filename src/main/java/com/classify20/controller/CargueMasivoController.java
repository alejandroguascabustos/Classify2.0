package com.classify20.controller;

import com.classify20.model.Agenda;
import com.classify20.service.AgendaService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cargue masivo de clases agendadas (CLS-133). Controlador independiente del
 * agendamiento individual: descarga la plantilla .xlsx con la identidad visual
 * de Classify y recibe el archivo diligenciado, validando fila a fila (los
 * conflictos de horario los detecta AgendaService igual que en el formulario).
 * Las rutas cuelgan de /agenda, así que AuthInterceptor exige sesión y permiso
 * del módulo "agenda" sin configuración adicional.
 */
@Controller
@RequestMapping("/agenda/cargue-masivo")
@RequiredArgsConstructor
public class CargueMasivoController {

    // Colores de marca (los mismos de style.css y de los exports)
    private static final java.awt.Color VERDE_OSCURO = new java.awt.Color(0x01, 0x35, 0x01);
    private static final java.awt.Color VERDE = new java.awt.Color(0x00, 0x80, 0x00);
    private static final java.awt.Color VERDE_CLARO = new java.awt.Color(0xEB, 0xF3, 0xE8);

    /** Máximo de filas de datos que acepta un cargue. */
    private static final int MAX_FILAS = 200;
    /** Fila (0-based) donde viven los encabezados de la plantilla. */
    private static final int FILA_ENCABEZADOS = 3;

    private static final String[] ENCABEZADOS = {
            "Grado *", "Grupo", "Materia *", "Profesor *", "Fecha *",
            "Hora inicio *", "Duración (min)", "Modalidad", "Tema principal *",
            "Objetivos", "Dificultades", "Materiales básicos", "Recursos necesarios"
    };

    // Las mismas listas que ofrece el formulario de agendamiento
    private static final String[] MATERIAS = {"Matematicas", "Español", "Historia", "Ingles",
            "Etica y valores", "Educación fisica", "Informatica"};
    private static final String[] PROFESORES = {"Ana García", "Carlos Méndez", "Laura Fernández",
            "Jorge Ramírez", "Sofía Torres", "Andrés López", "Marta Ríos"};
    private static final String[] GRADOS = {"1", "2", "3", "4", "5", "6"};
    private static final String[] DURACIONES = {"30", "45", "60", "90", "120"};
    private static final String[] MODALIDADES = {"presencial", "virtual"};
    private static final String[] MATERIALES = {"si", "no", "parcialmente"};

    private static final DateTimeFormatter[] FORMATOS_FECHA = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    private final AgendaService agendaService;

    // ── GET /agenda/cargue-masivo/plantilla → .xlsx para diligenciar ─────
    @GetMapping("/plantilla")
    public void plantilla(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=plantilla-cargue-clases.xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Clases");

            XSSFFont fuenteTitulo = wb.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 15);
            fuenteTitulo.setColor(new XSSFColor(VERDE_OSCURO, null));
            XSSFCellStyle estiloTitulo = (XSSFCellStyle) wb.createCellStyle();
            estiloTitulo.setFont(fuenteTitulo);
            estiloTitulo.setVerticalAlignment(VerticalAlignment.CENTER);

            Row filaTitulo = sheet.createRow(0);
            filaTitulo.setHeightInPoints(26);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue("Classify — Plantilla de cargue masivo de clases");
            celdaTitulo.setCellStyle(estiloTitulo);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            XSSFFont fuenteNota = wb.createFont();
            fuenteNota.setItalic(true);
            fuenteNota.setColor(new XSSFColor(VERDE, null));
            XSSFCellStyle estiloNota = (XSSFCellStyle) wb.createCellStyle();
            estiloNota.setFont(fuenteNota);

            Row filaNota = sheet.createRow(1);
            Cell celdaNota = filaNota.createCell(0);
            celdaNota.setCellValue("Campos con * son obligatorios. Fecha AAAA-MM-DD (ej. 2026-08-10), "
                    + "hora HH:MM (ej. 08:30). No borres la fila de encabezados. Máximo "
                    + MAX_FILAS + " clases por archivo.");
            celdaNota.setCellStyle(estiloNota);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 12));

            XSSFFont fuenteEncabezado = wb.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(new XSSFColor(java.awt.Color.WHITE, null));
            XSSFCellStyle estiloEncabezado = (XSSFCellStyle) wb.createCellStyle();
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(new XSSFColor(VERDE_OSCURO, null));
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloEncabezado.setAlignment(HorizontalAlignment.CENTER);
            bordesFinos(estiloEncabezado);

            Row filaEncabezado = sheet.createRow(FILA_ENCABEZADOS);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(ENCABEZADOS[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            // Zona de datos con fondo alterno suave para guiar el diligenciamiento
            XSSFCellStyle estiloDatoAlterno = (XSSFCellStyle) wb.createCellStyle();
            estiloDatoAlterno.setFillForegroundColor(new XSSFColor(VERDE_CLARO, null));
            estiloDatoAlterno.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            int filaFin = FILA_ENCABEZADOS + MAX_FILAS;
            for (int r = FILA_ENCABEZADOS + 1; r <= filaFin; r++) {
                if ((r - FILA_ENCABEZADOS) % 2 == 0) {
                    Row fila = sheet.createRow(r);
                    for (int c = 0; c < ENCABEZADOS.length; c++) {
                        fila.createCell(c).setCellStyle(estiloDatoAlterno);
                    }
                }
            }

            // Desplegables para los campos cerrados
            validacionLista(sheet, GRADOS, 0);
            validacionLista(sheet, MATERIAS, 2);
            validacionLista(sheet, PROFESORES, 3);
            validacionLista(sheet, DURACIONES, 6);
            validacionLista(sheet, MODALIDADES, 7);
            validacionLista(sheet, MATERIALES, 11);

            // La fecha y la hora van como texto: así el parseo no depende
            // del formato regional del Excel de cada docente.
            int[] anchos = {2200, 2200, 4500, 5200, 3600, 3600, 3900, 3600, 7000, 7000, 7000, 4700, 7000};
            for (int i = 0; i < anchos.length; i++) {
                sheet.setColumnWidth(i, anchos[i]);
            }
            sheet.createFreezePane(0, FILA_ENCABEZADOS + 1);

            wb.write(response.getOutputStream());
        }
    }

    // ── POST /agenda/cargue-masivo → procesa el .xlsx diligenciado ───────
    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cargar(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return error(400, "Selecciona el archivo .xlsx diligenciado.");
        }
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        if (!nombre.toLowerCase().endsWith(".xlsx")) {
            return error(400, "El archivo debe ser la plantilla en formato .xlsx.");
        }

        List<Map<String, Object>> fallidas = new ArrayList<>();
        int guardadas = 0;
        int total = 0;

        try (Workbook wb = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Row encabezado = sheet.getRow(FILA_ENCABEZADOS);
            if (encabezado == null || !texto(encabezado.getCell(0)).toLowerCase().startsWith("grado")) {
                return error(400, "El archivo no corresponde a la plantilla: descárgala de nuevo y no borres los encabezados.");
            }

            for (int r = FILA_ENCABEZADOS + 1; r <= sheet.getLastRowNum(); r++) {
                Row fila = sheet.getRow(r);
                if (fila == null || filaVacia(fila)) continue;
                total++;
                if (total > MAX_FILAS) {
                    fallidas.add(fallo(r + 1, "Se supera el máximo de " + MAX_FILAS + " clases por archivo; el resto no se procesó."));
                    break;
                }
                try {
                    agendaService.guardarAgenda(leerFila(fila));
                    guardadas++;
                } catch (IllegalStateException e) {
                    // Conflicto de horario detectado por el servicio
                    fallidas.add(fallo(r + 1, e.getMessage()));
                } catch (IllegalArgumentException e) {
                    fallidas.add(fallo(r + 1, e.getMessage()));
                } catch (Exception e) {
                    fallidas.add(fallo(r + 1, "Error inesperado al guardar la fila."));
                }
            }
        } catch (IOException e) {
            return error(400, "No fue posible leer el archivo. Verifica que sea la plantilla .xlsx sin dañar.");
        }

        if (total == 0) {
            return error(400, "La plantilla no tiene clases diligenciadas.");
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", fallidas.isEmpty());
        resp.put("total", total);
        resp.put("guardadas", guardadas);
        resp.put("fallidas", fallidas);
        return ResponseEntity.ok(resp);
    }

    // ── Lectura y validación de una fila ─────────────────────────────────

    private Agenda leerFila(Row fila) {
        Agenda a = new Agenda();

        String grado = texto(fila.getCell(0));
        if (grado.isEmpty()) throw new IllegalArgumentException("Falta el grado.");
        try {
            int g = (int) Double.parseDouble(grado);
            if (g < 1 || g > 6) throw new NumberFormatException();
            a.setGrado(g);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Grado inválido: \"" + grado + "\" (debe ser 1 a 6).");
        }

        a.setGrupo(vacioANull(texto(fila.getCell(1))));

        String materia = texto(fila.getCell(2));
        if (materia.isEmpty()) throw new IllegalArgumentException("Falta la materia.");
        a.setMateria(materia);

        String profesor = texto(fila.getCell(3));
        if (profesor.isEmpty()) throw new IllegalArgumentException("Falta el profesor.");
        a.setProfesor(profesor);

        a.setFecha(leerFecha(fila.getCell(4)));
        a.setHoraInicio(leerHora(fila.getCell(5)));

        String duracion = texto(fila.getCell(6)).replace("min", "").trim();
        if (duracion.isEmpty()) {
            a.setDuracion(60);
        } else {
            try {
                int d = (int) Double.parseDouble(duracion);
                if (d < 15 || d > 240) throw new NumberFormatException();
                a.setDuracion(d);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Duración inválida: \"" + duracion + "\" (usa 30, 45, 60, 90 o 120).");
            }
        }

        String modalidad = texto(fila.getCell(7)).toLowerCase();
        if (modalidad.isEmpty()) modalidad = "presencial";
        if (!modalidad.equals("presencial") && !modalidad.equals("virtual")) {
            throw new IllegalArgumentException("Modalidad inválida: \"" + modalidad + "\" (presencial o virtual).");
        }
        a.setModalidad(modalidad);

        String tema = texto(fila.getCell(8));
        if (tema.isEmpty()) throw new IllegalArgumentException("Falta el tema principal.");
        a.setTemaPrincipal(tema);

        a.setObjetivos(vacioANull(texto(fila.getCell(9))));
        a.setDificultades(vacioANull(texto(fila.getCell(10))));

        String materiales = texto(fila.getCell(11)).toLowerCase();
        if (!materiales.isEmpty()) {
            if (!materiales.equals("si") && !materiales.equals("no") && !materiales.equals("parcialmente")) {
                throw new IllegalArgumentException("Materiales básicos inválido: \"" + materiales + "\" (si, no o parcialmente).");
            }
            a.setMaterialesBasicos(materiales);
        }
        a.setRecursosNecesarios(vacioANull(texto(fila.getCell(12))));
        return a;
    }

    private LocalDate leerFecha(Cell celda) {
        if (celda != null && celda.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celda)) {
            return celda.getLocalDateTimeCellValue().toLocalDate();
        }
        String crudo = texto(celda);
        if (crudo.isEmpty()) throw new IllegalArgumentException("Falta la fecha.");
        for (DateTimeFormatter f : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(crudo, f);
            } catch (Exception ignorada) {
                // prueba el siguiente formato
            }
        }
        throw new IllegalArgumentException("Fecha inválida: \"" + crudo + "\" (usa AAAA-MM-DD).");
    }

    private LocalTime leerHora(Cell celda) {
        if (celda != null && celda.getCellType() == CellType.NUMERIC) {
            // Excel guarda las horas como fracción de día
            double v = celda.getNumericCellValue();
            if (v >= 0 && v < 1) {
                int minutosDia = (int) Math.round(v * 24 * 60);
                return LocalTime.of(minutosDia / 60, minutosDia % 60);
            }
            if (DateUtil.isCellDateFormatted(celda)) {
                return celda.getLocalDateTimeCellValue().toLocalTime();
            }
        }
        String crudo = texto(celda);
        if (crudo.isEmpty()) throw new IllegalArgumentException("Falta la hora de inicio.");
        try {
            return LocalTime.parse(crudo.length() == 4 ? "0" + crudo : crudo);
        } catch (Exception e) {
            throw new IllegalArgumentException("Hora inválida: \"" + crudo + "\" (usa HH:MM, ej. 08:30).");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private final DataFormatter formatter = new DataFormatter();

    private String texto(Cell celda) {
        return celda == null ? "" : formatter.formatCellValue(celda).trim();
    }

    private boolean filaVacia(Row fila) {
        for (int c = 0; c < ENCABEZADOS.length; c++) {
            if (!texto(fila.getCell(c)).isEmpty()) return false;
        }
        return true;
    }

    private static String vacioANull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private static Map<String, Object> fallo(int filaExcel, String mensaje) {
        Map<String, Object> m = new HashMap<>();
        m.put("fila", filaExcel);
        m.put("mensaje", mensaje);
        return m;
    }

    private static ResponseEntity<Map<String, Object>> error(int status, String mensaje) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("message", mensaje);
        return ResponseEntity.status(status).body(m);
    }

    private static void bordesFinos(XSSFCellStyle estilo) {
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
    }

    private static void validacionLista(Sheet sheet, String[] valores, int columna) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(valores);
        CellRangeAddressList rango = new CellRangeAddressList(
                FILA_ENCABEZADOS + 1, FILA_ENCABEZADOS + MAX_FILAS, columna, columna);
        DataValidation validacion = helper.createValidation(constraint, rango);
        validacion.setSuppressDropDownArrow(true);
        validacion.setShowErrorBox(true);
        sheet.addValidationData(validacion);
    }
}
