package com.example.demo;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CsvService {

    public byte[] generateCsv(List<Map<String, Object>> jsonList) {
        if (jsonList == null || jsonList.isEmpty()) {
            return new byte[0];
        }

        // 1. Identificar todas as chaves únicas (Cabeçalho)
        Set<String> headers = new LinkedHashSet<>();
        for (Map<String, Object> map : jsonList) {
            headers.addAll(map.keySet());
        }

        StringBuilder csvContent = new StringBuilder();

        // 2. Escrever Cabeçalho
        csvContent.append(String.join(",", headers)).append("\n");

        
        // 3. Escrever Linhas de Dados
        for (Map<String, Object> row : jsonList) {
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                Object value = row.get(header);
                String stringValue = (value != null) ? String.valueOf(value) : "";
                values.add(escapeSpecialCharacters(stringValue));
            }
            csvContent.append(String.join(",", values)).append("\n");
        }

        return csvContent.toString().getBytes();
    }

        //Tratar caracteres especiais no CSV 
        private String escapeSpecialCharacters(String data) {
            String escapedData = data.replaceAll("\\R", " "); // Remove quebras de linha no meio do dado
            if (data.contains(",") || data.contains("\"") || data.contains("'")) {
                data = data.replace("\"", "\"\""); // Escapa aspas duplas
                escapedData = "\"" + data + "\"";
            }
            return escapedData;
        }
}
