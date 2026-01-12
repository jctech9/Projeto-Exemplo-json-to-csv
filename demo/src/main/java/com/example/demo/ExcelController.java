package com.example.demo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.demo.transformers.DadosProcessoTransformer;
import com.example.demo.transformers.RespostaRiscosTransformer;
import com.example.demo.transformers.IdentificacaoEventosTransformer;
import com.example.demo.transformers.AvaliacaoRiscosTransformer;
import com.example.demo.transformers.AtividadeControleTransformer;
import com.example.demo.transformers.OcorrenciaRiscoTransformer;

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
        
        // Detecta payload combinado ou JSON direto
        if (payload.containsKey("processos") || payload.containsKey("respostasRisco")) {
            // Payload combinado
            @SuppressWarnings("unchecked")
            Map<String, Object> processos = (Map<String, Object>) payload.get("processos");
            @SuppressWarnings("unchecked")
            Map<String, Object> riscos = (Map<String, Object>) payload.get("respostasRisco");
            
            if (processos != null) {
                allSheets.putAll(DadosProcessoTransformer.transform(processos));
            }
            if (riscos != null) {
                allSheets.putAll(RespostaRiscosTransformer.transform(riscos));
            }
        } else if (payload.containsKey("content")) {
            // JSON direto: detecta tipo pelo conteúdo
            Object content = payload.get("content");
            if (content instanceof List && !((List<?>) content).isEmpty()) {
                Object firstItem = ((List<?>) content).get(0);
                if (firstItem instanceof Map) {
                    Map<?, ?> item = (Map<?, ?>) firstItem;
                    
                    // Heurística: ocorrência, atividade, avaliação, resposta, evento ou processo
                    if (item.containsKey("dataOcorrencia") && item.containsKey("descricao")) {
                        allSheets.putAll(OcorrenciaRiscoTransformer.transform(payload));
                    } else if (item.containsKey("statusImplementacao") && item.containsKey("risco")) {
                        allSheets.putAll(AtividadeControleTransformer.transform(payload));
                    } else if (item.containsKey("probabilidade") && item.containsKey("risco")) {
                        allSheets.putAll(AvaliacaoRiscosTransformer.transform(payload));
                    } else if (item.containsKey("risco")) {
                        allSheets.putAll(RespostaRiscosTransformer.transform(payload));
                    } else if (item.containsKey("faseProcesso")) {
                        allSheets.putAll(IdentificacaoEventosTransformer.transform(payload));
                    } else {
                        allSheets.putAll(DadosProcessoTransformer.transform(payload));
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

    // Busca JSONs via API e gera Excel
    @PostMapping("/xlsx/auto")
    public ResponseEntity<byte[]> exportAuto(
            @org.springframework.web.bind.annotation.RequestParam(value = "baseUrl", defaultValue = "http://localhost:8090") String baseUrl
    ) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, List<Map<String, Object>>> allSheets = new LinkedHashMap<>();
        
        try {
            // Ordem correta: ETAPA 1 -> ETAPA 2 -> ETAPA 3 -> ETAPA 4 -> ETAPA 5 -> OCORRENCIAS
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/processos", DadosProcessoTransformer::transform);
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/riscos", IdentificacaoEventosTransformer::transform);
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/avaliacoesRiscoControle", AvaliacaoRiscosTransformer::transform);
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/respostasRisco", RespostaRiscosTransformer::transform);
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/atividadeControles", AtividadeControleTransformer::transform);
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/ocorrenciasRisco", OcorrenciaRiscoTransformer::transform);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("Erro ao buscar dados da API: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
        
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

    // Endpoint para buscar dados via API externa e gerar Excel
    @GetMapping("/xlsx/api")
    public ResponseEntity<byte[]> exportFromApi(
            @org.springframework.web.bind.annotation.RequestParam(value = "baseUrl", defaultValue = "http://localhost:8090") String baseUrl
    ) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, List<Map<String, Object>>> allSheets = new LinkedHashMap<>();
        
        try {
            // Buscar dados das ETAPAs
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/processos", DadosProcessoTransformer::transform);
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/riscos", IdentificacaoEventosTransformer::transform);
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/avaliacoesRiscoControle", AvaliacaoRiscosTransformer::transform);
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/respostasRisco", RespostaRiscosTransformer::transform);
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/atividadeControles", AtividadeControleTransformer::transform);
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/ocorrenciasRisco", OcorrenciaRiscoTransformer::transform);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("Erro ao buscar dados da API: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
        
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

    private void addSheetIfAvailable(Map<String, List<Map<String, Object>>> allSheets, RestTemplate restTemplate, 
            String baseUrl, String endpoint, java.util.function.Function<Map<String, Object>, Map<String, List<Map<String, Object>>>> transformer) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = restTemplate.getForObject(baseUrl + endpoint, Map.class);
            if (data != null) {
                allSheets.putAll(transformer.apply(data));
            }
        } catch (Exception e) {
            // Ignora se endpoint não existir
        }
    }
}
