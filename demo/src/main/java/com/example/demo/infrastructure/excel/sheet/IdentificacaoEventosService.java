package com.example.demo.infrastructure.excel.sheet;

import com.example.demo.contracts.SheetNames;
import com.example.demo.infrastructure.excel.shared.SheetServiceUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class IdentificacaoEventosService {

    private static final int EXTRA_EDITABLE_ROWS = 10;
    private static final String META_ROW_TYPE_KEY = "__meta_row_type";
    private static final String META_ROW_TYPE_OPTIONS = "etapa2_options";
    private static final String META_CATEGORIA_OPTIONS_KEY = "__meta_categoria_options";
    private static final List<String> FIXED_HEADERS = Arrays.asList(
            "Processo",
            "Fase",
            "Evento de Risco (indicar)",
            "Tipo de Risco",
            "Categoria",
            "Tipo de Risco de Integridade",
            "Causas (descrever)",
            "Consequências (descrever)"
    );

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {
        PreparedRows preparedRows = prepareRows(rows);
        List<Map<String, Object>> dataRows = preparedRows.dataRows;
        List<String> categoriaOptions = preparedRows.categoriaOptions;

        XSSFSheet sheet = wb.createSheet(sheetName);
        String etapa1SheetName = SheetServiceUtils.resolveSheetName(
                wb,
                SheetNames.ETAPA_1.marker(),
                SheetNames.ETAPA_1.displayName()
        );
        String processoReferenceFormula = buildProcessoReferenceFormula(etapa1SheetName);
        int firstEditableRow = 2;
        int lastEditableRow = SheetServiceUtils.computeLastEditableRow(firstEditableRow, dataRows.size(), EXTRA_EDITABLE_ROWS);

        // COR AZUL PERSONALIZADA
        byte[] rgbBlue = new byte[]{(byte) 180, (byte) 198, (byte) 231};
        XSSFColor npiBlue = new XSSFColor(rgbBlue, null);

        int r = 0;

        // 1. Estilo para o Cabeçalho Azul
        XSSFCellStyle blueStyle = wb.createCellStyle();
        blueStyle.setFillForegroundColor(npiBlue);
        blueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        blueStyle.setAlignment(HorizontalAlignment.CENTER);
        blueStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        SheetServiceUtils.applyBorders(blueStyle);
        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        blueStyle.setFont(boldFont);
        blueStyle.setWrapText(true);

        // 2. Criar Linha 1: Título Mesclado
        Row titleRow = sheet.createRow(r++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Identificação e Categorização de Riscos");
        titleCell.setCellStyle(blueStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        for (int i = 1; i <= 7; i++) {
            titleRow.createCell(i).setCellStyle(blueStyle);
        }

        // 3. Gerar Nomes das Colunas
        List<String> headerList = FIXED_HEADERS;

        Row headerRow = sheet.createRow(r++);
        headerRow.setHeightInPoints(35);
        for (int c = 0; c < headerList.size(); c++) {
            Cell cell = headerRow.createCell(c);
            String headerName = headerList.get(c);

            if (headerName.equalsIgnoreCase("Evento de Risco (indicar)")) {
                cell.setCellValue("Evento de Risco\n(indicar)");
            } else if (headerName.equalsIgnoreCase("Causas (descrever)")) {
                cell.setCellValue("Causas\n(descrever)");
            } else if (headerName.equalsIgnoreCase("Consequências (descrever)")) {
                cell.setCellValue("Consequências\n(descrever)");
            } else {
                cell.setCellValue(headerName);
            }

            cell.setCellStyle(blueStyle);
        }

        // 4. DEFINIÇÃO DOS ESTILOS DE DADOS
        CellStyle dataStyleCenter = wb.createCellStyle();
        SheetServiceUtils.applyBorders(dataStyleCenter);
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyleCenter.setWrapText(false);

        CellStyle dataStyleLeft = wb.createCellStyle();
        SheetServiceUtils.applyBorders(dataStyleLeft);
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
        dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyleLeft.setWrapText(false);

        // 5. Preenchimento de Dados com Lógica de Alinhamento
        for (Map<String, Object> rowData : dataRows) {
            Row dataRow = sheet.createRow(r++);
            for (int c = 0; c < headerList.size(); c++) {
                String headerName = headerList.get(c);
                Cell cell = dataRow.createCell(c);
                boolean isProcessoColumn = headerName.equalsIgnoreCase("Processo");

                if (isProcessoColumn) {
                    cell.setCellFormula(processoReferenceFormula);
                } else {
                    Object val = rowData.get(headerName);
                    String text = val == null ? "" : String.valueOf(val);
                    cell.setCellValue(normalizeSelectValue(headerName, text));
                }

                if (isProcessoColumn ||
                        headerName.contains("Evento") ||
                        headerName.contains("Causas") ||
                        headerName.contains("Consequências")) {
                    cell.setCellStyle(dataStyleLeft);
                } else {
                    cell.setCellStyle(dataStyleCenter);
                }
            }
        }

        for (int i = 0; i < EXTRA_EDITABLE_ROWS; i++) {
            Row extraRow = sheet.createRow(r++);
            for (int c = 0; c < headerList.size(); c++) {
                String headerName = headerList.get(c);
                Cell cell = extraRow.createCell(c);

                if (headerName.equalsIgnoreCase("Processo")) {
                    cell.setCellFormula(processoReferenceFormula);
                } else {
                    cell.setCellValue("");
                }

                if (headerName.equalsIgnoreCase("Processo")
                        || headerName.contains("Evento")
                        || headerName.contains("Causas")
                        || headerName.contains("Consequências")) {
                    cell.setCellStyle(dataStyleLeft);
                } else {
                    cell.setCellStyle(dataStyleCenter);
                }
            }
        }

        setupValidations(sheet, headerList, firstEditableRow, lastEditableRow, categoriaOptions);
        setColumnWidths(sheet, headerList);
    }

    private void setupValidations(
            XSSFSheet sheet,
            List<String> headers,
            int firstEditableRow,
            int lastEditableRow,
            List<String> categoriaOptions
    ) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        int colTipo = headers.indexOf("Tipo de Risco");
        int colCat = headers.indexOf("Categoria");
        int colIntegridade = headers.indexOf("Tipo de Risco de Integridade");

        if (colTipo != -1) {
            SheetServiceUtils.applySelect(
                    sheet,
                    helper,
                    new String[]{"Ameaça", "Oportunidade"},
                    colTipo,
                    firstEditableRow,
                    lastEditableRow
            );
        }
        if (colCat != -1 && categoriaOptions != null && !categoriaOptions.isEmpty()) {
            SheetServiceUtils.applySelect(
                    sheet,
                    helper,
                    categoriaOptions.toArray(new String[0]),
                    colCat,
                    firstEditableRow,
                    lastEditableRow
            );
        }
        if (colIntegridade != -1) {
            SheetServiceUtils.applySelect(
                    sheet,
                    helper,
                    new String[]{"Corrupção", "Fraude", "Desvio de conduta"},
                    colIntegridade,
                    firstEditableRow,
                    lastEditableRow
            );
        }
    }

    private PreparedRows prepareRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> dataRows = new ArrayList<>();
        LinkedHashSet<String> categoriaOptions = new LinkedHashSet<>();
        if (rows == null || rows.isEmpty()) {
            return new PreparedRows(dataRows, new ArrayList<>(categoriaOptions));
        }

        for (Map<String, Object> row : rows) {
            if (isMetadataRow(row)) {
                collectCategoriaOptionsFromMetadata(row, categoriaOptions);
                continue;
            }

            if (row == null) {
                continue;
            }

            dataRows.add(row);
            collectCategoriaOptionsFromData(row, categoriaOptions);
        }

        return new PreparedRows(dataRows, new ArrayList<>(categoriaOptions));
    }

    private boolean isMetadataRow(Map<String, Object> row) {
        if (row == null) {
            return false;
        }
        Object rowType = row.get(META_ROW_TYPE_KEY);
        return META_ROW_TYPE_OPTIONS.equals(rowType);
    }

    private void collectCategoriaOptionsFromMetadata(Map<String, Object> row, LinkedHashSet<String> categoriaOptions) {
        Object value = row.get(META_CATEGORIA_OPTIONS_KEY);
        if (!(value instanceof List<?> list)) {
            return;
        }

        for (Object item : list) {
            if (item == null) {
                continue;
            }
            String option = String.valueOf(item).trim();
            if (!option.isBlank()) {
                categoriaOptions.add(option);
            }
        }
    }

    private void collectCategoriaOptionsFromData(Map<String, Object> row, LinkedHashSet<String> categoriaOptions) {
        Object categoria = row.get("Categoria");
        if (categoria == null) {
            return;
        }

        String option = String.valueOf(categoria).trim();
        if (!option.isBlank()) {
            categoriaOptions.add(option);
        }
    }

    private static final class PreparedRows {
        private final List<Map<String, Object>> dataRows;
        private final List<String> categoriaOptions;

        private PreparedRows(List<Map<String, Object>> dataRows, List<String> categoriaOptions) {
            this.dataRows = dataRows;
            this.categoriaOptions = categoriaOptions;
        }
    }

    private void setColumnWidths(XSSFSheet sheet, List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h.contains("Evento") || h.contains("Causas")) {
                sheet.setColumnWidth(i, 15000);
            } else if (h.equalsIgnoreCase("Processo")) {
                sheet.setColumnWidth(i, 12000);
            } else if (h.contains("Consequências")) {
                sheet.setColumnWidth(i, 40000);
            } else {
                sheet.setColumnWidth(i, 7000);
            }
        }
    }

    private String normalizeSelectValue(String headerName, String value) {
        if (value == null) {
            return "";
        }

        if (!"Tipo de Risco".equalsIgnoreCase(headerName)) {
            return value;
        }

        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("Ameaca") || trimmed.equalsIgnoreCase("Ameaça")) {
            return "Ameaça";
        }
        if (trimmed.equalsIgnoreCase("Oportunidade")) {
            return "Oportunidade";
        }
        return value;
    }

    private String buildProcessoReferenceFormula(String etapa1SheetName) {
        return "'" + etapa1SheetName.replace("'", "''") + "'!A$5";
    }

}
