package com.example.demo;

import org.apache.poi.ss.usermodel.Cell;
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
                // Linha de cabeçalho
                Row headerRow = sheet.createRow(r++);
                for (int c = 0; c < headerList.size(); c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(headerList.get(c));
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
                
                // Ajustar largura das colunas
                for (int c = 0; c < headerList.size(); c++) {
                    sheet.setColumnWidth(c, 9000);
                }
            }
            
            wb.write(out);
            return out.toByteArray();
        }
    }
}
