package com.classify20.service;

import com.classify20.model.CargaResultado;
import com.classify20.model.ErrorFila;
import com.classify20.model.ParametrosColegio;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Lee el Excel de carga masiva, valida cada fila y —solo si TODO es válido—
 * guarda los usuarios en la tabla usuarios_pendientes (todo-o-nada).
 * Si hay errores, devuelve el reporte (fila · usuario · columna · motivo) sin guardar.
 */
@Service
public class CargaExcelService {

    private static final Set<String> TIPOS_VALIDOS =
            Set.of("estudiante", "docente", "acudiente", "coordinador");
    private static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final String DOC_REGEX = "\\d{8,11}";
    private static final String TEL_REGEX = "\\d{7,10}";
    /** Curso escrito como grado (1-2 dígitos) + grupo (una letra), ej. "5B", "10C". */
    private static final java.util.regex.Pattern CURSO_PATTERN =
            java.util.regex.Pattern.compile("\\s*(\\d{1,2})\\s*([A-Za-z])\\s*");

    private final ClassifyDatabaseService databaseService;
    private final ParametrosColegioService parametrosService;

    public CargaExcelService(ClassifyDatabaseService databaseService,
                             ParametrosColegioService parametrosService) {
        this.databaseService = databaseService;
        this.parametrosService = parametrosService;
    }

    public CargaResultado procesar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return new CargaResultado(false, 0, 0, List.of(),
                    "No se recibió ningún archivo. Selecciona un Excel (.xlsx).");
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase();
        if (!nombre.endsWith(".xlsx") && !nombre.endsWith(".xls")) {
            return new CargaResultado(false, 0, 0, List.of(),
                    "El archivo debe ser un Excel (.xlsx o .xls).");
        }

        // Parámetros del colegio: definen los grados, grupos y materias válidos
        ParametrosColegio params = parametrosService.obtener();

        List<ErrorFila> errores = new ArrayList<>();
        List<String[]> filasValidas = new ArrayList<>();
        // Duplicados dentro del propio archivo
        Set<String> correosVistos = new HashSet<>();
        Set<String> documentosVistos = new HashSet<>();
        Set<String> usuariosVistos = new HashSet<>();

        try (InputStream in = archivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(in);
             Connection connection = databaseService.openConnection()) {

            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            String errorEstructura = validarEstructura(header);
            if (errorEstructura != null) {
                return new CargaResultado(false, 0, 0, List.of(), errorEstructura);
            }

            int totalFilas = 0;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (esFilaVacia(row)) continue;

                String[] v = new String[PlantillaExcelService.COLUMNAS.length];
                for (int c = 0; c < v.length; c++) {
                    v[c] = leerCelda(row.getCell(c));
                }

                // Ignora las filas de ejemplo de la plantilla (aunque el usuario no las borre)
                String correoFila = v[PlantillaExcelService.COL_CORREO].toLowerCase();
                if (PlantillaExcelService.CORREOS_EJEMPLO.contains(correoFila)) {
                    continue;
                }
                totalFilas++;

                int numFila = r + 1; // fila real de Excel (1-based)
                String etiqueta = (v[0] + " " + v[1]).trim();
                if (etiqueta.isBlank()) etiqueta = "(sin nombre)";

                validarFila(connection, v, numFila, etiqueta, errores,
                        correosVistos, documentosVistos, usuariosVistos, params);

                if (errores.isEmpty() || erroresDeFila(errores, numFila) == 0) {
                    filasValidas.add(v);
                }
            }

            if (totalFilas == 0) {
                return new CargaResultado(false, 0, 0, List.of(),
                        "El archivo no contiene filas de usuarios para cargar.");
            }
            if (!errores.isEmpty()) {
                return new CargaResultado(false, 0, totalFilas, errores,
                        "Se encontraron " + errores.size() + " error(es). No se guardó ningún registro; corrige y vuelve a cargar.");
            }

