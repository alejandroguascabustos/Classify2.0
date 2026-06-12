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
                response.put("message", "Agenda guardada!");
                return ResponseEntity.ok(response);
            }
            model.addAttribute("mensaje", "Agenda guardada exitosamente!");
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

    @GetMapping("/api/agendas")
    @ResponseBody
    public ResponseEntity<List<Agenda>> listarAgendasJson() {
        return ResponseEntity.ok(agendaService.listarAgendas());
    }

    @PutMapping("/api/agendas/{id}")
    @ResponseBody
    public ResponseEntity<Object> actualizarAgenda(
            @PathVariable Long id,
            @RequestBody Agenda agendaActualizada) {
        try {
            Agenda actualizada = agendaService.actualizarAgenda(id, agendaActualizada);
            if (actualizada == null) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Agenda no encontrada con ID: " + id);
                return ResponseEntity.status(404).body(err);
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("data", actualizada);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @DeleteMapping("/api/agendas/{id}")
    @ResponseBody
    public ResponseEntity<Object> eliminarAgenda(@PathVariable Long id) {
        try {
            boolean ok = agendaService.eliminarAgenda(id);
            if (!ok) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "No encontrada: " + id);
                return ResponseEntity.status(404).body(err);
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @GetMapping("/programacion/exportarExcel")
    public void exportarExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=programacion.xlsx");
        List<Agenda> agendas = agendaService.listarAgendas();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Programacion");
            CellStyle hs = workbook.createCellStyle();
            hs.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            hs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font hf = workbook.createFont(); hf.setColor(IndexedColors.WHITE.getIndex()); hf.setBold(true);
            hs.setFont(hf); hs.setAlignment(HorizontalAlignment.CENTER);
            hs.setBorderBottom(BorderStyle.THIN); hs.setBorderTop(BorderStyle.THIN);
            hs.setBorderLeft(BorderStyle.THIN); hs.setBorderRight(BorderStyle.THIN);
            CellStyle ds = workbook.createCellStyle();
            ds.setBorderBottom(BorderStyle.THIN); ds.setBorderTop(BorderStyle.THIN);
            ds.setBorderLeft(BorderStyle.THIN); ds.setBorderRight(BorderStyle.THIN);
            Row hr = sheet.createRow(0);
            String[] headers = {"ID","Materia","Profesor","Fecha","Hora Inicio","Curso","Modalidad","Tema","Duracion"};
            for (int i = 0; i < headers.length; i++) { Cell c = hr.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(hs); }
            int ri = 1;
            for (Agenda a : agendas) {
                Row row = sheet.createRow(ri++);
                Cell c0=row.createCell(0); c0.setCellValue(a.getId()!=null?a.getId():0); c0.setCellStyle(ds);
                Cell c1=row.createCell(1); c1.setCellValue(a.getMateria()!=null?a.getMateria():""); c1.setCellStyle(ds);
                Cell c2=row.createCell(2); c2.setCellValue(a.getProfesor()!=null?a.getProfesor():""); c2.setCellStyle(ds);
                Cell c3=row.createCell(3); c3.setCellValue(a.getFecha()!=null?a.getFecha().toString():""); c3.setCellStyle(ds);
                Cell c4=row.createCell(4); c4.setCellValue(a.getHoraInicio()!=null?a.getHoraInicio().toString():""); c4.setCellStyle(ds);
                String curso=(a.getGrado()!=null?a.getGrado():"")+""+(a.getGrupo()!=null?a.getGrupo():"");
                Cell c5=row.createCell(5); c5.setCellValue(curso.trim()); c5.setCellStyle(ds);
                Cell c6=row.createCell(6); c6.setCellValue(a.getModalidad()!=null?a.getModalidad():""); c6.setCellStyle(ds);
                Cell c7=row.createCell(7); c7.setCellValue(a.getTemaPrincipal()!=null?a.getTemaPrincipal():""); c7.setCellStyle(ds);
                Cell c8=row.createCell(8); c8.setCellValue(a.getDuracion()!=null?a.getDuracion()+"min":""); c8.setCellStyle(ds);
            }
            for (int i=0;i<headers.length;i++) sheet.autoSizeColumn(i);
            workbook.write(response.getOutputStream());
        }
    }
}