package com.example.demo.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FontFormatting;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

@Service
public class AvaliacaoRiscosService {

    private static final String ETAPA_2_SHEET_FALLBACK_NAME = "ETAPA 2. IDENTIF. DE EVENTOS";
    private static final int EXTRA_EDITABLE_ROWS = 10;
    private static final List<String> FIXED_HEADERS = Arrays.asList(
            "Evento de Risco",
            "Probabilidade",
            "P",
            "Impacto",
            "I",
            "Risco Inerente (PxI)",
            "Classificação do Risco Inerente",
            "Controles Preventivos (descrever)",
            "Controles de Atenuação e recuperação (descrever)",
            "Avaliação dos Controles",
            "FAC",
            "Risco Residual",
            "Classificação do Risco Residual",
            "Data da Última Avaliação"
    );

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {

        XSSFSheet sheet = wb.createSheet(sheetName);
        String etapa2SheetName = resolveEtapa2SheetName(wb);

        byte[] rgbPink = new byte[]{(byte) 230, (byte) 145, (byte) 145};
        XSSFColor headerColor = new XSSFColor(rgbPink, null);

        List<String> headerList = FIXED_HEADERS;
        int r = 0;

        Row groupRow = sheet.createRow(r++);
        createGroupHeader(wb, groupRow, 0, 6, "Avaliação dos Riscos", headerColor, sheet);
        createGroupHeader(wb, groupRow, 7, 9, "Avaliação dos Controles", headerColor, sheet);
        createGroupHeader(wb, groupRow, 10, 13, "Risco Residual", headerColor, sheet);

        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(headerColor);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(headerStyle);
        headerStyle.setWrapText(true);

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(r++);
        headerRow.setHeightInPoints(35);
        for (int c = 0; c < headerList.size(); c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headerList.get(c));
            cell.setCellStyle(headerStyle);

            String hName = headerList.get(c);

            if (hName.equalsIgnoreCase("Risco Inerente (PxI)")) {
                cell.setCellValue("Risco Inerente\n(PxI)");
            } else if (hName.contains("Classificação do Risco Inerente")) {
                cell.setCellValue("Classificação do\nRisco Inerente");
            } else if (hName.contains("Controles Preventivos (descrever)")) {
                cell.setCellValue("Controles Preventivos\n(descrever)");
                
            } else if (hName.contains("Controles de Atenuação e recuperação (descrever)")) {
                cell.setCellValue("Controles de atenuação e recuperação\n(descrever)");
            } else if (hName.contains("Avaliação dos Controles")) {
                cell.setCellValue("Avaliação dos\nControles");
            } else if (hName.contains("Classificação do Risco Residual")) {
                cell.setCellValue("Classificação do\nRisco Residual");
            } else if (hName.contains("Data da Última Avaliação")) {
                cell.setCellValue("Data da Última\nAvaliação");
            } else {
                cell.setCellValue(hName);
            }
            cell.setCellStyle(headerStyle);
        }

