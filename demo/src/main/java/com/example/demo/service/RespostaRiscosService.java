package com.example.demo.service;

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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class RespostaRiscosService {

    private static final String ETAPA_2_SHEET_FALLBACK_NAME = "ETAPA 2. IDENTIF. DE EVENTOS";
    private static final int EXTRA_EDITABLE_ROWS = 10;
    private static final List<String> FIXED_HEADERS = Arrays.asList(
            "Processo",
            "Fase",
            "Evento de Risco",
            "Opção de Tratamento",
            "Justificativa da escolha da opção de tratamento"
    );

    private static final String[] OPCOES_TRATAMENTO = {
            "Aceitar",
            "Mitigar",
            "Compartilhar",
            "Evitar"
    };

    public void generateSheet(
            XSSFWorkbook wb,
            String sheetName,
            List<Map<String, Object>> rows
    ) {

        XSSFSheet sheet = wb.createSheet(sheetName);
        String etapa2SheetName = SheetServiceUtils.resolveSheetName(wb, "ETAPA 2", ETAPA_2_SHEET_FALLBACK_NAME);
        int firstEditableRow = 2;
        int lastEditableRow = SheetServiceUtils.computeLastEditableRow(firstEditableRow, rows.size(), EXTRA_EDITABLE_ROWS);

        byte[] rgbYellow = new byte[]{(byte) 255, (byte) 255, (byte) 0};
        XSSFColor headerColor = new XSSFColor(rgbYellow, null);

        int r = 0;

        Row titleRow = sheet.createRow(r++);

        XSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFillForegroundColor(headerColor);
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        SheetServiceUtils.applyBorders(titleStyle);

        Font bold = wb.createFont();
        bold.setBold(true);
        titleStyle.setFont(bold);

        for (int c = 0; c <= 4; c++) {
            Cell cell = titleRow.createCell(c);
            if (c == 2) {
                cell.setCellValue("Resposta aos Riscos");
            }
            cell.setCellStyle(titleStyle);
        }

        sheet.addMergedRegion(new CellRangeAddress(
                titleRow.getRowNum(),
                titleRow.getRowNum(),
                2,
                4
        ));

        Row headerRow = sheet.createRow(r++);

        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(headerColor);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        SheetServiceUtils.applyBorders(headerStyle);

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int c = 0; c < FIXED_HEADERS.size(); c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(FIXED_HEADERS.get(c));
            cell.setCellStyle(headerStyle);
        }

        CellStyle dataStyleLeft = wb.createCellStyle();
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
        dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        SheetServiceUtils.applyBorders(dataStyleLeft);

        CellStyle dataStyleCenter = wb.createCellStyle();
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        SheetServiceUtils.applyBorders(dataStyleCenter);

        int totalRows = rows.size() + EXTRA_EDITABLE_ROWS;
        for (int i = 0; i < totalRows; i++) {
            Map<String, Object> rowData = i < rows.size() ? rows.get(i) : null;
            Row dataRow = sheet.createRow(r++);
            int rowNum = dataRow.getRowNum() + 1;
            populateDataRow(dataRow, rowData, rowNum, etapa2SheetName, dataStyleLeft, dataStyleCenter);
        }

        DataValidationHelper helper = sheet.getDataValidationHelper();
        SheetServiceUtils.applySelect(sheet, helper, OPCOES_TRATAMENTO, 3, firstEditableRow, lastEditableRow);

        // ---------- TAMANHO COLUNAS ----------
        sheet.setColumnWidth(0, 9000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 22000);
        sheet.setColumnWidth(3, 7000);
        sheet.setColumnWidth(4, 50000);
    }

    private void populateDataRow(
            Row dataRow,
            Map<String, Object> rowData,
            int rowNum,
            String etapa2SheetName,
            CellStyle dataStyleLeft,
            CellStyle dataStyleCenter
    ) {
        Cell cell0 = dataRow.createCell(0);
        cell0.setCellFormula("IF('" + etapa2SheetName.replace("'", "''") + "'!A" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!A" + rowNum + ")");
        cell0.setCellStyle(dataStyleLeft);

        Cell cell1 = dataRow.createCell(1);
        cell1.setCellValue(rowData == null ? "" : String.valueOf(rowData.getOrDefault("Fase", "")));
        cell1.setCellStyle(dataStyleCenter);

        Cell cell2 = dataRow.createCell(2);
        cell2.setCellFormula("IF('" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + "=\"\",\"\",'" + etapa2SheetName.replace("'", "''") + "'!C" + rowNum + ")");
        cell2.setCellStyle(dataStyleLeft);

        Cell cell3 = dataRow.createCell(3);
        String rawOpcao = rowData == null ? "" : String.valueOf(rowData.getOrDefault("Opção de Tratamento", ""));
        cell3.setCellValue(normalizeOpcaoTratamento(rawOpcao));
        cell3.setCellStyle(dataStyleCenter);

        Cell cell4 = dataRow.createCell(4);
        cell4.setCellValue(rowData == null ? "" : String.valueOf(rowData.getOrDefault("Justificativa da escolha da opção de tratamento", "")));
        cell4.setCellStyle(dataStyleLeft);
    }

    private String normalizeOpcaoTratamento(String opcao) {
        if (opcao == null) {
            return "";
        }

        String trimmed = opcao.trim();
        for (String option : OPCOES_TRATAMENTO) {
            if (option.equalsIgnoreCase(trimmed)) {
                return option;
            }
        }
        return trimmed;
    }

}
