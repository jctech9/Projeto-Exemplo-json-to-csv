package com.example.demo.infrastructure.excel.sheet;

import com.example.demo.contracts.SheetNames;
import com.example.demo.infrastructure.excel.shared.SheetServiceUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FontFormatting;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class AvaliacaoRiscosService extends AbstractWorksheetTemplateService {

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
        String etapa2SheetName = SheetServiceUtils.resolveSheetName(
                wb,
                SheetNames.ETAPA_2.marker(),
                SheetNames.ETAPA_2.displayName()
        );
        int firstEditableRow = 2;
        int lastEditableRow = SheetServiceUtils.computeLastEditableRow(firstEditableRow, rows.size(), EXTRA_EDITABLE_ROWS);

        XSSFColor headerColor = color((byte) 230, (byte) 145, (byte) 145);

        List<String> headerList = FIXED_HEADERS;
        int r = 0;

        Row groupRow = sheet.createRow(r++);
        XSSFCellStyle groupHeaderStyle = createFilledBoldStyle(
            wb,
            headerColor,
            HorizontalAlignment.CENTER,
            VerticalAlignment.CENTER,
            false
        );
        createMergedHeaderInRow(groupRow, 0, 6, "Avaliação dos Riscos", groupHeaderStyle, sheet);
        createMergedHeaderInRow(groupRow, 7, 9, "Avaliação dos Controles", groupHeaderStyle, sheet);
        createMergedHeaderInRow(groupRow, 10, 13, "Risco Residual", groupHeaderStyle, sheet);

        XSSFCellStyle headerStyle = createFilledBoldStyle(
            wb,
            headerColor,
            HorizontalAlignment.CENTER,
            VerticalAlignment.CENTER,
            true
        );

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

        CellStyle defaultDataStyle = createDataStyle(
            wb,
            HorizontalAlignment.LEFT,
            VerticalAlignment.CENTER,
            false
        );

        r = appendDataRowsWithExtra(
            sheet,
            r,
            rows,
            EXTRA_EDITABLE_ROWS,
            (dataRow, rowData) -> {
                int rowNum = dataRow.getRowNum() + 1;
                populateDataRow(headerList, dataRow, rowData, rowNum, etapa2SheetName, defaultDataStyle);
            }
        );

        applyClassificationConditionalFormatting(sheet, headerList, "Classificação do Risco Inerente", firstEditableRow, lastEditableRow);
        applyClassificationConditionalFormatting(sheet, headerList, "Classificação do Risco Residual", firstEditableRow, lastEditableRow);
        setupValidations(sheet, headerList, firstEditableRow, lastEditableRow);
        setColumnWidths(sheet, headerList);
    }

    private void populateDataRow(
            List<String> headerList,
            Row dataRow,
            Map<String, Object> rowData,
            int rowNum,
            String etapa2SheetName,
            CellStyle defaultDataStyle
    ) {
        for (int c = 0; c < headerList.size(); c++) {
            String columnName = headerList.get(c);
            Cell cell = dataRow.createCell(c);
            Object val = rowData == null ? null : rowData.get(columnName);
            String strVal = val == null ? "" : String.valueOf(val);

            // Colunas derivadas são mantidas por fórmula para recalcular quando o usuário editar selects.
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
            } else if (rowData == null) {
                cell.setCellValue("");
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

    private void applyClassificationConditionalFormatting(
            XSSFSheet sheet,
            List<String> headers,
            String columnHeader,
            int firstEditableRow,
            int lastEditableRow
    ) {
        int classificationCol = headers.indexOf(columnHeader);
        if (classificationCol == -1) {
            return;
        }

        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
        String colLetter = CellReference.convertNumToColString(classificationCol);
        // A regra é criada uma vez com referência relativa e replicada para todo o intervalo.
        String baseRef = "$" + colLetter + (firstEditableRow + 1);

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

        CellRangeAddress[] regions = {new CellRangeAddress(firstEditableRow, lastEditableRow, classificationCol, classificationCol)};
        ConditionalFormattingRule[] rules = {ruleExtremo, ruleAlto, ruleMedio, ruleBaixo};
        sheetCF.addConditionalFormatting(regions, rules);
    }

    private void setupValidations(XSSFSheet sheet, List<String> headers, int firstEditableRow, int lastEditableRow) {

        DataValidationHelper helper = sheet.getDataValidationHelper();

        int colImpacto = headers.indexOf("Impacto");
        applySelectValidation(
                sheet,
                helper,
                new String[]{"Muito baixo", "Baixo", "Médio", "Alto", "Muito alto"},
                colImpacto,
                firstEditableRow,
                lastEditableRow
        );

        int colAvaliacao = headers.indexOf("Avaliação dos Controles");
        applySelectValidation(
                sheet,
                helper,
                new String[]{"Inexistente", "Fraco", "Mediano", "Satisfatório", "Forte"},
                colAvaliacao,
                firstEditableRow,
                lastEditableRow
        );

        int colProbabilidade = headers.indexOf("Probabilidade");
        applySelectValidation(
                sheet,
                helper,
                new String[]{"Muito baixa", "Baixa", "Média", "Alta", "Muito alta"},
                colProbabilidade,
                firstEditableRow,
                lastEditableRow
        );
    }

    private void setColumnWidths(XSSFSheet sheet, List<String> headers) {
        applyColumnWidthsByHeaders(sheet, headers, header -> {
            if (header.equals("P") || header.equals("I") || header.equals("FAC")) {
                return 1500;
            }
            if (header.contains("Classificação")) {
                return 6000;
            }
            if (header.contains("Evento de Risco")) {
                return 25000;
            }
            if (header.contains("Data da Última Avaliação") || header.contains("Data")) {
                return 5000;
            }
            if (header.contains("Avaliação dos Controles")) {
                return 5500;
            }
            if (header.contains("Controles Preventivos")
                    || header.contains("Controles de Atenuação e recuperação")) {
                return 15000;
            }
            return 5000;
        });
    }

}
