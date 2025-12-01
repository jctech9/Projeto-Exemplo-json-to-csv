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
        // Usamos LinkedHashSet para tentar manter uma ordem de inserção razoável
        Set<String> headers = new LinkedHashSet<>();
        for (Map<String, Object> map : jsonList) {
            headers.addAll(map.keySet());
        }

        StringBuilder csvContent = new StringBuilder();

        // 2. Escrever o Cabeçalho
        csvContent.append(String.join(",", headers)).append("\n");

        

        return csvContent.toString().getBytes();
    }

   
}