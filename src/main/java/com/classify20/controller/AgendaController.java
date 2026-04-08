package com.classify20.controller;

import com.classify20.model.Agenda;
import com.classify20.service.AgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Controller
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;

    //Muestra formulario vacio

    // POST: recibe los datos del formulario y los guarda
    @PostMapping("/guardar-agenda")
    public Object guardarAgenda(
            @ModelAttribute Agenda agenda, 
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            Model model) {
        
        try {
            agendaService.guardarAgenda(agenda);
            
            if ("XMLHttpRequest".equals(requestedWith)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "¡Agenda guardada!");
                return ResponseEntity.ok(response);
            }
            
            model.addAttribute("mensaje", "¡Agenda guardada exitosamente!");
            model.addAttribute("agenda", new Agenda());
            return "agenda/agenda";
            
        } catch (Exception e) {
            if ("XMLHttpRequest".equals(requestedWith)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Error al guardar: " + e.getMessage());
                return ResponseEntity.status(500).body(response);
            }
            model.addAttribute("error", "Error al guardar la agenda.");
            return "agenda/agenda";
        }
    }

    @GetMapping("/programacion/exportarExcel")
    public void exportarExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=programacion.xlsx");

        List<Agenda> agendas = agendaService.listarAgendas();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Programación");

            // Estilos atractivos
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

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Materia", "Profesor", "Fecha", "Hora Inicio", "Curso", "Modalidad", "Tema", "Duracion"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Agenda a : agendas) {
                Row row = sheet.createRow(rowIdx++);
                Cell c0 = row.createCell(0); c0.setCellValue(a.getId() != null ? a.getId() : 0); c0.setCellStyle(dataStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(a.getMateria() != null ? a.getMateria() : ""); c1.setCellStyle(dataStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(a.getProfesor() != null ? a.getProfesor() : ""); c2.setCellStyle(dataStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(a.getFecha() != null ? a.getFecha().toString() : ""); c3.setCellStyle(dataStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(a.getHoraInicio() != null ? a.getHoraInicio().toString() : ""); c4.setCellStyle(dataStyle);
                String curso = (a.getGrado() != null ? a.getGrado() : "") + " " + (a.getGrupo() != null ? a.getGrupo() : "");
                Cell c5 = row.createCell(5); c5.setCellValue(curso.trim()); c5.setCellStyle(dataStyle);
                Cell c6 = row.createCell(6); c6.setCellValue(a.getModalidad() != null ? a.getModalidad() : ""); c6.setCellStyle(dataStyle);
                Cell c7 = row.createCell(7); c7.setCellValue(a.getTemaPrincipal() != null ? a.getTemaPrincipal() : ""); c7.setCellStyle(dataStyle);
                Cell c8 = row.createCell(8); c8.setCellValue(a.getDuracion() != null ? a.getDuracion().toString() + " min" : ""); c8.setCellStyle(dataStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }
}