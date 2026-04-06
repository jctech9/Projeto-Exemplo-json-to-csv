package com.example.demo;

import com.example.demo.service.ExcelService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.example.demo.transformers.DadosProcessoTransformer;
import com.example.demo.transformers.RespostaRiscosTransformer;
import com.example.demo.transformers.IdentificacaoEventosTransformer;
import com.example.demo.transformers.AvaliacaoRiscosTransformer;
import com.example.demo.transformers.AtividadeControleTransformer;
import com.example.demo.transformers.OcorrenciaRiscoTransformer;
import com.example.demo.transformers.RefResolver;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/export")
public class ExcelController {

    private final ExcelService excelService;

    public ExcelController(ExcelService excelService) {
        this.excelService = excelService;
    }


    @PostMapping("/xlsx")
    public ResponseEntity<byte[]> exportToXlsx(@RequestBody Map<String, Object> payload) throws IOException {
        // Hidrata @ref antes da transformação
        RefResolver.resolve(payload);

        Map<String, List<Map<String, Object>>> allSheets = new LinkedHashMap<>();
        try {
            // Detecta payload combinado ou JSON direto
            if (isCombinedPayload(payload)) {
                // Garante consistencia e alinhamento por risco.id antes de gerar as abas.
                transformCombinedPayload(payload, allSheets);
            } else if (payload.containsKey("content")) {
                // JSON direto: detecta tipo pelo conteudo
                Object content = payload.get("content");
                if (content instanceof List && !((List<?>) content).isEmpty()) {
                    Object firstItem = ((List<?>) content).get(0);
                    if (firstItem instanceof Map) {
                        Map<?, ?> item = (Map<?, ?>) firstItem;

                        // Heuristica: ocorrencia, atividade, avaliacao, resposta, evento ou processo
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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        }

        // Se nenhuma aba foi detectada, retorna 204 (sem conteudo)
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

    // Endpoint GET para abrir no navegador: /export/xlsx/api?id=63
    @GetMapping("/xlsx/api")
    public ResponseEntity<byte[]> exportFromApiGet(
            @org.springframework.web.bind.annotation.RequestParam(value = "baseUrl", defaultValue = "http://localhost:8090") String baseUrl,
            @org.springframework.web.bind.annotation.RequestParam(value = "id", required = false) Integer id
    ) throws IOException {
        Map<String, Integer> body = null;
        if (id != null) {
            body = new java.util.HashMap<>();
            body.put("id", id);
        }
        return exportFromApi(baseUrl, body);
    }

    // Endpoint para buscar dados via API externa e gerar Excel com ID fornecido
    @PostMapping("/xlsx/api")
    public ResponseEntity<byte[]> exportFromApi(
            @org.springframework.web.bind.annotation.RequestParam(value = "baseUrl", defaultValue = "http://localhost:8090") String baseUrl,
            @RequestBody(required = false) Map<String, Integer> body
    ) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, List<Map<String, Object>>> allSheets = new LinkedHashMap<>();
        
        // Se o ID for passado no corpo, usa ele. Sem ID, não aplica filtro por processo.
        java.util.concurrent.atomic.AtomicInteger mainProcessId = new java.util.concurrent.atomic.AtomicInteger(
            (body != null && body.containsKey("id")) ? body.get("id") : -1
        );
        
        try {
            // Se houver ID específico, filtra processos por esse ID.
            // Sem ID, mantém a lista completa de processos.
            addSheetIfAvailable(allSheets, restTemplate, baseUrl, "/processos", data -> {
                List<Map<String, Object>> content = getList(data);
                if (content != null && !content.isEmpty()) {
                    if (mainProcessId.get() != -1) {
                        // Filtra pelo ID fornecido no POST
                        List<Map<String, Object>> filteredInfo = new ArrayList<>();
                        for (Map<String, Object> p : content) {
                             if (checkId(p, mainProcessId.get(), new HashMap<>())) { // Cache vazio, check direto
                                 filteredInfo.add(p);
                                 break; // Assume 1 processo por ID
                             }
                        }
                        data.put("content", filteredInfo);
                    }
                }
                return DadosProcessoTransformer.transform(data);
            });
            
            // Riscos viram a lista canônica de referência entre as etapas.
            Set<Integer> riscoIdsDoProcesso = new LinkedHashSet<>();
            Map<Integer, String> riscoNomePorId = new LinkedHashMap<>();
            Map<String, Object> riscosData = fetchEndpointData(restTemplate, baseUrl, "/riscos");
            if (riscosData != null) {
                List<Map<String, Object>> riscosFiltrados = filterByProcessIfNeeded(riscosData, mainProcessId.get());
                riscosData.put("content", riscosFiltrados);

                for (Map<String, Object> risco : riscosFiltrados) {
                    Object riscoId = risco.get("id");
                    if (riscoId instanceof Number) {
                        Integer id = ((Number) riscoId).intValue();
                        riscoIdsDoProcesso.add(id);
                        riscoNomePorId.put(id, String.valueOf(risco.getOrDefault("nome", "")));
                    }
                }

                allSheets.putAll(IdentificacaoEventosTransformer.transform(riscosData));
            }

            // ETAPA 3 alinhada pela ordem dos risco.id canônicos.
            Map<String, Object> avaliacoesData = fetchEndpointData(restTemplate, baseUrl, "/avaliacoesRiscoControle");
            if (avaliacoesData != null) {
                List<Map<String, Object>> content = filterByProcessIfNeeded(avaliacoesData, mainProcessId.get());
                avaliacoesData.put("content", alignByRiscoIds(content, riscoIdsDoProcesso));
                allSheets.putAll(AvaliacaoRiscosTransformer.transform(avaliacoesData));
            }

            // ETAPA 4 alinhada pela mesma chave de risco.
            Map<String, Object> respostasData = fetchEndpointData(restTemplate, baseUrl, "/respostasRisco");
            if (respostasData != null) {
                List<Map<String, Object>> content = filterByProcessIfNeeded(respostasData, mainProcessId.get());
                respostasData.put("content", alignByRiscoIds(content, riscoIdsDoProcesso));
                allSheets.putAll(RespostaRiscosTransformer.transform(respostasData));
            }

            // ETAPA 5 alinhada pela mesma chave de risco.
            Map<String, Object> atividadesData = fetchEndpointData(restTemplate, baseUrl, "/atividadeControles");
            if (atividadesData != null) {
                List<Map<String, Object>> content = filterByProcessIfNeeded(atividadesData, mainProcessId.get());
                atividadesData.put("content", alignByRiscoIds(content, riscoIdsDoProcesso));
                allSheets.putAll(AtividadeControleTransformer.transform(atividadesData));
            }
            
            // Ocorrências de Risco: busca por cada risco do processo usando /ocorrenciasRisco/risco/{riscoId}
            // Mesmo sem risco canônico (ex.: ID inválido), a aba deve existir no arquivo final.
            List<Map<String, Object>> todasOcorrencias = new ArrayList<>();
            if (!riscoIdsDoProcesso.isEmpty()) {
                for (Integer riscoId : riscoIdsDoProcesso) {
                    try {
                        String url = baseUrl + "/ocorrenciasRisco/risco/" + riscoId + "?page=0&size=999999";
                        @SuppressWarnings("unchecked")
                        Map<String, Object> ocData = restTemplate.getForObject(url, Map.class);
                        if (ocData != null) {
                            List<Map<String, Object>> ocContent = getList(ocData);
                            if (ocContent != null) {
                                String nomeRisco = riscoNomePorId.getOrDefault(riscoId, "");
                                for (Map<String, Object> ocorrencia : ocContent) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> risco = ocorrencia.get("risco") instanceof Map<?, ?> m
                                            ? (Map<String, Object>) m
                                            : new LinkedHashMap<>();

                                    if (risco.get("id") == null) {
                                        risco.put("id", riscoId);
                                    }
                                    // Mantem o nome do risco consistente com a lista canonica do processo.
                                    if (!nomeRisco.isBlank()) {
                                        risco.put("nome", nomeRisco);
                                    }
                                    ocorrencia.put("risco", risco);
                                }
                                todasOcorrencias.addAll(ocContent);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[EXPORT] Erro ao buscar ocorrências do risco " + riscoId + ": " + e.getMessage());
                    }
                }
            }

            List<Map<String, Object>> ocorrenciasAlinhadas = alignByRiscoIds(todasOcorrencias, riscoIdsDoProcesso);
            applyCanonicalRiscoNome(ocorrenciasAlinhadas, riscoNomePorId);

            Map<String, Object> ocPayload = new LinkedHashMap<>();
            ocPayload.put("content", ocorrenciasAlinhadas);
            // Hidrata @ref antes da transformação
            RefResolver.resolve(ocPayload);
            allSheets.putAll(OcorrenciaRiscoTransformer.transform(ocPayload));
            
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

    private boolean isCombinedPayload(Map<String, Object> payload) {
        return payload.containsKey("processos")
                || payload.containsKey("riscos")
                || payload.containsKey("avaliacoesRiscoControle")
                || payload.containsKey("respostasRisco")
                || payload.containsKey("atividadeControles")
                || payload.containsKey("ocorrenciasRisco");
    }

    private void transformCombinedPayload(
            Map<String, Object> payload,
            Map<String, List<Map<String, Object>>> allSheets
    ) {
        // Le cada secao conhecida do payload combinado.
        Map<String, Object> processosData = getPayloadSection(payload, "processos");
        Map<String, Object> riscosData = getPayloadSection(payload, "riscos");
        Map<String, Object> avaliacoesData = getPayloadSection(payload, "avaliacoesRiscoControle");
        Map<String, Object> respostasData = getPayloadSection(payload, "respostasRisco");
        Map<String, Object> atividadesData = getPayloadSection(payload, "atividadeControles");
        Map<String, Object> ocorrenciasData = getPayloadSection(payload, "ocorrenciasRisco");

        if (processosData != null) {
            allSheets.putAll(DadosProcessoTransformer.transform(processosData));
        }

        List<Map<String, Object>> riscosContent = getContentOrEmpty(riscosData);
        List<Map<String, Object>> avaliacoesContent = getContentOrEmpty(avaliacoesData);
        List<Map<String, Object>> respostasContent = getContentOrEmpty(respostasData);
        List<Map<String, Object>> atividadesContent = getContentOrEmpty(atividadesData);
        List<Map<String, Object>> ocorrenciasContent = getContentOrEmpty(ocorrenciasData);

        Set<Integer> canonicalRiscoIds = buildCanonicalRiscoIds(
                riscosContent,
                avaliacoesContent,
                respostasContent,
                atividadesContent,
                ocorrenciasContent
        );

        Map<Integer, String> riscoNomePorId = buildRiscoNomePorId(riscosContent);

        int colecoesComRisco = countNonEmptyCollections(
                riscosContent,
                avaliacoesContent,
                respostasContent,
                atividadesContent
        );
        if (colecoesComRisco > 1 && canonicalRiscoIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: nao foi possivel montar uma lista canonica de risco.id para alinhar as etapas."
            );
        }

        if (riscosData != null) {
            allSheets.putAll(IdentificacaoEventosTransformer.transform(riscosData));
        }

        if (avaliacoesData != null) {
            // Falha rapido se houver risco.id ausente, fora da base ou duplicado.
            validateStrictRiscoCollection("avaliacoesRiscoControle", avaliacoesContent, canonicalRiscoIds);
            avaliacoesData.put("content", alignByRiscoIds(avaliacoesContent, canonicalRiscoIds));
            allSheets.putAll(AvaliacaoRiscosTransformer.transform(avaliacoesData));
        }

        if (respostasData != null) {
            validateStrictRiscoCollection("respostasRisco", respostasContent, canonicalRiscoIds);
            respostasData.put("content", alignByRiscoIds(respostasContent, canonicalRiscoIds));
            allSheets.putAll(RespostaRiscosTransformer.transform(respostasData));
        }

        if (atividadesData != null) {
            validateStrictRiscoCollection("atividadeControles", atividadesContent, canonicalRiscoIds);
            atividadesData.put("content", alignByRiscoIds(atividadesContent, canonicalRiscoIds));
            allSheets.putAll(AtividadeControleTransformer.transform(atividadesData));
        }

        if (ocorrenciasData != null) {
            List<Map<String, Object>> ocorrenciasAlinhadas = alignByRiscoIds(ocorrenciasContent, canonicalRiscoIds);
            applyCanonicalRiscoNome(ocorrenciasAlinhadas, riscoNomePorId);
            ocorrenciasData.put("content", ocorrenciasAlinhadas);
            allSheets.putAll(OcorrenciaRiscoTransformer.transform(ocorrenciasData));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPayloadSection(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new IllegalArgumentException(
                "Inconsistencia no payload: a colecao '" + key + "' deve ser um objeto com campo 'content'."
        );
    }

    private List<Map<String, Object>> getContentOrEmpty(Map<String, Object> data) {
        if (data == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> content = getList(data);
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(content);
    }

    private Set<Integer> buildCanonicalRiscoIds(
            List<Map<String, Object>> riscosContent,
            List<Map<String, Object>> avaliacoesContent,
            List<Map<String, Object>> respostasContent,
            List<Map<String, Object>> atividadesContent,
            List<Map<String, Object>> ocorrenciasContent
    ) {
        if (riscosContent != null && !riscosContent.isEmpty()) {
            return extractCanonicalRiscoIdsFromRiscos(riscosContent);
        }

        Set<Integer> inferred = new LinkedHashSet<>();
        appendRiscoIds(inferred, avaliacoesContent);
        appendRiscoIds(inferred, respostasContent);
        appendRiscoIds(inferred, atividadesContent);
        appendRiscoIds(inferred, ocorrenciasContent);
        return inferred;
    }

    private Set<Integer> extractCanonicalRiscoIdsFromRiscos(List<Map<String, Object>> riscosContent) {
        Set<Integer> canonical = new LinkedHashSet<>();
        Set<Integer> duplicated = new LinkedHashSet<>();
        List<Integer> missingPositions = new ArrayList<>();

        for (int i = 0; i < riscosContent.size(); i++) {
            Integer riscoId = extractEntityId(riscosContent.get(i));
            if (riscoId == null) {
                missingPositions.add(i + 1);
                continue;
            }

            if (!canonical.add(riscoId)) {
                duplicated.add(riscoId);
            }
        }

        if (!missingPositions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: colecao 'riscos' possui itens sem id nas posicoes " + missingPositions + "."
            );
        }

        if (!duplicated.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: colecao 'riscos' possui ids duplicados " + duplicated + "."
            );
        }

        return canonical;
    }

    private void appendRiscoIds(Set<Integer> target, List<Map<String, Object>> content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        for (Map<String, Object> item : content) {
            Integer riscoId = extractRiscoId(item);
            if (riscoId != null) {
                target.add(riscoId);
            }
        }
    }

    private Map<Integer, String> buildRiscoNomePorId(List<Map<String, Object>> riscosContent) {
        Map<Integer, String> riscoNomePorId = new LinkedHashMap<>();
        if (riscosContent == null || riscosContent.isEmpty()) {
            return riscoNomePorId;
        }

        for (Map<String, Object> risco : riscosContent) {
            Integer riscoId = extractEntityId(risco);
            if (riscoId == null) {
                continue;
            }
            riscoNomePorId.put(riscoId, String.valueOf(risco.getOrDefault("nome", "")));
        }
        return riscoNomePorId;
    }

    private int countNonEmptyCollections(List<Map<String, Object>>... collections) {
        int count = 0;
        for (List<Map<String, Object>> collection : collections) {
            if (collection != null && !collection.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void validateStrictRiscoCollection(
            String collectionName,
            List<Map<String, Object>> content,
            Set<Integer> canonicalRiscoIds
    ) {
        if (content == null || content.isEmpty() || canonicalRiscoIds == null || canonicalRiscoIds.isEmpty()) {
            return;
        }

        List<Integer> missingRiscoIdPositions = new ArrayList<>();
        Set<Integer> outOfCanonical = new LinkedHashSet<>();
        Set<Integer> duplicated = new LinkedHashSet<>();
        Set<Integer> seen = new LinkedHashSet<>();

        for (int i = 0; i < content.size(); i++) {
            Integer riscoId = extractRiscoId(content.get(i));
            if (riscoId == null) {
                missingRiscoIdPositions.add(i + 1);
                continue;
            }

            if (!canonicalRiscoIds.contains(riscoId)) {
                outOfCanonical.add(riscoId);
            }

            if (!seen.add(riscoId)) {
                duplicated.add(riscoId);
            }
        }

        if (!missingRiscoIdPositions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: colecao '" + collectionName
                            + "' possui itens sem risco.id nas posicoes " + missingRiscoIdPositions + "."
            );
        }

        if (!outOfCanonical.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: colecao '" + collectionName
                            + "' possui risco.id fora da lista canonica " + outOfCanonical + "."
            );
        }

        if (!duplicated.isEmpty()) {
            throw new IllegalArgumentException(
                    "Inconsistencia no payload: colecao '" + collectionName
                            + "' possui risco.id duplicado " + duplicated + "."
            );
        }
    }

    private Integer extractEntityId(Map<String, Object> item) {
        if (item == null) {
            return null;
        }
        Object id = item.get("id");
        if (id instanceof Number) {
            return ((Number) id).intValue();
        }
        return null;
    }

    private void addSheetIfAvailable(Map<String, List<Map<String, Object>>> allSheets, RestTemplate restTemplate,
            String baseUrl, String endpoint,
            java.util.function.Function<Map<String, Object>, Map<String, List<Map<String, Object>>>> transformer) {
        try {
            String url = baseUrl + endpoint + "?page=0&size=999999";
            @SuppressWarnings("unchecked")
            Map<String, Object> data = restTemplate.getForObject(url, Map.class);
            if (data != null) {
                // Hidrata @ref antes da transformação
                RefResolver.resolve(data);
                allSheets.putAll(transformer.apply(data));
            }
        } catch (Exception e) {
            System.err.println("[EXPORT] Erro ao buscar " + endpoint + ": " + e.getMessage());
        }
    }
/////alinhamento por mapa de risco.id no fluxo de exportação/////////////
   
    // Busca um endpoint paginado e já resolve referências serializadas (@ref).
    private Map<String, Object> fetchEndpointData(RestTemplate restTemplate, String baseUrl, String endpoint) {
        try {
            String url = baseUrl + endpoint + "?page=0&size=999999";
            @SuppressWarnings("unchecked")
            Map<String, Object> data = restTemplate.getForObject(url, Map.class);
            if (data != null) {
                RefResolver.resolve(data);
            }
            return data;
        } catch (Exception e) {
            System.err.println("[EXPORT] Erro ao buscar " + endpoint + ": " + e.getMessage());
            return null;
        }
    }

    // Aplica filtro por processo quando informado; sem processo, retorna todos os itens.
    private List<Map<String, Object>> filterByProcessIfNeeded(Map<String, Object> data, int processId) {
        List<Map<String, Object>> content = getList(data);
        if (content == null) {
            return new ArrayList<>();
        }
        if (processId == -1) {
            return new ArrayList<>(content);
        }

        List<Map<String, Object>> filtered = new ArrayList<>();
        Map<String, Integer> refCache = new HashMap<>();
        for (Map<String, Object> item : content) {
            if (isRelatedToProcess(item, processId, refCache)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    @SuppressWarnings("unchecked")
    // Reordena uma coleção usando o conjunto canônico de risco.id.
    private List<Map<String, Object>> alignByRiscoIds(List<Map<String, Object>> content, Set<Integer> canonicalRiscoIds) {
        // Sem lista canônica, mantém o conteúdo original.
        if (canonicalRiscoIds == null || canonicalRiscoIds.isEmpty()) {
            if (content == null || content.isEmpty()) {
                return new ArrayList<>();
            }
            return new ArrayList<>(content);
        }

        // Com lista canônica, mesmo conteúdo vazio deve gerar placeholders para manter
        // o alinhamento de linhas entre as etapas e preservar fórmulas por linha.
        if (content == null || content.isEmpty()) {
            List<Map<String, Object>> onlyPlaceholders = new ArrayList<>();
            for (Integer riscoId : canonicalRiscoIds) {
                onlyPlaceholders.add(createPlaceholderByRiscoId(riscoId));
            }
            return onlyPlaceholders;
        }

        Map<Integer, List<Map<String, Object>>> byRiscoId = new LinkedHashMap<>();
        List<Map<String, Object>> withoutRiscoId = new ArrayList<>();

        for (Map<String, Object> item : content) {
            Integer riscoId = extractRiscoId(item);
            if (riscoId == null) {
                withoutRiscoId.add(item);
                continue;
            }
            byRiscoId.computeIfAbsent(riscoId, key -> new ArrayList<>()).add(item);
        }

        List<Map<String, Object>> aligned = new ArrayList<>();
        Set<Integer> includedRiscoIds = new LinkedHashSet<>();
        for (Integer riscoId : canonicalRiscoIds) {
            List<Map<String, Object>> items = byRiscoId.get(riscoId);
            if (items != null && !items.isEmpty()) {
                aligned.addAll(items);
                includedRiscoIds.add(riscoId);
            } else {
                // Mantem o mesmo numero de linhas da lista canonica para evitar deslocamento
                // de formulas que referenciam por indice de linha entre etapas.
                aligned.add(createPlaceholderByRiscoId(riscoId));
            }
        }

        // Adiciona itens de risco.id que não estavam na lista canônica no final, para não perder dados.
        for (Map.Entry<Integer, List<Map<String, Object>>> entry : byRiscoId.entrySet()) {
            if (!includedRiscoIds.contains(entry.getKey())) {
                aligned.addAll(entry.getValue());
            }
        }

        // Mantém itens sem risco.id no final para não perder dados de endpoints heterogêneos.
        aligned.addAll(withoutRiscoId);
        return aligned;
    }

    private Map<String, Object> createPlaceholderByRiscoId(Integer riscoId) {
        Map<String, Object> placeholder = new LinkedHashMap<>();
        Map<String, Object> risco = new LinkedHashMap<>();
        risco.put("id", riscoId);
        placeholder.put("risco", risco);
        return placeholder;
    }

    @SuppressWarnings("unchecked")
    private void applyCanonicalRiscoNome(List<Map<String, Object>> items, Map<Integer, String> riscoNomePorId) {
        if (items == null || items.isEmpty() || riscoNomePorId == null || riscoNomePorId.isEmpty()) {
            return;
        }

        for (Map<String, Object> item : items) {
            if (item == null) {
                continue;
            }

            Integer riscoId = extractRiscoId(item);
            if (riscoId == null) {
                continue;
            }

            String nomeCanonico = riscoNomePorId.getOrDefault(riscoId, "");
            if (nomeCanonico.isBlank()) {
                continue;
            }

            Object riscoObj = item.get("risco");
            Map<String, Object> risco;
            if (riscoObj instanceof Map<?, ?> riscoMap) {
                risco = (Map<String, Object>) riscoMap;
            } else {
                risco = new LinkedHashMap<>();
                risco.put("id", riscoId);
                item.put("risco", risco);
            }
            risco.put("nome", nomeCanonico);
        }
    }

    // Extrai apenas risco.id da estrutura aninhada (item.risco.id).
    // Evita fallback para item.id, que pode representar IDs de outras entidades.
    private Integer extractRiscoId(Map<String, Object> item) {
        if (item == null) {
            return null;
        }

        Object riscoObj = item.get("risco");
        if (riscoObj instanceof Map<?, ?> riscoMap) {
            Object nestedId = riscoMap.get("id");
            if (nestedId instanceof Number) {
                return ((Number) nestedId).intValue();
            }
        }

        return null;
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

        // Se o item não tem link direto com processo nem risco.processo,
        // inclui o item (ex: ocorrências de risco que não possuem essa associação)
        if (procObj == null && riscoObj == null) {
            return false;
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
