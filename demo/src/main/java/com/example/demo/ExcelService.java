package com.example.demo;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
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

                int r = 0;
                
                // ==================== ESTILIZAÇÃO DOS CABEÇALHOS ====================
                // Cria um estilo personalizado para os cabeçalhos
                CellStyle headerStyle = wb.createCellStyle();
                // Cria uma fonte em negrito
                Font headerFont = wb.createFont();
                headerFont.setBold(true);
                // Aplica a fonte ao estilo
                headerStyle.setFont(headerFont);
                

                // Linha de cabeçalho
                Row headerRow = sheet.createRow(r++);
                for (int c = 0; c < headerList.size(); c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(headerList.get(c));
                    cell.setCellStyle(headerStyle); // Aplica o estilo ao cabeçalho
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
