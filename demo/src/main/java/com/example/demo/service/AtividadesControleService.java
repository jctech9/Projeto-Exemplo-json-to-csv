package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AtividadesControleService {

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {
        XSSFSheet sheet = wb.createSheet(sheetName);

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
        applyBorders(greyStyle);
        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        greyStyle.setFont(boldFont);

        XSSFCellStyle greyStyleLeft = wb.createCellStyle();
        greyStyleLeft.cloneStyleFrom(greyStyle);
        greyStyleLeft.setAlignment(HorizontalAlignment.LEFT);

        CellStyle dataStyleLeft = wb.createCellStyle();
        applyBorders(dataStyleLeft);
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);

        CellStyle dataStyleCenter = wb.createCellStyle();
        applyBorders(dataStyleCenter);
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);

        // 2. Criar Cabeçalho Duplo (Linha 1: Títulos de Grupo)
        Row row1 = sheet.createRow(r++);
        Row row2 = sheet.createRow(r++);

        // Plano de Tratamento (Colunas A até G)
        createGroupHeader(row1, 0, 7, "Plano de Tratamento", greyStyle, sheet);
        // Plano de Contingência (Colunas H até J)
        createGroupHeader(row1, 8, 10, "Plano de Contingência", greyStyle, sheet);

        // 3. Gerar Nomes das Colunas (Linha 2)
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) headers.addAll(row.keySet());
        List<String> headerList = new ArrayList<>(headers);

        for (int c = 0; c < headerList.size(); c++) {
            Cell cell = row2.createCell(c);
            String hName = headerList.get(c);
            cell.setCellValue(hName);

            // CORREÇÃO: Se o título for longo, alinha à esquerda para não "comer" o início
            if (hName.contains("Responsável") || hName.contains("Data") || hName.contains("Evento")) {
                cell.setCellStyle(greyStyleLeft);
            } else {
                cell.setCellStyle(greyStyle);
            }
        }

        // 4. Preenchimento de Dados
        for (Map<String, Object> rowData : rows) {
            Row dataRow = sheet.createRow(r++);
            for (int c = 0; c < headerList.size(); c++) {
                String headerName = headerList.get(c);
                Cell cell = dataRow.createCell(c);
                Object val = rowData.get(headerName);
                cell.setCellValue(val == null ? "" : String.valueOf(val));

                // Alinhamento à esquerda para descrições
                if (headerName.contains("Evento") || headerName.contains("Ações") || headerName.contains("Gatilho")
                || headerName.contains("Monitoramento")) {
                    cell.setCellStyle(dataStyleLeft);
                } else {
                    cell.setCellStyle(dataStyleCenter);
                }
            }
        }

        // 5. Formatação Condicional para Status
        applyStatusFormatting(sheet, headerList);

        // 6. Selects e Larguras
        setupValidations(sheet, headerList);
        setColumnWidths(sheet, headerList);
    }

    private void applyStatusFormatting(XSSFSheet sheet, List<String> headers) {
        int colStatus = headers.indexOf("Status");
        if (colStatus == -1) return;

        SheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();

        // Implementado -> Verde
        createRule(scf, colStatus, "\"Implementado\"", new byte[]{(byte) 146, (byte) 208, 80});
        // Em implementação -> Amarelo
        createRule(scf, colStatus, "\"Em implementação\"", new byte[]{(byte)255, (byte)255, 0});
        // Não implementado -> Vermelho
        createRule(scf, colStatus, "\"Não implementado\"", new byte[]{(byte)255, 0, 0});
    }

    private void createRule(SheetConditionalFormatting scf, int col, String value, byte[] rgb) {
        ConditionalFormattingRule rule = scf.createConditionalFormattingRule(ComparisonOperator.EQUAL, value);
        XSSFPatternFormatting fill = (XSSFPatternFormatting) rule.createPatternFormatting();
        fill.setFillBackgroundColor(new XSSFColor(rgb, null));
        fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        CellRangeAddress[] regions = { new CellRangeAddress(2, 1000, col, col) };
        scf.addConditionalFormatting(regions, rule);
    }

    private void createGroupHeader(Row row, int start, int end, String text, CellStyle style, XSSFSheet sheet) {
        Cell cell = row.createCell(start);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        for (int i = start + 1; i <= end; i++) row.createCell(i).setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), start, end));
    }

    private void setupValidations(XSSFSheet sheet, List<String> headers) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        int colStatus = headers.indexOf("Status");
        if (colStatus != -1) applySelect(sheet, helper, new String[]{"Não implementado", "Em implementação", "Implementado"}, colStatus);
    }

    private void applySelect(XSSFSheet sheet, DataValidationHelper helper, String[] opts, int col) {
        DataValidationConstraint c = helper.createExplicitListConstraint(opts);
        CellRangeAddressList addr = new CellRangeAddressList(2, 1000, col, col);
        DataValidation v = helper.createValidation(c, addr);
        v.setSuppressDropDownArrow(true);
        v.setShowErrorBox(true);
        sheet.addValidationData(v);
    }

    private void setColumnWidths(XSSFSheet sheet, List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h.contains("Evento") || h.contains("Ações") || h.contains("Gatilho") || h.contains("Monitoramento")) sheet.setColumnWidth(i, 15000);
            else if (h.contains("Data"))  {
                sheet.setColumnWidth(i, 10000);
            } else sheet.setColumnWidth(i, 6000);
        }
    }

    private void applyBorders(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}