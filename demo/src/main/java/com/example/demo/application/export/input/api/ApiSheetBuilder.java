package com.example.demo.application.export.input.api;

import com.example.demo.domain.risco.RiscoAlignmentService;
import com.example.demo.domain.risco.RiscoValidationService;

import com.example.demo.transformers.AtividadeControleTransformer;
import com.example.demo.transformers.AvaliacaoRiscosTransformer;
import com.example.demo.transformers.DadosProcessoTransformer;
import com.example.demo.transformers.IdentificacaoEventosTransformer;
import com.example.demo.transformers.OcorrenciaRiscoTransformer;
import com.example.demo.transformers.RefResolver;
import com.example.demo.transformers.RespostaRiscosTransformer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
// Consulta endpoints da API e monta as abas com alinhamento por risco.
public class ApiSheetBuilder {

    private final RiscoAlignmentService riscoAlignmentService;
    private final RiscoValidationService riscoValidationService;
    private final ApiHttpClient apiHttpClient;

    public ApiSheetBuilder(
            RiscoAlignmentService riscoAlignmentService,
            RiscoValidationService riscoValidationService,
            ApiHttpClient apiHttpClient
    ) {
        this.riscoAlignmentService = riscoAlignmentService;
        this.riscoValidationService = riscoValidationService;
        this.apiHttpClient = apiHttpClient;
    }

    public Map<String, List<Map<String, Object>>> buildSheetsFromApi(String baseUrl, Map<String, Integer> body) {
        Map<String, List<Map<String, Object>>> allSheets = new LinkedHashMap<>();

        Integer requestId = (body != null && body.containsKey("id")) ? body.get("id") : null;
        int mainProcessId = requestId != null ? requestId : -1;

        addSheetIfAvailable(allSheets, baseUrl, "/processos", data -> {
            List<Map<String, Object>> content = getList(data);
            if (content != null && !content.isEmpty()) {
                if (mainProcessId != -1) {
                    List<Map<String, Object>> filteredInfo = new ArrayList<>();
                    for (Map<String, Object> p : content) {
                        if (checkId(p, mainProcessId, new HashMap<>())) {
                            filteredInfo.add(p);
                            break;
                        }
                    }
                    data.put("content", filteredInfo);
                }
            }
            return DadosProcessoTransformer.transform(data);
        });

        Set<Integer> riscoIdsDoProcesso = new LinkedHashSet<>();
        Map<Integer, String> riscoNomePorId = new LinkedHashMap<>();
        Map<String, Object> riscosData = fetchEndpointData(baseUrl, "/riscos");
        if (riscosData != null) {
            List<Map<String, Object>> riscosFiltrados = filterByProcessIfNeeded(riscosData, mainProcessId);
            riscosData.put("content", riscosFiltrados);

            for (Map<String, Object> risco : riscosFiltrados) {
                Integer id = parseIntegerId(risco.get("id"));
                if (id != null) {
                    riscoIdsDoProcesso.add(id);
                    riscoNomePorId.put(id, String.valueOf(risco.getOrDefault("nome", "")));
                }
            }

            allSheets.putAll(IdentificacaoEventosTransformer.transform(riscosData));
        }

        Map<String, Object> avaliacoesData = fetchEndpointData(baseUrl, "/avaliacoesRiscoControle");
        if (avaliacoesData != null) {
            List<Map<String, Object>> content = filterByProcessIfNeeded(avaliacoesData, mainProcessId);
            riscoValidationService.validateStrictRiscoCollection("avaliacoesRiscoControle", content, riscoIdsDoProcesso);
            avaliacoesData.put("content", riscoAlignmentService.alignByRiscoIds(content, riscoIdsDoProcesso));
            allSheets.putAll(AvaliacaoRiscosTransformer.transform(avaliacoesData));
        }

        Map<String, Object> respostasData = fetchEndpointData(baseUrl, "/respostasRisco");
        if (respostasData != null) {
            List<Map<String, Object>> content = filterByProcessIfNeeded(respostasData, mainProcessId);
            riscoValidationService.validateStrictRiscoCollection("respostasRisco", content, riscoIdsDoProcesso);
            respostasData.put("content", riscoAlignmentService.alignByRiscoIds(content, riscoIdsDoProcesso));
            allSheets.putAll(RespostaRiscosTransformer.transform(respostasData));
        }

        Map<String, Object> atividadesData = fetchEndpointData(baseUrl, "/atividadeControles");
        if (atividadesData != null) {
            List<Map<String, Object>> content = filterByProcessIfNeeded(atividadesData, mainProcessId);
            riscoValidationService.validateStrictRiscoCollection("atividadeControles", content, riscoIdsDoProcesso);
            atividadesData.put("content", riscoAlignmentService.alignByRiscoIds(content, riscoIdsDoProcesso));
            allSheets.putAll(AtividadeControleTransformer.transform(atividadesData));
        }

        List<Map<String, Object>> todasOcorrencias = fetchOcorrencias(baseUrl, riscoIdsDoProcesso, riscoNomePorId);
        enrichOcorrenciasWithCanonicalRisco(todasOcorrencias, riscoNomePorId);

        List<Map<String, Object>> ocorrenciasAlinhadas = riscoAlignmentService.alignByRiscoIds(todasOcorrencias, riscoIdsDoProcesso);
        riscoAlignmentService.applyCanonicalRiscoNome(ocorrenciasAlinhadas, riscoNomePorId);

        Map<String, Object> ocPayload = new LinkedHashMap<>();
        ocPayload.put("content", ocorrenciasAlinhadas);
        RefResolver.resolve(ocPayload);
        allSheets.putAll(OcorrenciaRiscoTransformer.transform(ocPayload));

        return allSheets;
    }

