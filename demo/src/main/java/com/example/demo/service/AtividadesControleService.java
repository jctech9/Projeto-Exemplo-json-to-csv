package com.example.demo.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class AtividadesControleService {

    private static final String ETAPA_2_SHEET_FALLBACK_NAME = "ETAPA 2. IDENTIF. DE EVENTOS";
    private static final String ETAPA_4_SHEET_FALLBACK_NAME = "ETAPA 4. RESPOSTA AOS RISCOS";
    private static final int EXTRA_EDITABLE_ROWS = 10;
    private static final List<String> FIXED_HEADERS = Arrays.asList(
            "Evento de Risco",
            "Opção de Tratamento",
            "Responsável pelo Tratamento",
            "Data prevista para início da implementação",
            "Data prevista para o fim da implementação",
            "Status",
            "Ações preventivas (descrever)",
            "Monitoramento",
            "Gatilho (descrever)",
            "Ações de Contingência (descrever)",
            "Responsável"
    );

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {
        XSSFSheet sheet = wb.createSheet(sheetName);
        String etapa2SheetName = SheetServiceUtils.resolveSheetName(wb, "ETAPA 2", ETAPA_2_SHEET_FALLBACK_NAME);
        String etapa4SheetName = SheetServiceUtils.resolveSheetName(wb, "ETAPA 4", ETAPA_4_SHEET_FALLBACK_NAME);
        int firstEditableRow = 2;
        int lastEditableRow = SheetServiceUtils.computeLastEditableRow(firstEditableRow, rows.size(), EXTRA_EDITABLE_ROWS);

        // Cor Cinza para o Cabeçalho
        byte[] rgbGrey = new byte[]{(byte) 217, (byte) 217, (byte) 217};
        XSSFColor npiGrey = new XSSFColor(rgbGrey, null);

        int r = 0;

        // 1. Estilos de Cabeçalho e Dados
        XSSFCellStyle greyStyle = wb.createCellStyle();
        greyStyle.setFillForegroundColor(npiGrey);
        greyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        greyStyle.setAlignment(HorizontalAlignment.CENTER);
        greyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        greyStyle.setWrapText(true);
        SheetServiceUtils.applyBorders(greyStyle);
        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        greyStyle.setFont(boldFont);

        CellStyle dataStyleLeft = wb.createCellStyle();
        SheetServiceUtils.applyBorders(dataStyleLeft);
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);

        CellStyle dataStyleCenter = wb.createCellStyle();
        SheetServiceUtils.applyBorders(dataStyleCenter);
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);

        // 2. Criar Cabeçalho Duplo (Linha 1: Títulos de Grupo)
        Row row1 = sheet.createRow(r++);
        Row row2 = sheet.createRow(r++);
        row2.setHeightInPoints(35);

        // Plano de Tratamento (Colunas A até G)
        createGroupHeader(row1, 0, 7, "Plano de Tratamento", greyStyle, sheet);
        // Plano de Contingência (Colunas H até J)
        createGroupHeader(row1, 8, 10, "Plano de Contingência", greyStyle, sheet);

        // 3. Gerar Nomes das Colunas (Linha 2)
        List<String> headerList = FIXED_HEADERS;

        for (int c = 0; c < headerList.size(); c++) {
            Cell cell = row2.createCell(c);
            String hName = headerList.get(c);
            cell.setCellValue(hName);

            if (hName.equalsIgnoreCase("Opção de Tratamento")) {
                cell.setCellValue("Opção de\nTratamento");
            } else if (hName.equalsIgnoreCase("Responsável pelo Tratamento")) {
                cell.setCellValue("Responsável pelo\nTratamento");
            } else if (hName.equalsIgnoreCase("Data prevista para início da implementação")) {
                cell.setCellValue("Data prevista para início\nda implementação");
            } else if (hName.equalsIgnoreCase("Data prevista para o fim da implementação")) {
                cell.setCellValue("Data prevista para o fim\nda implementação");
            } else if (hName.equalsIgnoreCase("Ações preventivas (descrever)")) {
                cell.setCellValue("Ações preventivas\n(descrever)");
            } else if (hName.equalsIgnoreCase("Gatilho (descrever)")) {
                cell.setCellValue("Gatilho\n(descrever)");
            } else if (hName.equalsIgnoreCase("Ações de Contingência (descrever)")) {
                cell.setCellValue("Ações de Contingência\n(descrever)");
            } else {
                cell.setCellValue(hName);
            }

            cell.setCellStyle(greyStyle);
        }

        // 4. Preenchimento de Dados (linhas do payload + linhas extras editáveis)
        int totalRows = rows.size() + EXTRA_EDITABLE_ROWS;
        for (int i = 0; i < totalRows; i++) {
            Map<String, Object> rowData = i < rows.size() ? rows.get(i) : null;
            Row dataRow = sheet.createRow(r++);
            int rowNum = dataRow.getRowNum() + 1;

            for (int c = 0; c < headerList.size(); c++) {
                String headerName = headerList.get(c);
                Cell cell = dataRow.createCell(c);

                if (headerName.equalsIgnoreCase("Evento de Risco")) {
                    String formula = "IF('" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + ")";
                    cell.setCellFormula(formula);
                    cell.setCellStyle(dataStyleLeft);
                } else if (c == 1) {
                    String formula = "IF('" + etapa4SheetName.replace("'", "''") + "'!D" + rowNum + "=\"\",\"\",'" + etapa4SheetName.replace("'", "''") + "'!D" + rowNum + ")";
                    cell.setCellFormula(formula);
                    cell.setCellStyle(dataStyleCenter);
                } else {
                    Object val = rowData == null ? null : rowData.get(headerName);
                    cell.setCellValue(val == null ? "" : String.valueOf(val));
                    boolean isTextColumn = c == 0 || c == 6 || c == 7 || c == 8 || c == 9;
                    if (isTextColumn) {
                        cell.setCellStyle(dataStyleLeft);
                    } else {
                        cell.setCellStyle(dataStyleCenter);
                    }
                }
            }
        }

        // 5. Formatação Condicional para Status
        applyStatusFormatting(sheet, headerList, firstEditableRow, lastEditableRow);

        // 6. Selects e Larguras
        setupValidations(sheet, headerList, firstEditableRow, lastEditableRow);
        setColumnWidths(sheet, headerList);
    }

    private void applyStatusFormatting(XSSFSheet sheet, List<String> headers, int firstEditableRow, int lastEditableRow) {
        int colStatus = headers.indexOf("Status");
        if (colStatus == -1) return;

        SheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();

        // Implementado -> Verde
        createRule(scf, colStatus, "\"Implementado\"", IndexedColors.BRIGHT_GREEN.getIndex(), firstEditableRow, lastEditableRow);
        // Em implementação -> Amarelo
        createRule(scf, colStatus, "\"Em implementação\"", IndexedColors.YELLOW.getIndex(), firstEditableRow, lastEditableRow);
        // Não implementado -> Vermelho
        createRule(scf, colStatus, "\"Não implementado\"", IndexedColors.RED.getIndex(), firstEditableRow, lastEditableRow);
    }

    private void createRule(
            SheetConditionalFormatting scf,
            int col,
            String value,
            short colorIndex,
            int firstEditableRow,
            int lastEditableRow
    ) {
        ConditionalFormattingRule rule = scf.createConditionalFormattingRule(ComparisonOperator.EQUAL, value);
        PatternFormatting fill = rule.createPatternFormatting();
        fill.setFillForegroundColor(colorIndex);
        fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        CellRangeAddress[] regions = {new CellRangeAddress(firstEditableRow, lastEditableRow, col, col)};
        scf.addConditionalFormatting(regions, rule);
    }

    private void createGroupHeader(Row row, int start, int end, String text, CellStyle style, XSSFSheet sheet) {
        Cell cell = row.createCell(start);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        for (int i = start + 1; i <= end; i++) row.createCell(i).setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), start, end));
    }

    private void setupValidations(XSSFSheet sheet, List<String> headers, int firstEditableRow, int lastEditableRow) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        int colStatus = headers.indexOf("Status");
        if (colStatus != -1) {
            SheetServiceUtils.applySelect(
                    sheet,
                    helper,
                    new String[]{"Não implementado", "Em implementação", "Implementado"},
                    colStatus,
                    firstEditableRow,
                    lastEditableRow
            );
        }
    }

    private void setColumnWidths(XSSFSheet sheet, List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h.contains("Evento") || h.contains("Ações") || h.contains("Gatilho") || h.contains("Monitoramento")) sheet.setColumnWidth(i, 15000);
            else if (h.contains("Data") || h.contains("Responsável"))  {
                sheet.setColumnWidth(i, 7000);
            } else sheet.setColumnWidth(i, 6000);
        }
    }

}



