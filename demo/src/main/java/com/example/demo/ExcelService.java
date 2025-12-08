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

    /**
     * Gera um arquivo XLSX com múltiplas abas.
     * Espera um mapa onde a chave é o nome da aba 
     * e o valor é a lista de objetos (mapas) daquela aba.
     */
    public byte[] generateXlsx(Map<String, List<Map<String, Object>>> etapas) throws IOException {
        // O try-with-resources fechamento automático do Workbook e da Stream para evitar vazamento de memória
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (etapas == null || etapas.isEmpty()) {
                // Cria uma planilha vazia padrão
                wb.createSheet("Planilha");
                wb.write(out);
                return out.toByteArray();
            }
            //2. Itera sobre cada chave do mapa principal para criar as abas
            for (Map.Entry<String, List<Map<String, Object>>> entry : etapas.entrySet()) {
                String sheetName = entry.getKey();
                List<Map<String, Object>> rows = entry.getValue();
                
                // Garante um nome válido (fallback) e cria a nova aba no workbook
                if (sheetName == null || sheetName.isBlank()) sheetName = "Sheet";
                XSSFSheet sheet = wb.createSheet(sheetName);

                // Cabeçalho: união das chaves preservando ordem de inserção
                LinkedHashSet<String> headers = new LinkedHashSet<>();
                if (rows != null) {
                    for (Map<String, Object> row : rows) {
                        if (row != null) headers.addAll(row.keySet());
                    }
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
                if (rows != null) {
                    for (Map<String, Object> row : rows) {
                        Row dataRow = sheet.createRow(r++);
                        for (int c = 0; c < headerList.size(); c++) {
                            String key = headerList.get(c);
                            Object val = row == null ? null : row.get(key);
                            Cell cell = dataRow.createCell(c);
                            cell.setCellValue(val == null ? "" : String.valueOf(val));
                        }
                    }
                }

                // Ajuste largura das colunas com largura fixa para melhor performance
                for (int c = 0; c < headerList.size(); c++) {
                    sheet.setColumnWidth(c, 5000); // ~50px
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }
}
