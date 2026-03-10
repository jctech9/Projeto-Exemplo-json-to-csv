package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ExcelService {
    private final IdentificacaoEventosService identificacaoEventosService;

    public  ExcelService(IdentificacaoEventosService identificacaoEventosService) {
        this.identificacaoEventosService = identificacaoEventosService;
    }

    public byte[] generateXlsx(Map<String, List<Map<String, Object>>> etapas) throws IOException {

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] rgbPink = new byte[]{(byte) 230, (byte) 145, (byte) 145};
            XSSFColor npiPink = new XSSFColor(rgbPink, null);

            // Estilos de Classificação
            CellStyle styleExtremo = createRGBStyle(wb, new byte[]{(byte) 255, 0, 0});
            CellStyle styleAlto = createRGBStyle(wb, new byte[]{(byte) 255, (byte) 192, 0});
            CellStyle styleMedio = createRGBStyle(wb, new byte[]{(byte) 255, (byte) 255, 0});
            CellStyle styleBaixo = createRGBStyle(wb, new byte[]{(byte) 146, (byte) 208, 80});

            for (Map.Entry<String, List<Map<String, Object>>> entry : etapas.entrySet()) {

                String sheetName = entry.getKey();
                List<Map<String, Object>> rows = entry.getValue();

                if(sheetName.contains("ETAPA 2")) {
                    identificacaoEventosService.generateSheet(wb, sheetName, rows);
                    continue;
                }

                XSSFSheet sheet = wb.createSheet(sheetName);


                LinkedHashSet<String> headers = new LinkedHashSet<>();
                for (Map<String, Object> row : rows) {
                    headers.addAll(row.keySet());
                }

                List<String> headerList = new ArrayList<>(headers);
                int r = 0;

                // Cabeçalho especial ETAPA 3
                if (sheetName.contains("ETAPA 3")) {

                    Row groupRow = sheet.createRow(r++);

                    createGroupHeader(wb, groupRow, 0, 5,
                            "Avaliação dos Riscos", npiPink, sheet);

                    createGroupHeader(wb, groupRow, 6, 10,
                            "Avaliação dos Controles", npiPink, sheet);

                    createGroupHeader(wb, groupRow, 11, 12,
                            "Risco Residual", npiPink, sheet);
                }

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

                            cell.setCellFormula("B" + rowNum + "*D" + rowNum);

                        } else if (columnName.equalsIgnoreCase("Risco Residual")) {

                            cell.setCellFormula("E" + rowNum + "*J" + rowNum);

                        } else if (columnName.equalsIgnoreCase("FAC")) {

                            cell.setCellFormula(
                                    "IF(I" + rowNum + "=\"Forte\",0.2," +
                                            "IF(I" + rowNum + "=\"Satisfatório\",0.4," +
                                            "IF(I" + rowNum + "=\"Mediano\",0.6," +
                                            "IF(I" + rowNum + "=\"Fraco\",0.8,1))))"
                            );

                        } else if (columnName.equalsIgnoreCase("Classificação do Risco Residual")) {

                            cell.setCellFormula(
                                    "IF(K" + rowNum + "=0,\"\",IF(K" + rowNum +
                                            "<10,\"Risco Baixo\",IF(K" + rowNum +
                                            "<40,\"Risco Médio\",IF(K" + rowNum +
                                            "<80,\"Risco Alto\",\"Risco Extremo\"))))"
                            );

                        } else if (columnName.equalsIgnoreCase("Classificação do Risco Inerente")) {

                            cell.setCellFormula(
                                    "IF(E" + rowNum + "=0,\"\",IF(E" + rowNum +
                                            "<10,\"Risco Baixo\",IF(E" + rowNum +
                                            "<40,\"Risco Médio\",IF(E" + rowNum +
                                            "<80,\"Risco Alto\",\"Risco Extremo\"))))"
                            );

                        } else {

                            try {
                                double num = Double.parseDouble(strVal.replace(",", "."));
                                cell.setCellValue(num);
                            } catch (Exception e) {
                                cell.setCellValue(strVal);
                            }
                        }

                        // Aplicar cores classificação inerente
                        if (columnName.equalsIgnoreCase("Classificação do Risco Inerente")) {

                            double risco = 0;

                            try {
                                risco = Double.parseDouble(
                                        rowData.get("Risco Inerente (PxI)").toString());
                            } catch (Exception ignored) {}

                            if (risco >= 80) {
                                cell.setCellStyle(styleExtremo);
                            } else if (risco >= 40) {
                                cell.setCellStyle(styleAlto);
                            } else if (risco >= 10) {
                                cell.setCellStyle(styleMedio);
                            } else {
                                cell.setCellStyle(styleBaixo);
                            }
                        }

                        // Aplicar cores classificação residual
                        if (columnName.equalsIgnoreCase("Classificação do Risco Residual")) {

                            double riscoResidual = 0;

                            try {

                                double riscoInerente =
                                        Double.parseDouble(
                                                rowData.get("Risco Inerente (PxI)").toString());

                                double fac = 1;

                                String avaliacao =
                                        String.valueOf(rowData.get("Avaliação dos Controles"));

                                if (avaliacao.equalsIgnoreCase("Forte")) fac = 0.2;
                                else if (avaliacao.equalsIgnoreCase("Satisfatório")) fac = 0.4;
                                else if (avaliacao.equalsIgnoreCase("Mediano")) fac = 0.6;
                                else if (avaliacao.equalsIgnoreCase("Fraco")) fac = 0.8;

                                riscoResidual = riscoInerente * fac;

                            } catch (Exception ignored) {}

                            if (riscoResidual >= 80) {
                                cell.setCellStyle(styleExtremo);
                            } else if (riscoResidual >= 40) {
                                cell.setCellStyle(styleAlto);
                            } else if (riscoResidual >= 10) {
                                cell.setCellStyle(styleMedio);
                            } else {
                                cell.setCellStyle(styleBaixo);
                            }
                        }
                    }
                }

                DataValidationHelper validationHelper = sheet.getDataValidationHelper();

                int colImpacto = headerList.indexOf("Impacto");

                if (colImpacto != -1) {

                    String[] opcoesProb =
                            {"Muito baixo", "Baixo", "Médio", "Alto", "Muito alto"};

                    DataValidationConstraint constraint =
                            validationHelper.createExplicitListConstraint(opcoesProb);

                    CellRangeAddressList addressList =
                            new CellRangeAddressList(2, 1000, colImpacto, colImpacto);

                    DataValidation validation =
                            validationHelper.createValidation(constraint, addressList);

                    validation.setSuppressDropDownArrow(true);
                    validation.setShowErrorBox(true);

                    sheet.addValidationData(validation);
                }

                int colAvaliacao = headerList.indexOf("Avaliação dos Controles");

                if (colAvaliacao != -1) {

                    String[] opcoes =
                            {"Inexistente", "Fraco", "Mediano", "Satisfatório", "Forte"};

                    DataValidationConstraint constraint =
                            validationHelper.createExplicitListConstraint(opcoes);

                    CellRangeAddressList addressList =
                            new CellRangeAddressList(2, 1000, colAvaliacao, colAvaliacao);

                    DataValidation validation =
                            validationHelper.createValidation(constraint, addressList);

                    validation.setSuppressDropDownArrow(true);
                    validation.setShowErrorBox(true);

                    sheet.addValidationData(validation);
                }

                for (int c = 0; c < headerList.size(); c++) {

                    String header = headerList.get(c);

                    if (header.equals("P") || header.equals("I") || header.equals("FAC")) {
                        sheet.setColumnWidth(c, 1500);
                    }
                    else if (header.contains("Classificação") || header.contains("Avaliação") || header.contains("Data Última Avaliação")) {
                        sheet.setColumnWidth(c, 8000);
                    }
                    else if (header.contains("Evento de Risco")) {
                        sheet.setColumnWidth(c, 30000);
                    }
                    else if (header.contains("Controles Preventivos") || header.contains("Controles de Atenuação e recuperação")) {
                        sheet.setColumnWidth(c, 20000);
                    }
                    else {
                        sheet.setColumnWidth(c, 4500);
                    }
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createRGBStyle(XSSFWorkbook wb, byte[] rgb) {

        XSSFCellStyle style = wb.createCellStyle();
        XSSFColor color = new XSSFColor(rgb, null);

        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private void createGroupHeader(
            XSSFWorkbook wb,
            Row row,
            int start,
            int end,
            String text,
            XSSFColor color,
            XSSFSheet sheet) {

        XSSFCellStyle style = wb.createCellStyle();

        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);

        Font font = wb.createFont();
        font.setBold(true);

        style.setFont(font);

        for (int i = start; i <= end; i++) {

            Cell cell = row.createCell(i);

            if (i == start) {
                cell.setCellValue(text);
            }

            cell.setCellStyle(style);
        }

        sheet.addMergedRegion(
                new org.apache.poi.ss.util.CellRangeAddress(
                        row.getRowNum(),
                        row.getRowNum(),
                        start,
                        end
                )
        );
    }
}