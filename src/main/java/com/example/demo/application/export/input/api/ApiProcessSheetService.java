package com.example.demo.application.export.input.api;

import com.example.demo.transformers.DadosProcessoTransformer;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
// Responsavel por montar os dados da aba de processo principal.
public class ApiProcessSheetService {

    private final ApiEndpointDataService endpointDataService;
    private final ApiPayloadFilterService payloadFilterService;

    public ApiProcessSheetService(
            ApiEndpointDataService endpointDataService,
            ApiPayloadFilterService payloadFilterService
    ) {
        this.endpointDataService = endpointDataService;
        this.payloadFilterService = payloadFilterService;
    }

    public Map<String, List<Map<String, Object>>> buildProcessSheets(int processId) {
        Map<String, Object> processosData = endpointDataService.fetchEndpointData("/processos");
        if (processosData == null) {
            return new LinkedHashMap<>();
        }

        List<Map<String, Object>> filteredInfo = payloadFilterService.filterByExactId(processosData, processId);
        if (filteredInfo.isEmpty()) {
            throw new IllegalArgumentException("Processo com id " + processId + " nao foi encontrado na API origem.");
        }

        processosData.put("content", filteredInfo);
        return DadosProcessoTransformer.transform(processosData);
    }
}
