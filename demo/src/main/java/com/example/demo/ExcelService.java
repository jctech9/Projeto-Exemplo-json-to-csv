package com.example.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ExcelService {
    // Gera um arquivo XLSX em bytes a partir dos dados das etapas
    public byte[] generateXlsx(Map<String, List<Map<String, Object>>> etapas) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Map.Entry<String, List<Map<String, Object>>> entry : etapas.entrySet()) {
                XSSFSheet sheet = wb.createSheet(entry.getKey());
                List<Map<String, Object>> rows = entry.getValue();
                // Cabeçalhos: coletar todas as chaves únicas
                LinkedHashSet<String> headers = new LinkedHashSet<>();
                for (Map<String, Object> row : rows) {
                    headers.addAll(row.keySet());
                }
                List<String> headerList = new ArrayList<>(headers);

                boolean isEtapa3 = entry.getKey().contains("ETAPA 3");
                int r = 0;
                
                // ==================== ESTILIZAÇÃO DOS CABEÇALHOS ====================
                CellStyle headerStyle = wb.createCellStyle();
                Font headerFont = wb.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                // ==================== GRUPO DE CABEÇALHOS (ETAPA 3) ====================
                if (isEtapa3) {
                    CellStyle groupStyle = wb.createCellStyle();
                    Font groupFont = wb.createFont();
                    groupFont.setBold(true);
                    groupStyle.setFont(groupFont);
                    groupStyle.setAlignment(HorizontalAlignment.CENTER);

                    Row groupRow = sheet.createRow(r++);

                    // "Avaliação dos Riscos" -> colunas B-G (índices 1-6)
                    Cell g1 = groupRow.createCell(1);
                    g1.setCellValue("Avaliação dos Riscos");
                    g1.setCellStyle(groupStyle);
                    for (int ci = 2; ci <= 6; ci++) groupRow.createCell(ci).setCellStyle(groupStyle);
                    sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 6));

                    // "Avaliação dos Controles" -> colunas H-J (índices 7-9)
                    Cell g2 = groupRow.createCell(7);
                    g2.setCellValue("Avaliação dos Controles");
                    g2.setCellStyle(groupStyle);
                    for (int ci = 8; ci <= 9; ci++) groupRow.createCell(ci).setCellStyle(groupStyle);
                    sheet.addMergedRegion(new CellRangeAddress(0, 0, 7, 9));

                    // "Risco Residual" -> colunas K-N (índices 10-13)
                    Cell g3 = groupRow.createCell(10);
                    g3.setCellValue("Risco Residual");
                    g3.setCellStyle(groupStyle);
                    for (int ci = 11; ci <= 13; ci++) groupRow.createCell(ci).setCellStyle(groupStyle);
                    sheet.addMergedRegion(new CellRangeAddress(0, 0, 10, 13));
                }

                // Linha de cabeçalho
                Row headerRow = sheet.createRow(r++);
                for (int c = 0; c < headerList.size(); c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(headerList.get(c));
                    cell.setCellStyle(headerStyle);
                }

                // Linhas de dados
                for (Map<String, Object> row : rows) {
                    Row dataRow = sheet.createRow(r++);
                    for (int c = 0; c < headerList.size(); c++) {
                        Cell cell = dataRow.createCell(c);
                        Object val = row.get(headerList.get(c));
                        cell.setCellValue(val == null ? "" : String.valueOf(val));
                    }
                }
                
                // Ajustar largura das colunas automaticamente conforme conteúdo
                for (int c = 0; c < headerList.size(); c++) {
                    sheet.autoSizeColumn(c, true);
                }
            }
            
            wb.write(out);
            return out.toByteArray();
        }
    }
}
