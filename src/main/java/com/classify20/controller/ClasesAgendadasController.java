package com.classify20.controller;

import com.classify20.model.Agenda;
import com.classify20.service.AgendaService;
import com.classify20.service.AgendaService.CursoOption;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Vista de consulta de clases agendadas (CLS-122): filtro por curso, profesor
 * y materia, con exportación del resultado filtrado a Excel y PDF con la
 * identidad visual de Classify. El acceso lo controla AuthInterceptor a través
 * del módulo "clases-agendadas" registrado en PermisosService.
 */
@Controller
@RequestMapping("/clases-agendadas")
@RequiredArgsConstructor
public class ClasesAgendadasController {

    // Colores de marca (los mismos --green-dark / --green / --green-light de style.css)
    private static final java.awt.Color VERDE_OSCURO = new java.awt.Color(0x01, 0x35, 0x01);
    private static final java.awt.Color VERDE = new java.awt.Color(0x00, 0x80, 0x00);
    private static final java.awt.Color VERDE_CLARO = new java.awt.Color(0xEB, 0xF3, 0xE8);

    private static final DateTimeFormatter FORMATO_GENERACION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Tamaño de página de la tabla (CLS-132); los exports y el dashboard siguen usando el resultado completo. */
    private static final int CLASES_POR_PAGINA = 10;

    private final AgendaService agendaService;
    private final SpringTemplateEngine templateEngine;

    /** URI file:/// del logo, extraído del jar una sola vez (Flying Saucer y POI lo leen de disco). */
    private volatile String logoUriCache;

    // ── GET /clases-agendadas → vista con filtros ────────────────────────
    @GetMapping
    public String vista(@RequestParam(required = false) String curso,
                        @RequestParam(required = false) String profesor,
                        @RequestParam(required = false) String materia,
                        @RequestParam(defaultValue = "1") int pagina,
                        Model model) {
        List<Agenda> filtradas = agendaService.filtrarClases(curso, profesor, materia);

        int totalPaginas = Math.max(1, (int) Math.ceil(filtradas.size() / (double) CLASES_POR_PAGINA));
        if (pagina < 1) pagina = 1;
        if (pagina > totalPaginas) pagina = totalPaginas;
        int desde = (pagina - 1) * CLASES_POR_PAGINA;
        int hasta = Math.min(desde + CLASES_POR_PAGINA, filtradas.size());

        model.addAttribute("clases", filtradas.subList(desde, hasta));
        model.addAttribute("totalClases", filtradas.size());
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("desde", filtradas.isEmpty() ? 0 : desde + 1);
        model.addAttribute("hasta", hasta);
        model.addAttribute("paginas", numerosDePagina(pagina, totalPaginas));
        model.addAttribute("cursos", agendaService.listarCursos());
        model.addAttribute("profesores", agendaService.listarProfesores());
        model.addAttribute("materias", agendaService.listarMaterias());
        model.addAttribute("filtroCurso", curso == null ? "" : curso);
        model.addAttribute("filtroProfesor", profesor == null ? "" : profesor);
        model.addAttribute("filtroMateria", materia == null ? "" : materia);
        return "clases-agendadas/clases-agendadas";
    }

