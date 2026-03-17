package com.example.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class OcorrenciaRiscoService {

    public void generateSheet(XSSFWorkbook wb, String sheetName, List<Map<String, Object>> rows) {
        XSSFSheet sheet = wb.createSheet(sheetName);

        // COR TEAL/CIANO PERSONALIZADA (RGB da imagem)
        byte[] rgbTeal = new byte[]{(byte) 0, (byte) 188, (byte) 212};
        XSSFColor npiTeal = new XSSFColor(rgbTeal, null);

        int r = 0;

        // 1. Estilos
        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(npiTeal);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(headerStyle);
        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        headerStyle.setFont(boldFont);

        CellStyle dataStyleLeft = wb.createCellStyle();
        applyBorders(dataStyleLeft);
        dataStyleLeft.setAlignment(HorizontalAlignment.LEFT);
        dataStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle dataStyleCenter = wb.createCellStyle();
        applyBorders(dataStyleCenter);
        dataStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        dataStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);

        // 2. Cabeçalho Duplo
        Row row1 = sheet.createRow(r++);
        Cell titleCell = row1.createCell(0);
        titleCell.setCellValue("Ocorrências de Risco");
        titleCell.setCellStyle(headerStyle);

        // Mesclar título das colunas A até F (0 a 5)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        for(int i=1; i<=5; i++) row1.createCell(i).setCellStyle(headerStyle);

        // 3. Nomes das Colunas (Linha 2)
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        if (!rows.isEmpty()) headers.addAll(rows.get(0).keySet());
        List<String> headerList = new ArrayList<>(headers);

        Row row2 = sheet.createRow(r++);
        for (int c = 0; c < headerList.size(); c++) {
            Cell cell = row2.createCell(c);
            cell.setCellValue(headerList.get(c));
            cell.setCellStyle(headerStyle);
        }

        // 4. Preenchimento de Dados
        for (Map<String, Object> rowData : rows) {
            Row dataRow = sheet.createRow(r++);
            for (int c = 0; c < headerList.size(); c++) {
                String headerName = headerList.get(c);
                Cell cell = dataRow.createCell(c);
                Object val = rowData.get(headerName);
                cell.setCellValue(val == null ? "" : String.valueOf(val));

                // Alinhamento conforme o modelo
                if (headerName.contains("Evento") || headerName.contains("descritiva") || headerName.contains("Solução") || headerName.contains("Resultados") || headerName.contains("Descrição")) {
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
            if (h.contains("(descrever)") || h.contains("descritiva")) {
                sheet.setColumnWidth(i, 18000);
            }
            // Coluna do Evento (Geralmente a maior)
            else if (h.contains("Evento") || h.contains("Resultados")) {
                sheet.setColumnWidth(i, 29000);
            }
            // Coluna de Responsáveis (Diminuída conforme solicitado)
            else if (h.contains("Responsável")) {
                sheet.setColumnWidth(i, 9000); // Largura ideal para nomes como "João Silva"
            }
            // Colunas curtas (Datas, etc)
            else {
                sheet.setColumnWidth(i, 7000);
            }
        }
    }

    private void applyBorders(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}