        CellStyle defaultDataStyle = wb.createCellStyle();
        applyBorders(defaultDataStyle);
        defaultDataStyle.setAlignment(HorizontalAlignment.LEFT);
        defaultDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        for (Map<String, Object> rowData : rows) {

            Row dataRow = sheet.createRow(r++);
            int rowNum = dataRow.getRowNum() + 1;

            for (int c = 0; c < headerList.size(); c++) {

                String columnName = headerList.get(c);
                Cell cell = dataRow.createCell(c);
                Object val = rowData.get(columnName);
                String strVal = val == null ? "" : String.valueOf(val);

                if (columnName.equalsIgnoreCase("Risco Inerente (PxI)")) {
                    cell.setCellFormula("IF(OR(C" + rowNum + "=\"\",E" + rowNum + "=\"\"),\"\",C" + rowNum + "*E" + rowNum + ")");
                } else if (columnName.equalsIgnoreCase("Evento de Risco")) {
                    cell.setCellFormula("IF('" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + ")");
                } else if (columnName.equalsIgnoreCase("Risco Residual")) {
                    cell.setCellFormula("IF(OR(F" + rowNum + "=\"\",K" + rowNum + "=\"\"),\"\",F" + rowNum + "*K" + rowNum + ")");
                } else if (columnName.equalsIgnoreCase("P")) {
                    cell.setCellFormula(
                        "IF(B" + rowNum + "=\"\",\"\",IF(B" + rowNum + "=\"Muito alta\",10," +
                                    "IF(B" + rowNum + "=\"Alta\",8," +
                                    "IF(B" + rowNum + "=\"Média\",5," +
                                    "IF(B" + rowNum + "=\"Baixa\",2," +
                            "IF(B" + rowNum + "=\"Muito baixa\",1,0))))))"
                    );
                } else if (columnName.equalsIgnoreCase("I")) {
                    cell.setCellFormula(
                        "IF(D" + rowNum + "=\"\",\"\",IF(D" + rowNum + "=\"Muito alto\",10," +
                                    "IF(D" + rowNum + "=\"Alto\",8," +
                                    "IF(D" + rowNum + "=\"Médio\",5," +
                                    "IF(D" + rowNum + "=\"Baixo\",2," +
                            "IF(D" + rowNum + "=\"Muito baixo\",1,0))))))"
                    );
                } else if (columnName.equalsIgnoreCase("FAC")) {
                    cell.setCellFormula(
                        "IF(J" + rowNum + "=\"\",\"\",IF(J" + rowNum + "=\"Forte\",0.2," +
                                    "IF(J" + rowNum + "=\"Satisfatório\",0.4," +
                                    "IF(J" + rowNum + "=\"Mediano\",0.6," +
                            "IF(J" + rowNum + "=\"Fraco\",0.8,1)))))"
                    );
                } else if (columnName.equalsIgnoreCase("Classificação do Risco Inerente")) {
                    cell.setCellFormula(
                            "IF(F" + rowNum + "=0,\"\",IF(F" + rowNum +
                                    "<10,\"Risco Baixo\",IF(F" + rowNum +
                                    "<40,\"Risco Médio\",IF(F" + rowNum +
                                    "<80,\"Risco Alto\",\"Risco Extremo\"))))"
                    );
                } else if (columnName.equalsIgnoreCase("Classificação do Risco Residual")) {
                    cell.setCellFormula(
                            "IF(L" + rowNum + "=0,\"\",IF(L" + rowNum +
                                    "<10,\"Risco Baixo\",IF(L" + rowNum +
                                    "<40,\"Risco Médio\",IF(L" + rowNum +
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

                cell.setCellStyle(defaultDataStyle);
            }
        }

        for (int i = 0; i < EXTRA_EDITABLE_ROWS; i++) {
            Row dataRow = sheet.createRow(r++);
            int rowNum = dataRow.getRowNum() + 1;

            for (int c = 0; c < headerList.size(); c++) {
                String columnName = headerList.get(c);
                Cell cell = dataRow.createCell(c);

                if (columnName.equalsIgnoreCase("Risco Inerente (PxI)")) {
                    cell.setCellFormula("IF(OR(C" + rowNum + "=\"\",E" + rowNum + "=\"\"),\"\",C" + rowNum + "*E" + rowNum + ")");
                } else if (columnName.equalsIgnoreCase("Evento de Risco")) {
                    cell.setCellFormula("IF('" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + ")");
                } else if (columnName.equalsIgnoreCase("Risco Residual")) {
                    cell.setCellFormula("IF(OR(F" + rowNum + "=\"\",K" + rowNum + "=\"\"),\"\",F" + rowNum + "*K" + rowNum + ")");
                } else if (columnName.equalsIgnoreCase("P")) {
                    cell.setCellFormula(
                            "IF(B" + rowNum + "=\"\",\"\",IF(B" + rowNum + "=\"Muito alta\",10," +
                                    "IF(B" + rowNum + "=\"Alta\",8," +
                                    "IF(B" + rowNum + "=\"Média\",5," +
                                    "IF(B" + rowNum + "=\"Baixa\",2," +
                                    "IF(B" + rowNum + "=\"Muito baixa\",1,0))))))"
                    );
                } else if (columnName.equalsIgnoreCase("I")) {
                    cell.setCellFormula(
                            "IF(D" + rowNum + "=\"\",\"\",IF(D" + rowNum + "=\"Muito alto\",10," +
                                    "IF(D" + rowNum + "=\"Alto\",8," +
                                    "IF(D" + rowNum + "=\"Médio\",5," +
                                    "IF(D" + rowNum + "=\"Baixo\",2," +
                                    "IF(D" + rowNum + "=\"Muito baixo\",1,0))))))"
                    );
                } else if (columnName.equalsIgnoreCase("FAC")) {
                    cell.setCellFormula(
                            "IF(J" + rowNum + "=\"\",\"\",IF(J" + rowNum + "=\"Forte\",0.2," +
                                    "IF(J" + rowNum + "=\"Satisfatório\",0.4," +
                                    "IF(J" + rowNum + "=\"Mediano\",0.6," +
                                    "IF(J" + rowNum + "=\"Fraco\",0.8,1)))))"
                    );
                } else if (columnName.equalsIgnoreCase("Classificação do Risco Inerente")) {
                    cell.setCellFormula(
                            "IF(F" + rowNum + "=0,\"\",IF(F" + rowNum +
                                    "<10,\"Risco Baixo\",IF(F" + rowNum +
                                    "<40,\"Risco Médio\",IF(F" + rowNum +
                                    "<80,\"Risco Alto\",\"Risco Extremo\"))))"
                    );
                } else if (columnName.equalsIgnoreCase("Classificação do Risco Residual")) {
                    cell.setCellFormula(
                            "IF(L" + rowNum + "=0,\"\",IF(L" + rowNum +
                                    "<10,\"Risco Baixo\",IF(L" + rowNum +
                                    "<40,\"Risco Médio\",IF(L" + rowNum +
                                    "<80,\"Risco Alto\",\"Risco Extremo\"))))"
                    );
                } else {
                    cell.setCellValue("");
                }

                cell.setCellStyle(defaultDataStyle);
            }
        }

        applyClassificationConditionalFormatting(sheet, headerList, "Classificação do Risco Inerente");
        applyClassificationConditionalFormatting(sheet, headerList, "Classificação do Risco Residual");
        setupValidations(sheet, headerList);
        setColumnWidths(sheet, headerList);
    }

    private String resolveEtapa2SheetName(XSSFWorkbook wb) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            String name = wb.getSheetName(i);
            if (name != null && name.contains("ETAPA 2")) {
                return name;
            }
        }
        return ETAPA_2_SHEET_FALLBACK_NAME;
    }

    private void applyClassificationConditionalFormatting(XSSFSheet sheet, List<String> headers, String columnHeader) {
        int classificationCol = headers.indexOf(columnHeader);
        if (classificationCol == -1) {
            return;
        }

        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
        String colLetter = CellReference.convertNumToColString(classificationCol);
        String baseRef = "$" + colLetter + "3";

        ConditionalFormattingRule ruleExtremo = sheetCF.createConditionalFormattingRule(
                "ISNUMBER(SEARCH(\"Extremo\"," + baseRef + "))"
        );
        PatternFormatting fmtExtremo = ruleExtremo.createPatternFormatting();
        fmtExtremo.setFillForegroundColor(IndexedColors.RED.getIndex());
        fmtExtremo.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        FontFormatting fontExtremo = ruleExtremo.createFontFormatting();
        fontExtremo.setFontColorIndex(IndexedColors.BLACK.getIndex());

        ConditionalFormattingRule ruleAlto = sheetCF.createConditionalFormattingRule(
                "ISNUMBER(SEARCH(\"Alto\"," + baseRef + "))"
        );
        PatternFormatting fmtAlto = ruleAlto.createPatternFormatting();
        fmtAlto.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        fmtAlto.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        FontFormatting fontAlto = ruleAlto.createFontFormatting();
        fontAlto.setFontColorIndex(IndexedColors.BLACK.getIndex());

        ConditionalFormattingRule ruleMedio = sheetCF.createConditionalFormattingRule(
                "ISNUMBER(SEARCH(\"Médio\"," + baseRef + "))"
        );
        PatternFormatting fmtMedio = ruleMedio.createPatternFormatting();
        fmtMedio.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        fmtMedio.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        FontFormatting fontMedio = ruleMedio.createFontFormatting();
        fontMedio.setFontColorIndex(IndexedColors.BLACK.getIndex());

        ConditionalFormattingRule ruleBaixo = sheetCF.createConditionalFormattingRule(
                "ISNUMBER(SEARCH(\"Baixo\"," + baseRef + "))"
        );
        PatternFormatting fmtBaixo = ruleBaixo.createPatternFormatting();
        fmtBaixo.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
        fmtBaixo.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        FontFormatting fontBaixo = ruleBaixo.createFontFormatting();
        fontBaixo.setFontColorIndex(IndexedColors.BLACK.getIndex());

        CellRangeAddress[] regions = {new CellRangeAddress(2, 1000, classificationCol, classificationCol)};
        ConditionalFormattingRule[] rules = {ruleExtremo, ruleAlto, ruleMedio, ruleBaixo};
        sheetCF.addConditionalFormatting(regions, rules);
    }

    private void setupValidations(XSSFSheet sheet, List<String> headers) {

        DataValidationHelper helper = sheet.getDataValidationHelper();

        int colImpacto = headers.indexOf("Impacto");
        if (colImpacto != -1) {
            applySelect(sheet, helper,
                    new String[]{"Muito baixo", "Baixo", "Médio", "Alto", "Muito alto"},
                    colImpacto);
        }

        int colAvaliacao = headers.indexOf("Avaliação dos Controles");
        if (colAvaliacao != -1) {
            applySelect(sheet, helper,
                    new String[]{"Inexistente", "Fraco", "Mediano", "Satisfatório", "Forte"},
                    colAvaliacao);
        }

        int colProbabilidade = headers.indexOf("Probabilidade");
        if (colProbabilidade != -1) {
            applySelect(sheet, helper,
                    new String[]{"Muito baixa", "Baixa", "Média", "Alta", "Muito alta"},
                    colProbabilidade);
        }
    }

    private void applySelect(XSSFSheet sheet, DataValidationHelper helper, String[] opts, int col) {
        DataValidationConstraint constraint = helper.createExplicitListConstraint(opts);
        CellRangeAddressList addressList = new CellRangeAddressList(2, 1000, col, col);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void setColumnWidths(XSSFSheet sheet, List<String> headers) {
        for (int c = 0; c < headers.size(); c++) {
            String header = headers.get(c);

            if (header.equals("P") || header.equals("I") || header.equals("FAC")) {
                sheet.setColumnWidth(c, 1500);
            } else if (header.contains("Classificação")) {
                sheet.setColumnWidth(c, 6000);
            } else if (header.contains("Evento de Risco")) {
                sheet.setColumnWidth(c, 25000);
            } else if (header.contains("Data da Última Avaliação") || header.contains("Data")) {
                sheet.setColumnWidth(c, 5000);
            } else if (header.contains("Avaliação dos Controles")) {
                sheet.setColumnWidth(c, 5500);
            } else if (header.contains("Controles Preventivos")
                    || header.contains("Controles de Atenuação e recuperação")) {
                sheet.setColumnWidth(c, 15000);
            } else {
                sheet.setColumnWidth(c, 5000);
            }
        }
    }

    private void createGroupHeader(
            XSSFWorkbook wb,
            Row row,
            int start,
            int end,
            String text,
            XSSFColor color,
            XSSFSheet sheet
    ) {

        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(style);

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

        sheet.addMergedRegion(new CellRangeAddress(
                row.getRowNum(),
                row.getRowNum(),
                start,
                end
        ));
    }

    private void applyBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

}