    // ── GET /clases-agendadas/dashboard → gráficos de barras y torta ─────
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String curso,
                            @RequestParam(required = false) String profesor,
                            @RequestParam(required = false) String materia,
                            @RequestParam(defaultValue = "curso") String agrupar,
                            Model model) {
        if (!List.of("curso", "profesor", "materia").contains(agrupar)) {
            agrupar = "curso";
        }
        List<Agenda> clases = agendaService.filtrarClases(curso, profesor, materia);
        Map<String, Long> conteo = agendaService.contarPorDimension(clases, agrupar);

        // La torta pierde legibilidad con muchas porciones: top 5 + "Otros"
        List<String> tortaLabels = new java.util.ArrayList<>();
        List<Long> tortaValores = new java.util.ArrayList<>();
        long otros = 0;
        for (Map.Entry<String, Long> e : conteo.entrySet()) {
            if (tortaLabels.size() < 5) {
                tortaLabels.add(e.getKey());
                tortaValores.add(e.getValue());
            } else {
                otros += e.getValue();
            }
        }
        if (otros > 0) {
            tortaLabels.add("Otros");
            tortaValores.add(otros);
        }

        model.addAttribute("cursos", agendaService.listarCursos());
        model.addAttribute("profesores", agendaService.listarProfesores());
        model.addAttribute("materias", agendaService.listarMaterias());
        model.addAttribute("filtroCurso", curso == null ? "" : curso);
        model.addAttribute("filtroProfesor", profesor == null ? "" : profesor);
        model.addAttribute("filtroMateria", materia == null ? "" : materia);
        model.addAttribute("agrupar", agrupar);
        model.addAttribute("barraLabels", new java.util.ArrayList<>(conteo.keySet()));
        model.addAttribute("barraValores", new java.util.ArrayList<>(conteo.values()));
        model.addAttribute("tortaLabels", tortaLabels);
        model.addAttribute("tortaValores", tortaValores);
        model.addAttribute("totalClases", clases.size());
        model.addAttribute("totalProfesores", contarDistintos(clases, Agenda::getProfesor));
        model.addAttribute("totalMaterias", contarDistintos(clases, Agenda::getMateria));
        model.addAttribute("totalCursos", clases.stream()
                .map(AgendaService::etiquetaCursoDe)
                .filter(c -> !c.isEmpty())
                .distinct().count());
        return "clases-agendadas/dashboard";
    }

    /** Ventana de hasta 5 números de página centrada en la actual (1 … total). */
    private static List<Integer> numerosDePagina(int actual, int total) {
        int desde = Math.max(1, actual - 2);
        int hasta = Math.min(total, desde + 4);
        desde = Math.max(1, hasta - 4);
        List<Integer> numeros = new java.util.ArrayList<>();
        for (int i = desde; i <= hasta; i++) {
            numeros.add(i);
        }
        return numeros;
    }

    private static long contarDistintos(List<Agenda> clases, java.util.function.Function<Agenda, String> campo) {
        return clases.stream()
                .map(campo)
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase())
                .distinct().count();
    }

    // ── GET /clases-agendadas/exportar-excel → .xlsx con marca ───────────
    @GetMapping("/exportar-excel")
    public void exportarExcel(@RequestParam(required = false) String curso,
                              @RequestParam(required = false) String profesor,
                              @RequestParam(required = false) String materia,
                              HttpServletResponse response) throws IOException {
        List<Agenda> clases = agendaService.filtrarClases(curso, profesor, materia);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=clases-agendadas.xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Clases agendadas");

            // ── Cabecera de marca: logo + título + filtros ──
            insertarLogo(wb, sheet);

            XSSFFont fuenteTitulo = wb.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 16);
            fuenteTitulo.setColor(new XSSFColor(VERDE_OSCURO, null));
            XSSFCellStyle estiloTitulo = wb.createCellStyle();
            estiloTitulo.setFont(fuenteTitulo);
            estiloTitulo.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFRow filaTitulo = sheet.createRow(0);
            filaTitulo.setHeightInPoints(30);
            XSSFCell celdaTitulo = filaTitulo.createCell(2);
            celdaTitulo.setCellValue("Classify — Clases agendadas");
            celdaTitulo.setCellStyle(estiloTitulo);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 7));

            XSSFFont fuenteFiltros = wb.createFont();
            fuenteFiltros.setItalic(true);
            fuenteFiltros.setColor(new XSSFColor(VERDE, null));
            XSSFCellStyle estiloFiltros = wb.createCellStyle();
            estiloFiltros.setFont(fuenteFiltros);

            XSSFRow filaFiltros = sheet.createRow(1);
            XSSFCell celdaFiltros = filaFiltros.createCell(2);
            celdaFiltros.setCellValue(resumenFiltros(curso, profesor, materia)
                    + "  ·  Generado el " + FORMATO_GENERACION.format(LocalDateTime.now()));
            celdaFiltros.setCellStyle(estiloFiltros);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 7));

            // ── Encabezados de la tabla ──
            XSSFFont fuenteEncabezado = wb.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(new XSSFColor(java.awt.Color.WHITE, null));
            XSSFCellStyle estiloEncabezado = wb.createCellStyle();
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(new XSSFColor(VERDE_OSCURO, null));
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloEncabezado.setAlignment(HorizontalAlignment.CENTER);
            bordesFinos(estiloEncabezado);

            XSSFCellStyle estiloDato = wb.createCellStyle();
            bordesFinos(estiloDato);

            XSSFCellStyle estiloDatoAlterno = wb.createCellStyle();
            estiloDatoAlterno.setFillForegroundColor(new XSSFColor(VERDE_CLARO, null));
            estiloDatoAlterno.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bordesFinos(estiloDatoAlterno);

            String[] encabezados = {"N°", "Materia", "Profesor", "Fecha", "Hora inicio",
                    "Duración", "Curso", "Modalidad", "Tema principal"};
            int filaInicioTabla = 4;
            XSSFRow filaEncabezado = sheet.createRow(filaInicioTabla);
            for (int i = 0; i < encabezados.length; i++) {
                XSSFCell celda = filaEncabezado.createCell(i);
                celda.setCellValue(encabezados[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            // ── Datos ──
            int fila = filaInicioTabla + 1;
            int numero = 1;
            for (Agenda a : clases) {
                XSSFRow r = sheet.createRow(fila);
                XSSFCellStyle estilo = (numero % 2 == 0) ? estiloDatoAlterno : estiloDato;
                String[] valores = {
                        String.valueOf(numero),
                        texto(a.getMateria()),
                        texto(a.getProfesor()),
                        a.getFecha() != null ? a.getFecha().toString() : "",
                        a.getHoraInicio() != null ? a.getHoraInicio().toString() : "",
                        a.getDuracion() != null ? a.getDuracion() + " min" : "",
                        cursoDe(a),
                        texto(a.getModalidad()),
                        texto(a.getTemaPrincipal())
                };
                for (int i = 0; i < valores.length; i++) {
                    XSSFCell celda = r.createCell(i);
                    celda.setCellValue(valores[i]);
                    celda.setCellStyle(estilo);
                }
                fila++;
                numero++;
            }

            for (int i = 0; i < encabezados.length; i++) {
                sheet.autoSizeColumn(i);
                // margen extra para que el contenido no quede pegado al borde
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 255 * 256));
            }
            sheet.createFreezePane(0, filaInicioTabla + 1);

            wb.write(response.getOutputStream());
        }
    }

    // ── GET /clases-agendadas/exportar-pdf → PDF con marca ───────────────
    @GetMapping("/exportar-pdf")
    public void exportarPdf(@RequestParam(required = false) String curso,
                            @RequestParam(required = false) String profesor,
                            @RequestParam(required = false) String materia,
                            HttpServletResponse response) throws IOException {
        List<Agenda> clases = agendaService.filtrarClases(curso, profesor, materia);

        Context contexto = new Context();
        contexto.setVariable("clases", clases);
        contexto.setVariable("filtroCurso", agendaService.etiquetaCurso(curso));
        contexto.setVariable("filtroProfesor", profesor != null && !profesor.isBlank() ? profesor : null);
        contexto.setVariable("filtroMateria", materia != null && !materia.isBlank() ? materia : null);
        contexto.setVariable("resumenFiltros", resumenFiltros(curso, profesor, materia));
        contexto.setVariable("fechaGeneracion", FORMATO_GENERACION.format(LocalDateTime.now()));
        contexto.setVariable("logoUri", logoUri());

        String html = templateEngine.process("clases-agendadas/clasesAgendadasPdf", contexto);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"clases-agendadas.pdf\"");

        try {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(response.getOutputStream());
        } catch (Exception e) {
            throw new IOException("Error generando el PDF de clases agendadas: " + e.getMessage(), e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** "Curso: 3° A · Profesor: Ana García · Materia: Todas" según filtros activos. */
    private String resumenFiltros(String curso, String profesor, String materia) {
        String etiquetaCurso = agendaService.etiquetaCurso(curso);
        return "Curso: " + (etiquetaCurso != null ? etiquetaCurso : "Todos")
                + "  ·  Profesor: " + (profesor != null && !profesor.isBlank() ? profesor : "Todos")
                + "  ·  Materia: " + (materia != null && !materia.isBlank() ? materia : "Todas");
    }

    private static String cursoDe(Agenda a) {
        if (a.getGrado() == null) return "";
        String grupo = a.getGrupo() != null ? a.getGrupo().trim().toUpperCase() : "";
        return a.getGrado() + "°" + (grupo.isEmpty() ? "" : " " + grupo);
    }

    private static String texto(String v) {
        return v != null ? v : "";
    }

    private static void bordesFinos(XSSFCellStyle estilo) {
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
    }

    /** Inserta el logo (si existe) anclado en A1, escalado a ~55 px de alto. */
    private void insertarLogo(XSSFWorkbook wb, XSSFSheet sheet) {
        try (InputStream in = getClass().getResourceAsStream("/static/img/logo.png")) {
            if (in == null) return;
            byte[] bytes = in.readAllBytes();
            int indice = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
            XSSFDrawing dibujo = sheet.createDrawingPatriarch();
            XSSFClientAnchor ancla = new XSSFClientAnchor();
            ancla.setCol1(0);
            ancla.setRow1(0);
            XSSFPicture imagen = dibujo.createPicture(ancla, indice);
            // El PNG original mide 3642x1801: 0.03 lo deja en ~109x54 px
            imagen.resize(0.03);
        } catch (IOException e) {
            // sin logo el Excel sigue siendo válido
        }
    }

    /**
     * Flying Saucer no puede leer imágenes dentro del jar, así que el logo se
     * copia una única vez a un archivo temporal y se referencia con file:///.
     */
    private String logoUri() {
        String cache = logoUriCache;
        if (cache != null) return cache;
        try (InputStream in = getClass().getResourceAsStream("/static/img/logo.png")) {
            if (in == null) return null;
            Path tmp = Files.createTempFile("classify-logo-", ".png");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();
            logoUriCache = tmp.toUri().toString();
            return logoUriCache;
        } catch (IOException e) {
            return null;
        }
    }
}
