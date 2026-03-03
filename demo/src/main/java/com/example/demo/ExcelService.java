package com.example.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ExcelService {

    public byte[] generateXlsx(Map<String, List<Map<String, Object>>> etapas) throws IOException {

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Estilos de Classificação
            CellStyle styleExtremo = createColorStyle(wb, IndexedColors.RED.getIndex());
            CellStyle styleAlto = createColorStyle(wb, IndexedColors.ORANGE.getIndex());
            CellStyle styleMedio = createColorStyle(wb, IndexedColors.YELLOW.getIndex());
            CellStyle styleBaixo = createColorStyle(wb, IndexedColors.LIGHT_GREEN.getIndex());

            for (Map.Entry<String, List<Map<String, Object>>> entry : etapas.entrySet()) {
                String sheetName = entry.getKey();
                XSSFSheet sheet = wb.createSheet(sheetName);
                List<Map<String, Object>> rows = entry.getValue();

                LinkedHashSet<String> headers = new LinkedHashSet<>();
                for (Map<String, Object> row : rows) { headers.addAll(row.keySet()); }
                List<String> headerList = new ArrayList<>(headers);

                int r = 0;

                // LÓGICA ESPECIAL PARA AVALIAÇÃO DE RISCOS (ETAPA 3) estilização

                if (sheetName.contains("ETAPA 3")) {
                    Row groupRow = sheet.createRow(r++);
                    createGroupHeader(wb, groupRow, 0, 5, "Avaliação dos Riscos", IndexedColors.ROSE.getIndex(), sheet);
                    createGroupHeader(wb, groupRow, 6, 10, "Avaliação dos Controles", IndexedColors.ROSE.getIndex(), sheet);
                    createGroupHeader(wb, groupRow, 11, 14, "Risco Residual", IndexedColors.ROSE.getIndex(), sheet);
                }

                CellStyle headerStyle = wb.createCellStyle();
                Font headerFont = wb.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);


                headerStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                Row headerRow = sheet.createRow(r++);
                for (int c = 0; c < headerList.size(); c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(headerList.get(c));
                    cell.setCellStyle(headerStyle);
                }

                for (Map<String, Object> rowData : rows) {
                    Row dataRow = sheet.createRow(r++);
                    for (int c = 0; c < headerList.size(); c++) {
                        String columnName = headerList.get(c);
                        Cell cell = dataRow.createCell(c);
                        Object val = rowData.get(columnName);
                        String strVal = val == null ? "" : String.valueOf(val);

                        cell.setCellValue(strVal);

                        // APLICA CORES CONFORME A CLASSIFICAÇÃO
                        if (columnName.contains("Classificação")) {
                            if (strVal.equalsIgnoreCase("EXTREMO")) cell.setCellStyle(styleExtremo);
                            else if (strVal.equalsIgnoreCase("ALTO")) cell.setCellStyle(styleAlto);
                            else if (strVal.equalsIgnoreCase("MÉDIO")) cell.setCellStyle(styleMedio);
                            else if (strVal.equalsIgnoreCase("BAIXO")) cell.setCellStyle(styleBaixo);
                        }

                        // Centraliza colunas curtas (P, I, FAC, etc)
                        if (columnName.length() <= 3) {
                            CellStyle center = wb.createCellStyle();
                            center.setAlignment(HorizontalAlignment.CENTER);
                            cell.setCellStyle(center);
                        }
                    }
                }

                //select
                int colAvaliacao = headerList.indexOf("Avaliação dos Controles");

                if (colAvaliacao != -1) {
                    DataValidationHelper validationHelper = sheet.getDataValidationHelper();

                    String[] opcoes = {"Inexistente", "Fraco", "Mediano", "Satisfatório", "Forte"};

                    DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(opcoes);


                    CellRangeAddressList addressList = new CellRangeAddressList(2, 1000, colAvaliacao, colAvaliacao);

                    DataValidation validation = validationHelper.createValidation(constraint, addressList);

                    validation.setSuppressDropDownArrow(true);
                    validation.setShowErrorBox(true);

                    sheet.addValidationData(validation);
                }

                for (int c = 0; c < headerList.size(); c++) { sheet.autoSizeColumn(c); }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // Método auxiliar para criar os grupos superiores mesclados
    private void createGroupHeader(XSSFWorkbook wb, Row row, int start, int end, String text, short color, XSSFSheet sheet) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);

        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);

        for (int i = start; i <= end; i++) {
            Cell cell = row.createCell(i);
            if (i == start) cell.setCellValue(text);
            cell.setCellStyle(style);
        }
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(row.getRowNum(), row.getRowNum(), start, end));
    }

    // Método auxiliar para os estilos de risco
    private CellStyle createColorStyle(XSSFWorkbook wb, short color) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }
}



