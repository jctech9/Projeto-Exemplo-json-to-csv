package com.example.demo.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DadosProcessoService {

    private static final String[] OBJETIVOS_KEYS = {
            "Objetivos do Processo (Geral e específicos)",
            "Objetivos Gerais"
    };
    private static final String[] RESPONSAVEL_KEYS = {
            "Responsável"
    };

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {

        XSSFSheet sheet = wb.createSheet(sheetName);
        int lastCol = 4;

        // Estilos base do template da ETAPA 1.
        XSSFColor green = new XSSFColor(new byte[]{(byte) 169, (byte) 208, (byte) 142}, null);
        XSSFCellStyle metaLabelStyle = wb.createCellStyle();
        metaLabelStyle.setFillForegroundColor(green);
        metaLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        metaLabelStyle.setAlignment(HorizontalAlignment.LEFT);
        metaLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        SheetServiceUtils.applyBorders(metaLabelStyle);

        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        metaLabelStyle.setFont(boldFont);

        XSSFCellStyle metaValueStyle = wb.createCellStyle();
        metaValueStyle.setAlignment(HorizontalAlignment.LEFT);
        metaValueStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        metaValueStyle.setWrapText(true);
        SheetServiceUtils.applyBorders(metaValueStyle);

        XSSFCellStyle sectionTitleStyle = wb.createCellStyle();
        sectionTitleStyle.cloneStyleFrom(metaLabelStyle);
        sectionTitleStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.cloneStyleFrom(metaLabelStyle);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setWrapText(true);

        XSSFCellStyle dataStyleLeft = wb.createCellStyle();
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
        dataStyleLeft.setVerticalAlignment(VerticalAlignment.TOP);
        dataStyleLeft.setWrapText(true);
        SheetServiceUtils.applyBorders(dataStyleLeft);

        String unidade = getFirstValue(rows, "Unidade");
        String responsavel = getFirstValue(rows, RESPONSAVEL_KEYS);

        int r = 0;

        // Bloco superior com metadados do processo.
        createMetaRow(sheet, r++, "Unidade:", unidade, metaLabelStyle, metaValueStyle, lastCol);
        createMetaRow(sheet, r++, "Responsável pelo gerenciamento:", responsavel, metaLabelStyle, metaValueStyle, lastCol);

        // Seção principal com título e cabeçalho de colunas.
        org.apache.poi.ss.usermodel.Row sectionRow = sheet.createRow(r++);
        for (int c = 0; c <= lastCol; c++) {
            Cell cell = sectionRow.createCell(c);
            if (c == 0) {
                cell.setCellValue("Processos");
            }
            cell.setCellStyle(sectionTitleStyle);
        }
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(sectionRow.getRowNum(), sectionRow.getRowNum(), 0, lastCol));

        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(r++);
        Cell processoHeader = headerRow.createCell(0);
        processoHeader.setCellValue("Processo\n(indicar)");
        processoHeader.setCellStyle(headerStyle);

        for (int c = 1; c <= lastCol; c++) {
            Cell headerCell = headerRow.createCell(c);
            if (c == 1) {
                headerCell.setCellValue("Objetivos do Processo (Geral e específicos)\n(descrever)");
            }
            headerCell.setCellStyle(headerStyle);
        }
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(headerRow.getRowNum(), headerRow.getRowNum(), 1, lastCol));
        headerRow.setHeightInPoints(30f);

        // Linhas de conteúdo; a coluna de objetivos é mesclada para texto longo.
        for (Map<String, Object> rowData : rows) {
            org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(r++);

            Cell processoCell = dataRow.createCell(0);
            processoCell.setCellValue(String.valueOf(rowData.getOrDefault("Processo", "")));
            processoCell.setCellStyle(dataStyleLeft);

            for (int c = 1; c <= lastCol; c++) {
                Cell objetivoCell = dataRow.createCell(c);
                if (c == 1) {
                    objetivoCell.setCellValue(getValue(rowData, OBJETIVOS_KEYS));
                }
                objetivoCell.setCellStyle(dataStyleLeft);
            }
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(dataRow.getRowNum(), dataRow.getRowNum(), 1, lastCol));

            dataRow.setHeightInPoints(36f);
        }

        sheet.setColumnWidth(0, 11000);
        sheet.setColumnWidth(1, 22000);
        sheet.setColumnWidth(2, 9000);
        sheet.setColumnWidth(3, 9000);
        sheet.setColumnWidth(4, 9000);
    }

    private void createMetaRow(
            XSSFSheet sheet,
            int rowIndex,
            String label,
            String value,
            CellStyle labelStyle,
            CellStyle valueStyle,
            int lastCol) {

        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        for (int c = 1; c <= lastCol; c++) {
            Cell valueCell = row.createCell(c);
            if (c == 1) {
                valueCell.setCellValue(value == null ? "" : value);
            }
            valueCell.setCellStyle(valueStyle);
        }

        // Mescla os campos de valor para manter o padrão visual do formulário.
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(row.getRowNum(), row.getRowNum(), 1, lastCol));
    }

    private String getFirstValue(List<Map<String, Object>> rows, String... keys) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }

        return getValue(rows.get(0), keys);
    }

    private String getValue(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) {
            return "";
        }

        for (String key : keys) {
            if (key != null && row.containsKey(key)) {
                Object value = row.get(key);
                return value == null ? "" : String.valueOf(value);
            }
        }
        return "";
    }

}
