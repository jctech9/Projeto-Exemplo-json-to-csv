package com.example.demo.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
public class OcorrenciaRiscoService {

    private static final int EXTRA_EDITABLE_ROWS = 10;
    private static final List<String> FIXED_HEADERS = Arrays.asList(
            "Evento de Risco",
            "Data da Ocorrência",
            "Descrição da Ocorrência",
            "Responsável pela Solução",
            "Solução",
            "Resultados"
    );

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {
        XSSFSheet sheet = wb.createSheet(sheetName);

        byte[] rgbTeal = new byte[]{(byte) 0, (byte) 188, (byte) 212};
        XSSFColor npiTeal = new XSSFColor(rgbTeal, null);

        int r = 0;

        // 1. Estilos
        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(npiTeal);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        SheetServiceUtils.applyBorders(headerStyle);
        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        headerStyle.setFont(boldFont);

        CellStyle dataStyleLeft = wb.createCellStyle();
        SheetServiceUtils.applyBorders(dataStyleLeft);
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
        dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle dataStyleCenter = wb.createCellStyle();
        SheetServiceUtils.applyBorders(dataStyleCenter);
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);

        // 2. Cabeçalho Duplo
        Row row1 = sheet.createRow(r++);
        Cell titleCell = row1.createCell(0);
        titleCell.setCellValue("Ocorrências de Risco");
        titleCell.setCellStyle(headerStyle);

        // Mesclar título das colunas A até F (0 a 5)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        for (int i = 1; i <= 5; i++) {
            row1.createCell(i).setCellStyle(headerStyle);
        }

        // 3. Nomes das Colunas (Linha 2)
        List<String> headerList = FIXED_HEADERS;

        Row row2 = sheet.createRow(r++);
        row2.setHeightInPoints(35);
        for (int c = 0; c < headerList.size(); c++) {
            Cell cell = row2.createCell(c);
            cell.setCellValue(headerList.get(c));
            cell.setCellStyle(headerStyle);

            String hName = headerList.get(c);

            if (hName.equalsIgnoreCase("Descrição da Ocorrência")) {
                cell.setCellValue("Descrição da Ocorrência\n(descrever)");
            } else if (hName.equalsIgnoreCase("Responsável pela Solução")) {
                cell.setCellValue("Responsável pela\nSolução");
            } else if (hName.equalsIgnoreCase("Solução")) {
                cell.setCellValue("Solução\n(descrever)");
            } else if (hName.equalsIgnoreCase("Resultados")) {
                cell.setCellValue("Resultados\n(descrever)");
            } else {
                cell.setCellValue(hName);
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

                if (headerName.contains("Evento") || headerName.contains("Descrição")
                        || headerName.contains("Solução") || headerName.contains("Resultados")) {
                    cell.setCellStyle(dataStyleLeft);
                } else {
                    cell.setCellStyle(dataStyleCenter);
                }
            }
        }

        for (int i = 0; i < EXTRA_EDITABLE_ROWS; i++) {
            Row dataRow = sheet.createRow(r++);
            for (int c = 0; c < headerList.size(); c++) {
                String headerName = headerList.get(c);
                Cell cell = dataRow.createCell(c);
                cell.setCellValue("");

                if (headerName.contains("Evento") || headerName.contains("Descrição")
                        || headerName.contains("Solução") || headerName.contains("Resultados")) {
                    cell.setCellStyle(dataStyleLeft);
                } else {
                    cell.setCellStyle(dataStyleCenter);
                }
            }
        }

        setColumnWidths(sheet, headerList);
    }

    private void setColumnWidths(XSSFSheet sheet, List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h.contains("Descrição") || h.contains("Solução")) {
                sheet.setColumnWidth(i, 18000);
            } else if (h.contains("Evento") || h.contains("Resultados")) {
                sheet.setColumnWidth(i, 20000);
            } else if (h.contains("Responsável")) {
                sheet.setColumnWidth(i, 9000);
            } else {
                sheet.setColumnWidth(i, 7000);
            }
        }
    }

}
