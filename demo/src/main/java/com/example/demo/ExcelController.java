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
import java.nio.file.Paths;
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

    // Busca JSONs localmente da pasta json/ e gera Excel
    @PostMapping("/xlsx/auto")
    public ResponseEntity<byte[]> exportAuto() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, List<Map<String, Object>>> allSheets = new LinkedHashMap<>();
        java.util.concurrent.atomic.AtomicInteger mainProcessId = new java.util.concurrent.atomic.AtomicInteger(-1);
        
        try {
            // Define o caminho da pasta json (relativo ao projeto)
            Path jsonFolder = Paths.get("json");
            
            // Ordem correta: ETAPA 1 -> ETAPA 2 -> ETAPA 3 -> ETAPA 4 -> ETAPA 5 -> OCORRENCIAS
            
            // ETAPA 1: Processamento especial para capturar o ID do primeiro processo
            addSheetIfFileExists(allSheets, objectMapper, jsonFolder, "01-processo-controller.json", data -> {
                List<Map<String, Object>> content = getList(data);
                if (content != null && !content.isEmpty()) {
                    Map<String, Object> first = content.get(0);
                    Object id = first.get("id");
                    if (id instanceof Number) {
                        mainProcessId.set(((Number) id).intValue());
                    }
                    data.put("content", java.util.Collections.singletonList(first));
                }
                return DadosProcessoTransformer.transform(data);
            });

            // Demais etapas: Aplica filtro baseado no ID capturado
            addSheetIfFileExists(allSheets, objectMapper, jsonFolder, "02-risco-controller.json", filterByProcess(IdentificacaoEventosTransformer::transform, mainProcessId));
            addSheetIfFileExists(allSheets, objectMapper, jsonFolder, "03.avalicao-risco-controle-controller.json", filterByProcess(AvaliacaoRiscosTransformer::transform, mainProcessId));
            addSheetIfFileExists(allSheets, objectMapper, jsonFolder, "04-resposta-risco-controller.json", filterByProcess(RespostaRiscosTransformer::transform, mainProcessId));
            addSheetIfFileExists(allSheets, objectMapper, jsonFolder, "05-atividade-controle-controller.json", filterByProcess(AtividadeControleTransformer::transform, mainProcessId));
            addSheetIfFileExists(allSheets, objectMapper, jsonFolder, "ocorrencia-risco-controller.json", filterByProcess(OcorrenciaRiscoTransformer::transform, mainProcessId));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("Erro ao ler arquivos JSON locais: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
        // Se nenhuma aba foi detectada, retorna 204 (sem conteúdo)
        if (allSheets.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        // Gerar Excel
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
        java.util.concurrent.atomic.AtomicInteger mainProcessId = new java.util.concurrent.atomic.AtomicInteger(-1);
        
        try {
            // Buscar dados das ETAPAs
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/processos", data -> {
                List<Map<String, Object>> content = getList(data);
                if (content != null && !content.isEmpty()) {
                    Map<String, Object> first = content.get(0);
                    Object id = first.get("id");
                    if (id instanceof Number) {
                        mainProcessId.set(((Number) id).intValue());
                    }
                    data.put("content", java.util.Collections.singletonList(first));
                }
                return DadosProcessoTransformer.transform(data);
            });
            
            // Demais etapas filtradas
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/riscos", filterByProcess(IdentificacaoEventosTransformer::transform, mainProcessId));
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/avaliacoesRiscoControle", filterByProcess(AvaliacaoRiscosTransformer::transform, mainProcessId));
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/respostasRisco", filterByProcess(RespostaRiscosTransformer::transform, mainProcessId));
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/atividadeControles", filterByProcess(AtividadeControleTransformer::transform, mainProcessId));
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/ocorrenciasRisco", filterByProcess(OcorrenciaRiscoTransformer::transform, mainProcessId));
            
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

    private void addSheetIfFileExists(Map<String, List<Map<String, Object>>> allSheets, ObjectMapper objectMapper,
            Path jsonFolder, String filename, java.util.function.Function<Map<String, Object>, Map<String, List<Map<String, Object>>>> transformer) {
        try {
            Path filePath = jsonFolder.resolve(filename);
            if (Files.exists(filePath)) {
                String jsonContent = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(jsonContent, Map.class);
                if (data != null) {
                    allSheets.putAll(transformer.apply(data));
                }
            }
        } catch (Exception e) {
            // Ignora se arquivo não existir
        }
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

    // --- Helpers de filtro ---

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> data) {
        if (data.containsKey("content")) {
            return (List<Map<String, Object>>) data.get("content");
        }
        return null;
    }

    private java.util.function.Function<Map<String, Object>, Map<String, List<Map<String, Object>>>> filterByProcess(
            java.util.function.Function<Map<String, Object>, Map<String, List<Map<String, Object>>>> original,
            java.util.concurrent.atomic.AtomicInteger processId) {
        return data -> {
            if (processId.get() != -1) {
                List<Map<String, Object>> content = getList(data);
                if (content != null) {
                    List<Map<String, Object>> filtered = new java.util.ArrayList<>();
                    Map<String, Integer> refCache = new java.util.HashMap<>();
                    
                    for (Map<String, Object> item : content) {
                        if (isRelatedToProcess(item, processId.get(), refCache)) {
                            filtered.add(item);
                        }
                    }
                    data.put("content", filtered);
                }
            }
            return original.apply(data);
        };
    }

    @SuppressWarnings("unchecked")
    private boolean isRelatedToProcess(Map<String, Object> item, int pid, Map<String, Integer> refCache) {
        // Cacheia @id do próprio item se existir (útil se o item for referenciado depois, embora aqui filtramos itens 'raiz')
        checkId(item, -1, refCache); 
        
        // Link direto (processo.id)
        Object procObj = item.get("processo");
        if (procObj instanceof Map) {
            Map<String, Object> proc = (Map<String, Object>) procObj;
            if (checkId(proc, pid, refCache)) return true;
        }
        
        // Link via risco (risco.processo.id)
        Object riscoObj = item.get("risco");
        if (riscoObj instanceof Map) {
            Map<String, Object> risco = (Map<String, Object>) riscoObj;
            // Tenta cachear o ID do risco também, caso ajude em algo
            checkId(risco, -1, refCache);
            
            Object procViaRisco = risco.get("processo");
            if (procViaRisco instanceof Map) {
                 Map<String, Object> proc = (Map<String, Object>) procViaRisco;
                 if (checkId(proc, pid, refCache)) return true;
            }
        }
        return false;
    }

    private boolean checkId(Map<String, Object> obj, int pid, Map<String, Integer> refCache) {
        if (obj == null) return false;
        
        Object idVal = obj.get("id");
        Object atId = obj.get("@id");
        Object atRef = obj.get("@ref");

        // 1. Se tem ID, verifica e armazena no cache se tiver @id
        if (idVal instanceof Number) {
            int currentId = ((Number) idVal).intValue();
            if (atId != null) {
                refCache.put(atId.toString(), currentId);
            }
            return currentId == pid;
        }

        // 2. Se é Referência (@ref), busca no cache
        if (atRef != null && refCache.containsKey(atRef.toString())) {
            int cachedId = refCache.get(atRef.toString());
            return cachedId == pid;
        }

        return false;
    }
}
