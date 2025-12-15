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

    // Combina JSONs locais em um único payload
    @PostMapping("/xlsx/auto")
    public ResponseEntity<byte[]> exportAuto(
            @org.springframework.web.bind.annotation.RequestParam(value = "procPath", required = false) String procPath,
            @org.springframework.web.bind.annotation.RequestParam(value = "riskPath", required = false) String riskPath,
            @org.springframework.web.bind.annotation.RequestParam(value = "eventoPath", required = false) String eventoPath,
            @org.springframework.web.bind.annotation.RequestParam(value = "avaliacaoPath", required = false) String avaliacaoPathParam,
            @org.springframework.web.bind.annotation.RequestParam(value = "atividadePath", required = false) String atividadePathParam,
            @org.springframework.web.bind.annotation.RequestParam(value = "ocorrenciaPath", required = false) String ocorrenciaPathParam
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Caminhos padrão se não informados
        Path processosPath = Path.of(procPath == null || procPath.isBlank() ? "json/01-processo-controller.json" : procPath);
        Path riscosPath = Path.of(riskPath == null || riskPath.isBlank() ? "json/04-resposta-risco-controller.json" : riskPath);
        Path eventosPath = Path.of(eventoPath == null || eventoPath.isBlank() ? "json/02-risco-controller.json" : eventoPath);
        Path avaliacaoPath = Path.of(avaliacaoPathParam == null || avaliacaoPathParam.isBlank() ? "json/03.avalicao-risco-controle-controller.json" : avaliacaoPathParam);
        Path atividadePath = Path.of(atividadePathParam == null || atividadePathParam.isBlank() ? "json/05-atividade-controle-controller.json" : atividadePathParam);
        Path ocorrenciaPath = Path.of(ocorrenciaPathParam == null || ocorrenciaPathParam.isBlank() ? "json/ocorrencia-risco-controller.json" : ocorrenciaPathParam);

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
        
        // Ordem correta: ETAPA 1 → ETAPA 2 → ETAPA 3 → ETAPA 4
        allSheets.putAll(DadosProcessoTransformer.transform(processos));
        
        // ETAPA 2: Incluir eventos/riscos se arquivo existir
        if (Files.exists(eventosPath)) {
            String eventosJson = Files.readString(eventosPath, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> eventos = mapper.readValue(eventosJson, Map.class);
            allSheets.putAll(IdentificacaoEventosTransformer.transform(eventos));
        }
        
        // ETAPA 3: Incluir avaliação de riscos se arquivo existir
        if (Files.exists(avaliacaoPath)) {
            String avaliacaoJson = Files.readString(avaliacaoPath, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> avaliacao = mapper.readValue(avaliacaoJson, Map.class);
            allSheets.putAll(AvaliacaoRiscosTransformer.transform(avaliacao));
        }
        
        allSheets.putAll(RespostaRiscosTransformer.transform(respostasRisco));
        
        // ETAPA 5: Incluir atividades de controle se arquivo existir
        if (Files.exists(atividadePath)) {
            String atividadeJson = Files.readString(atividadePath, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> atividade = mapper.readValue(atividadeJson, Map.class);
            allSheets.putAll(AtividadeControleTransformer.transform(atividade));
        }
        
        // OCORRÊNCIAS: Incluir ocorrências de risco se arquivo existir
        if (Files.exists(ocorrenciaPath)) {
            String ocorrenciaJson = Files.readString(ocorrenciaPath, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> ocorrencia = mapper.readValue(ocorrenciaJson, Map.class);
            allSheets.putAll(OcorrenciaRiscoTransformer.transform(ocorrencia));
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