            // Todo válido → guardar
            int guardados = guardar(connection, filasValidas);
            return new CargaResultado(true, guardados, totalFilas, List.of(),
                    guardados + " usuario(s) autorizados correctamente.");

        } catch (SQLException e) {
            return new CargaResultado(false, 0, 0, List.of(),
                    "Error al guardar en la base de datos: " + e.getMessage());
        } catch (Exception e) {
            return new CargaResultado(false, 0, 0, List.of(),
                    "No se pudo leer el archivo Excel: " + e.getMessage());
        }
    }

    private String validarEstructura(Row header) {
        if (header == null) return "El archivo está vacío o no tiene fila de encabezados.";
        for (int i = 0; i < PlantillaExcelService.COLUMNAS.length; i++) {
            String esperado = PlantillaExcelService.COLUMNAS[i];
            String actual = leerCelda(header.getCell(i)).toLowerCase();
            if (!esperado.equalsIgnoreCase(actual)) {
                return "La estructura del archivo no coincide con la plantilla. " +
                        "Columna " + (i + 1) + " debe ser \"" + esperado + "\" (se encontró \"" + actual + "\"). " +
                        "Descarga la plantilla oficial.";
            }
        }
        return null;
    }

    private void validarFila(Connection conn, String[] v, int fila, String etiqueta, List<ErrorFila> errores,
                             Set<String> correosVistos, Set<String> documentosVistos, Set<String> usuariosVistos,
                             ParametrosColegio params) {
        String nombre = v[0], apellido = v[1], correo = v[2], documento = v[3],
                telefono = v[4], usuario = v[5], tipo = v[6].toLowerCase();

        if (nombre.isBlank()) errores.add(new ErrorFila(fila, etiqueta, "nombre", nombre, "Campo obligatorio vacío"));
        if (apellido.isBlank()) errores.add(new ErrorFila(fila, etiqueta, "apellido", apellido, "Campo obligatorio vacío"));

        if (correo.isBlank()) {
            errores.add(new ErrorFila(fila, etiqueta, "correo", correo, "Campo obligatorio vacío"));
        } else if (!correo.matches(EMAIL_REGEX)) {
            errores.add(new ErrorFila(fila, etiqueta, "correo", correo, "Formato de correo inválido"));
        } else if (!correosVistos.add(correo.toLowerCase())) {
            errores.add(new ErrorFila(fila, etiqueta, "correo", correo, "Correo duplicado dentro del archivo"));
        } else if (existe(conn, "correo", correo)) {
            errores.add(new ErrorFila(fila, etiqueta, "correo", correo, "Ya existe un usuario con ese correo"));
        }

        if (documento.isBlank()) {
            errores.add(new ErrorFila(fila, etiqueta, "documento", documento, "Campo obligatorio vacío"));
        } else if (!documento.matches(DOC_REGEX)) {
            errores.add(new ErrorFila(fila, etiqueta, "documento", documento, "Debe tener entre 8 y 11 dígitos"));
        } else if (!documentosVistos.add(documento)) {
            errores.add(new ErrorFila(fila, etiqueta, "documento", documento, "Documento duplicado dentro del archivo"));
        } else if (existe(conn, "documento", documento)) {
            errores.add(new ErrorFila(fila, etiqueta, "documento", documento, "Ya existe un usuario con ese documento"));
        }

        if (!telefono.isBlank() && !telefono.matches(TEL_REGEX)) {
            errores.add(new ErrorFila(fila, etiqueta, "telefono", telefono, "Debe tener entre 7 y 10 dígitos"));
        }

        if (usuario.isBlank()) {
            errores.add(new ErrorFila(fila, etiqueta, "nombre_usuario", usuario, "Campo obligatorio vacío"));
        } else if (!usuariosVistos.add(usuario.toLowerCase())) {
            errores.add(new ErrorFila(fila, etiqueta, "nombre_usuario", usuario, "Usuario duplicado dentro del archivo"));
        } else if (existe(conn, "nombre_usuario", usuario)) {
            errores.add(new ErrorFila(fila, etiqueta, "nombre_usuario", usuario, "Ese nombre de usuario ya está en uso"));
        }

        if (tipo.isBlank()) {
            errores.add(new ErrorFila(fila, etiqueta, "tipo_usuario", v[6], "Campo obligatorio vacío"));
        } else if (!TIPOS_VALIDOS.contains(tipo)) {
            errores.add(new ErrorFila(fila, etiqueta, "tipo_usuario", v[6],
                    "Debe ser: estudiante, docente, acudiente o coordinador"));
        }

        // Campos condicionales por rol, validados contra los parámetros del colegio
        String materia = v[PlantillaExcelService.COL_MATERIA];
        String cursos = v[PlantillaExcelService.COL_CURSOS];
        String grado = v[PlantillaExcelService.COL_GRADO];
        String grupo = v[PlantillaExcelService.COL_GRUPO];

        if ("docente".equals(tipo)) {
            // Un docente dicta una o varias materias (separadas por coma) en uno o varios
            // cursos (separados por coma, ej. "5B, 6A"). No lleva grado/grupo propios.
            if (materia.isBlank()) {
                errores.add(new ErrorFila(fila, etiqueta, "materia", materia,
                        "Obligatoria para docente (una o varias separadas por coma)"));
            } else {
                for (String m : materia.split(",")) {
                    String mm = m.trim();
                    if (!mm.isBlank() && !contieneIgnoreCase(params.materias(), mm)) {
                        errores.add(new ErrorFila(fila, etiqueta, "materia", mm,
                                "No es una materia del colegio. Válidas: " + String.join(", ", params.materias())));
                    }
                }
            }
            if (cursos.isBlank()) {
                errores.add(new ErrorFila(fila, etiqueta, "cursos", cursos,
                        "Obligatorio para docente: los cursos donde dicta (ej. 5B, 6A)"));
            } else {
                for (String c : cursos.split(",")) {
                    String cc = c.trim();
                    if (cc.isBlank()) continue;
                    String motivo = validarCurso(cc, params);
                    if (motivo != null) {
                        errores.add(new ErrorFila(fila, etiqueta, "cursos", cc, motivo));
                    }
                }
            }
        }

        if ("estudiante".equals(tipo)) {
            if (grado.isBlank()) {
                errores.add(new ErrorFila(fila, etiqueta, "grado", grado, "Obligatorio para estudiante"));
            } else if (!params.grados().contains(grado.trim())) {
                errores.add(new ErrorFila(fila, etiqueta, "grado", grado,
                        "Grado inválido. El colegio tiene grados 1 a " + params.numGrados()));
            }
            if (grupo.isBlank()) {
                errores.add(new ErrorFila(fila, etiqueta, "grupo", grupo, "Obligatorio para estudiante"));
            } else if (!contieneIgnoreCase(params.grupos(), grupo.trim())) {
                errores.add(new ErrorFila(fila, etiqueta, "grupo", grupo,
                        "Grupo inválido. Válidos: " + String.join(", ", params.grupos())));
            }
        }
    }

    /**
     * Valida un curso escrito como grado+grupo pegados (ej. "5B", "10C").
     * Devuelve el motivo del error, o null si el curso es válido.
     */
    private String validarCurso(String curso, ParametrosColegio params) {
        java.util.regex.Matcher m = CURSO_PATTERN.matcher(curso);
        if (!m.matches()) {
            return "Curso inválido. Escríbelo como grado+grupo, ej. 5B";
        }
        String grado = m.group(1);
        String grupo = m.group(2).toUpperCase();
        if (!params.grados().contains(grado)) {
            return "Grado " + grado + " inválido. El colegio tiene grados 1 a " + params.numGrados();
        }
        if (!contieneIgnoreCase(params.grupos(), grupo)) {
            return "Grupo " + grupo + " inválido. Válidos: " + String.join(", ", params.grupos());
        }
        return null;
    }

    private boolean contieneIgnoreCase(List<String> lista, String valor) {
        return lista.stream().anyMatch(x -> x.equalsIgnoreCase(valor));
    }

    private int erroresDeFila(List<ErrorFila> errores, int fila) {
        int n = 0;
        for (ErrorFila e : errores) if (e.fila() == fila) n++;
        return n;
    }

    /** ¿Existe el valor en registro_usuarios o en usuarios_pendientes? */
    private boolean existe(Connection conn, String columna, String valor) {
        String sql = "SELECT 1 FROM registro_usuarios WHERE LOWER(" + columna + ") = LOWER(?) " +
                "UNION SELECT 1 FROM usuarios_pendientes WHERE LOWER(" + columna + ") = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setString(2, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private int guardar(Connection conn, List<String[]> filas) throws SQLException {
        String sql = "INSERT INTO usuarios_pendientes " +
                "(nombre, apellido, correo, documento, telefono, nombre_usuario, tipo_usuario, materia, grado, grupo, curso, estado, origen) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'autorizado', 'excel')";
        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);
        int guardados = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] v : filas) {
                String tipo = v[PlantillaExcelService.COL_TIPO].toLowerCase();
                String materia = null, grado = null, grupo = null, curso = null;

                if ("docente".equals(tipo)) {
                    // El docente guarda sus materias y sus cursos (lista); no tiene grado/grupo propios.
                    materia = normalizarLista(v[PlantillaExcelService.COL_MATERIA]);
                    curso = normalizarLista(v[PlantillaExcelService.COL_CURSOS]);
                } else if ("estudiante".equals(tipo)) {
                    grado = blankToNull(v[PlantillaExcelService.COL_GRADO]);
                    String grupoRaw = blankToNull(v[PlantillaExcelService.COL_GRUPO]);
                    grupo = grupoRaw == null ? null : grupoRaw.toUpperCase();
                    if (grado != null && grupo != null) {
                        curso = (grado + grupo).trim(); // curso = grado+grupo, ej. 5B
                    }
                }

                ps.setString(1, v[0]);
                ps.setString(2, v[1]);
                ps.setString(3, v[2]);
                ps.setString(4, v[3]);
                ps.setString(5, blankToNull(v[4]));
                ps.setString(6, v[5]);
                ps.setString(7, tipo);
                ps.setString(8, materia);   // materia (docente)
                ps.setString(9, grado);     // grado (estudiante)
                ps.setString(10, grupo);    // grupo (estudiante)
                ps.setString(11, curso);    // curso: cursos del docente o grado+grupo del estudiante
                ps.addBatch();
                guardados++;
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
        return guardados;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /** Limpia una lista separada por comas ("Sociales , Religion") → "Sociales, Religion". Null si queda vacía. */
    private static String normalizarLista(String s) {
        if (s == null || s.isBlank()) return null;
        String limpia = Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .collect(java.util.stream.Collectors.joining(", "));
        return limpia.isBlank() ? null : limpia;
    }

    private boolean esFilaVacia(Row row) {
        if (row == null) return true;
        for (int c = 0; c < PlantillaExcelService.COLUMNAS.length; c++) {
            if (!leerCelda(row.getCell(c)).isBlank()) return false;
        }
        return true;
    }

    /** Lee cualquier celda como texto, evitando notación científica en números. */
    private String leerCelda(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    yield new BigDecimal(cell.getNumericCellValue()).toBigInteger().toString();
                }
                yield BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }
}
