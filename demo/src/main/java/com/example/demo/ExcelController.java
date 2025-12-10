package com.example.demo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.ProcessoDataTransformer;
import com.example.demo.RespostaRiscoDataTransformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/export")
public class ExcelController {

    private final ExcelService excelService;

    public ExcelController(ExcelService excelService) {
        this.excelService = excelService;
    }


    @PostMapping("/xlsx")
    public ResponseEntity<byte[]> exportToXlsx(@RequestBody Map<String, Object> payload) throws IOException {
        Map<String, List<Map<String, Object>>> allSheets = new LinkedHashMap<>();
        
        // Detecta automaticamente se é payload combinado ou JSON direto
        if (payload.containsKey("processos") || payload.containsKey("respostasRisco")) {
            // Payload combinado
            @SuppressWarnings("unchecked")
            Map<String, Object> processos = (Map<String, Object>) payload.get("processos");
            @SuppressWarnings("unchecked")
            Map<String, Object> riscos = (Map<String, Object>) payload.get("respostasRisco");
            
            if (processos != null) {
                allSheets.putAll(ProcessoDataTransformer.transform(processos));
            }
            if (riscos != null) {
                allSheets.putAll(RespostaRiscoDataTransformer.transform(riscos));
            }
        } else if (payload.containsKey("content")) {
            // JSON direto - detecta tipo pelo conteúdo
            Object content = payload.get("content");
            if (content instanceof List && !((List<?>) content).isEmpty()) {
                Object firstItem = ((List<?>) content).get(0);
                if (firstItem instanceof Map) {
                    Map<?, ?> item = (Map<?, ?>) firstItem;
                    
                    // Se tem campo "risco" é resposta a riscos, senão é processo
                    if (item.containsKey("risco")) {
                        allSheets.putAll(RespostaRiscoDataTransformer.transform(payload));
                    } else {
                        allSheets.putAll(ProcessoDataTransformer.transform(payload));
                    }
                }
            }
        }
        
        // Se nenhuma aba foi detectada, retorna 204 (sem conteúdo)
        if (allSheets.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        byte[] bytes = excelService.generateXlsx(allSheets);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dados.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(bytes.length)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // Endpoint automático: combina os dois arquivos JSON locais em um único payload
    @PostMapping("/xlsx/auto")
    public ResponseEntity<byte[]> exportAuto(
            @org.springframework.web.bind.annotation.RequestParam(value = "procPath", required = false) String procPath,
            @org.springframework.web.bind.annotation.RequestParam(value = "riskPath", required = false) String riskPath
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Caminhos padrão se não informados
        Path processosPath = Path.of(procPath == null || procPath.isBlank() ? "json/processo-controller.json" : procPath);
        Path riscosPath = Path.of(riskPath == null || riskPath.isBlank() ? "json/resposta-risco-controller.json" : riskPath);

        if (!Files.exists(processosPath)) {
            return ResponseEntity.status(404).body(("Arquivo de processos não encontrado: " + processosPath).getBytes(StandardCharsets.UTF_8));
        }
        if (!Files.exists(riscosPath)) {
            return ResponseEntity.status(404).body(("Arquivo de riscos não encontrado: " + riscosPath).getBytes(StandardCharsets.UTF_8));
        }

        String processosJson = Files.readString(processosPath, StandardCharsets.UTF_8);
        String riscosJson = Files.readString(riscosPath, StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        Map<String, Object> processos = mapper.readValue(processosJson, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> respostasRisco = mapper.readValue(riscosJson, Map.class);

        Map<String, List<Map<String, Object>>> allSheets = new LinkedHashMap<>();
        allSheets.putAll(ProcessoDataTransformer.transform(processos));
        allSheets.putAll(RespostaRiscoDataTransformer.transform(respostasRisco));

        if (allSheets.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        byte[] bytes = excelService.generateXlsx(allSheets);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dados.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(bytes.length)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
