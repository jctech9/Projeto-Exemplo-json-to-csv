package com.example.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
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

            byte[] rgbPink = new byte[]{(byte) 230, (byte) 145, (byte) 145};
            XSSFColor npiPink = new XSSFColor(rgbPink, null);

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
                    createGroupHeader(wb, groupRow, 0, 5, "Avaliação dos Riscos", npiPink, sheet);
                    createGroupHeader(wb, groupRow, 6, 10, "Avaliação dos Controles", npiPink, sheet);
                    createGroupHeader(wb, groupRow, 11, 12, "Risco Residual", npiPink, sheet);
                }

                // Cria estilo rosa personalizado
                CellStyle headerStyle;

                if (sheetName.contains("ETAPA 3")) {

                    XSSFCellStyle pinkStyle = wb.createCellStyle();
                    pinkStyle.setFillForegroundColor(npiPink);
                    pinkStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    pinkStyle.setAlignment(HorizontalAlignment.CENTER);
                    pinkStyle.setBorderBottom(BorderStyle.THIN);
                    pinkStyle.setBorderTop(BorderStyle.THIN);
                    pinkStyle.setBorderLeft(BorderStyle.THIN);
                    pinkStyle.setBorderRight(BorderStyle.THIN);

                    Font headerFont = wb.createFont();
                    headerFont.setBold(true);
                    pinkStyle.setFont(headerFont);

                    headerStyle = pinkStyle;

                } else {

                    CellStyle defaultStyle = wb.createCellStyle();
                    Font headerFont = wb.createFont();
                    headerFont.setBold(true);
                    defaultStyle.setFont(headerFont);
                    defaultStyle.setAlignment(HorizontalAlignment.CENTER);
                    defaultStyle.setBorderBottom(BorderStyle.THIN);

                    headerStyle = defaultStyle;
                }

                Row headerRow = sheet.createRow(r++);


                for (int c = 0; c < headerList.size(); c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(headerList.get(c));
                    cell.setCellStyle(headerStyle);
                }

                for (Map<String, Object> rowData : rows) {
                    Row dataRow = sheet.createRow(r++);
                    int rowNum = dataRow.getRowNum() + 1;
                    for (int c = 0; c < headerList.size(); c++) {
                        String columnName = headerList.get(c);
                        Cell cell = dataRow.createCell(c);
                        Object val = rowData.get(columnName);
                        String strVal = val == null ? "" : String.valueOf(val);

                        if (columnName.equalsIgnoreCase("Risco Inerente (PxI)")) {
                            // Coluna C (P) * Coluna E (I) -> Índices 2 e 4
                            cell.setCellFormula("B" + rowNum + "*D" + rowNum);
                        }
                        else if (columnName.equalsIgnoreCase("Risco Residual")) {
                            // Coluna F (Risco Inerente) * Coluna K (FAC)
                            cell.setCellFormula("E" + rowNum + "*J" + rowNum);
                        }
                        else if (columnName.equalsIgnoreCase("FAC")) {
                            // Mapeia o select da coluna J para o valor numérico do FAC
                            cell.setCellFormula("IF(I"+rowNum+"=\"Forte\",0.2,IF(I"+rowNum+"=\"Satisfatório\",0.4,IF(I"+rowNum+"=\"Mediano\",0.6,IF(I"+rowNum+"=\"Fraco\",0.8,1))))");
                        }
                        else if (columnName.equalsIgnoreCase("Classificação do Risco Residual")) {
                            // Usamos a coluna K (Risco Residual) como base para o cálculo
                            cell.setCellFormula("IF(K" + rowNum + "=0,\"\",IF(K" + rowNum + "<10,\"Risco Baixo\",IF(K" + rowNum + "<40,\"Risco Médio\",IF(K" + rowNum + "<80,\"Risco Alto\",\"Risco Extremo\"))))");
                        }
                        else if (columnName.equalsIgnoreCase("Classificação do Risco Inerente")) {
                            // A coluna F é o resultado de P * I
                            cell.setCellFormula("IF(E" + rowNum + "=0,\"\",IF(E" + rowNum + "<10,\"Risco Baixo\",IF(E" + rowNum + "<40,\"Risco Médio\",IF(E" + rowNum + "<80,\"Risco Alto\",\"Risco Extremo\"))))");
                        }
                        else {
                            try{
                                double num = Double.parseDouble(strVal.replace(",", "."));
                                cell.setCellValue(num);
                            }catch (Exception e) {
                                cell.setCellValue(strVal);
                            }

                        }


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
                DataValidationHelper validationHelper = sheet.getDataValidationHelper();
//select impacto
                int colImpacto = headerList.indexOf("Impacto");
                if (colImpacto != -1) {
                    String[] opcoesProb = {"Muito baixo", "Baixo", "Médio", "Alto", "Muito alto"}; //

                    DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(opcoesProb);
                    CellRangeAddressList addressList = new CellRangeAddressList(2, 1000, colImpacto, colImpacto); // Inicia na linha 2

                    DataValidation validation = validationHelper.createValidation(constraint, addressList);

                    validation.setSuppressDropDownArrow(true);
                    validation.setShowErrorBox(true);
                    sheet.addValidationData(validation);
                }



                //select avaliação dos controles
                int colAvaliacao = headerList.indexOf("Avaliação dos Controles");

                if (colAvaliacao != -1) {
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
    private void createGroupHeader(XSSFWorkbook wb, Row row, int start, int end, String text, XSSFColor color, XSSFSheet sheet) {
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
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



