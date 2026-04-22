package com.example.demo.application.export.input.api;

import com.example.demo.domain.risco.RiscoAlignmentService;
import com.example.demo.transformers.OcorrenciaRiscoTransformer;
import com.example.demo.transformers.RefResolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
// Monta ocorrencias por risco com fallback para endpoint por risco quando necessario.
public class ApiOcorrenciaSheetService {

    private final ApiEndpointDataService endpointDataService;
    private final ApiPayloadFilterService payloadFilterService;
    private final RiscoAlignmentService riscoAlignmentService;

    public ApiOcorrenciaSheetService(
            ApiEndpointDataService endpointDataService,
            ApiPayloadFilterService payloadFilterService,
            RiscoAlignmentService riscoAlignmentService
    ) {
        this.endpointDataService = endpointDataService;
        this.payloadFilterService = payloadFilterService;
        this.riscoAlignmentService = riscoAlignmentService;
    }

    public Map<String, List<Map<String, Object>>> buildOcorrenciaSheets(
            String baseUrl,
            Set<Integer> riscoIdsDoProcesso,
            Map<Integer, String> riscoNomePorId
    ) {
        List<Map<String, Object>> todasOcorrencias = fetchOcorrencias(baseUrl, riscoIdsDoProcesso, riscoNomePorId);
        enrichOcorrenciasWithCanonicalRisco(todasOcorrencias, riscoNomePorId);

        List<Map<String, Object>> ocorrenciasAlinhadas =
                riscoAlignmentService.alignByRiscoIds(todasOcorrencias, riscoIdsDoProcesso);
        riscoAlignmentService.applyCanonicalRiscoNome(ocorrenciasAlinhadas, riscoNomePorId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", ocorrenciasAlinhadas);
        RefResolver.resolve(payload);
        return OcorrenciaRiscoTransformer.transform(payload);
    }

    private List<Map<String, Object>> fetchOcorrencias(
            String baseUrl,
            Set<Integer> riscoIdsDoProcesso,
            Map<Integer, String> riscoNomePorId
    ) {
        if (riscoIdsDoProcesso == null || riscoIdsDoProcesso.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Object> ocorrenciasData = endpointDataService.fetchEndpointData(baseUrl, "/ocorrenciasRisco");
        if (ocorrenciasData == null) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> ocorrenciasContent = payloadFilterService.getContentList(ocorrenciasData);
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
            Map<String, Object> ocData =
                    endpointDataService.fetchEndpointData(baseUrl, "/ocorrenciasRisco/risco/" + riscoId);
            if (ocData == null) {
                continue;
            }

            List<Map<String, Object>> ocContent = payloadFilterService.getContentList(ocData);
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

            Map<String, Object> risco = ocorrencia.get("risco") instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
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

            Map<String, Object> risco = ocorrencia.get("risco") instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
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
}