    private void addSheetIfAvailable(
            Map<String, List<Map<String, Object>>> allSheets,
            String baseUrl,
            String endpoint,
            Function<Map<String, Object>, Map<String, List<Map<String, Object>>>> transformer
    ) {
        Map<String, Object> data = fetchEndpointData(baseUrl, endpoint);
        if (data != null) {
            allSheets.putAll(transformer.apply(data));
        }
    }

    private Map<String, Object> fetchEndpointData(String baseUrl, String endpoint) {
        Map<String, Object> data = apiHttpClient.fetchAllPages(baseUrl, endpoint);
        if (data != null) {
            RefResolver.resolve(data);
        }
        return data;
    }

    private List<Map<String, Object>> fetchOcorrencias(
            String baseUrl,
            Set<Integer> riscoIdsDoProcesso,
            Map<Integer, String> riscoNomePorId
    ) {
        if (riscoIdsDoProcesso == null || riscoIdsDoProcesso.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Object> ocorrenciasData = fetchEndpointData(baseUrl, "/ocorrenciasRisco");
        if (ocorrenciasData == null) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> ocorrenciasContent = getList(ocorrenciasData);
        if (ocorrenciasContent == null || ocorrenciasContent.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> filtradas = new ArrayList<>();
        int withRiscoId = 0;
        for (Map<String, Object> ocorrencia : ocorrenciasContent) {
            Integer riscoId = riscoAlignmentService.extractRiscoId(ocorrencia);
            if (riscoId != null) {
                withRiscoId++;
            }
            if (riscoId != null && riscoIdsDoProcesso.contains(riscoId)) {
                filtradas.add(ocorrencia);
            }
        }

        // Fallback de compatibilidade: algumas APIs nao retornam risco.id no endpoint agregado.
        if (withRiscoId == 0) {
            return fetchOcorrenciasByRisco(baseUrl, riscoIdsDoProcesso, riscoNomePorId);
        }

        return filtradas;
    }

    private List<Map<String, Object>> fetchOcorrenciasByRisco(
            String baseUrl,
            Set<Integer> riscoIdsDoProcesso,
            Map<Integer, String> riscoNomePorId
    ) {
        List<Map<String, Object>> todasOcorrencias = new ArrayList<>();
        for (Integer riscoId : riscoIdsDoProcesso) {
            Map<String, Object> ocData = fetchEndpointData(baseUrl, "/ocorrenciasRisco/risco/" + riscoId);
            if (ocData == null) {
                continue;
            }

            List<Map<String, Object>> ocContent = getList(ocData);
            if (ocContent == null || ocContent.isEmpty()) {
                continue;
            }

            bindRiscoToOcorrencias(ocContent, riscoId, riscoNomePorId.getOrDefault(riscoId, ""));
            todasOcorrencias.addAll(ocContent);
        }
        return todasOcorrencias;
    }

    @SuppressWarnings("unchecked")
    private void bindRiscoToOcorrencias(List<Map<String, Object>> ocorrencias, Integer riscoId, String nomeRisco) {
        if (ocorrencias == null || ocorrencias.isEmpty() || riscoId == null) {
            return;
        }

        for (Map<String, Object> ocorrencia : ocorrencias) {
            if (ocorrencia == null) {
                continue;
            }

            Map<String, Object> risco = ocorrencia.get("risco") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : new LinkedHashMap<>();
            if (risco.get("id") == null) {
                risco.put("id", riscoId);
            }
            if (nomeRisco != null && !nomeRisco.isBlank()) {
                risco.put("nome", nomeRisco);
            }
            ocorrencia.put("risco", risco);
        }
    }

    @SuppressWarnings("unchecked")
    private void enrichOcorrenciasWithCanonicalRisco(
            List<Map<String, Object>> ocorrencias,
            Map<Integer, String> riscoNomePorId
    ) {
        if (ocorrencias == null || ocorrencias.isEmpty()) {
            return;
        }

        for (Map<String, Object> ocorrencia : ocorrencias) {
            if (ocorrencia == null) {
                continue;
            }

            Integer riscoId = riscoAlignmentService.extractRiscoId(ocorrencia);
            if (riscoId == null) {
                continue;
            }

            Map<String, Object> risco = ocorrencia.get("risco") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : new LinkedHashMap<>();

            if (risco.get("id") == null) {
                risco.put("id", riscoId);
            }

            String nomeRisco = riscoNomePorId.getOrDefault(riscoId, "");
            if (!nomeRisco.isBlank()) {
                risco.put("nome", nomeRisco);
            }

            ocorrencia.put("risco", risco);
        }
    }

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
    private boolean isRelatedToProcess(Map<String, Object> item, int pid, Map<String, Integer> refCache) {
        checkId(item, -1, refCache);

        Object procObj = item.get("processo");
        if (procObj instanceof Map) {
            Map<String, Object> proc = (Map<String, Object>) procObj;
            if (checkId(proc, pid, refCache)) {
                return true;
            }
        }

        Object riscoObj = item.get("risco");
        if (riscoObj instanceof Map) {
            Map<String, Object> risco = (Map<String, Object>) riscoObj;
            checkId(risco, -1, refCache);

            Object procViaRisco = risco.get("processo");
            if (procViaRisco instanceof Map) {
                Map<String, Object> proc = (Map<String, Object>) procViaRisco;
                if (checkId(proc, pid, refCache)) {
                    return true;
                }
            }
        }

        if (procObj == null && riscoObj == null) {
            return false;
        }
        return false;
    }

    private boolean checkId(Map<String, Object> obj, int pid, Map<String, Integer> refCache) {
        if (obj == null) {
            return false;
        }

        Object idVal = obj.get("id");
        Object atId = obj.get("@id");
        Object atRef = obj.get("@ref");

        Integer currentId = parseIntegerId(idVal);
        if (currentId != null) {
            if (atId != null) {
                refCache.put(atId.toString(), currentId);
            }
            return currentId == pid;
        }

        if (atRef != null && refCache.containsKey(atRef.toString())) {
            int cachedId = refCache.get(atRef.toString());
            return cachedId == pid;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> data) {
        if (data.containsKey("content")) {
            return (List<Map<String, Object>>) data.get("content");
        }
        return null;
    }

    private Integer parseIntegerId(Object rawId) {
        if (rawId == null) {
            return null;
        }
        if (rawId instanceof Number number) {
            return number.intValue();
        }

        String value = String.valueOf(rawId).trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
