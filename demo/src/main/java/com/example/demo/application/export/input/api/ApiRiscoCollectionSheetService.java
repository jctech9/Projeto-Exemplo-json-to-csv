package com.example.demo.application.export.input.api;

import com.example.demo.domain.risco.RiscoAlignmentService;
import com.example.demo.domain.risco.RiscoValidationService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
// Monta colecoes dependentes de risco com validacao estrita e alinhamento canonico.
public class ApiRiscoCollectionSheetService {

    private final ApiEndpointDataService endpointDataService;
    private final ApiPayloadFilterService payloadFilterService;
    private final RiscoAlignmentService riscoAlignmentService;
    private final RiscoValidationService riscoValidationService;

    public ApiRiscoCollectionSheetService(
            ApiEndpointDataService endpointDataService,
            ApiPayloadFilterService payloadFilterService,
            RiscoAlignmentService riscoAlignmentService,
            RiscoValidationService riscoValidationService
    ) {
        this.endpointDataService = endpointDataService;
        this.payloadFilterService = payloadFilterService;
        this.riscoAlignmentService = riscoAlignmentService;
        this.riscoValidationService = riscoValidationService;
    }

    public Map<String, List<Map<String, Object>>> buildAlignedSheet(
            String baseUrl,
            String endpoint,
            int processId,
            Set<Integer> canonicalRiscoIds,
            String collectionName,
            Function<Map<String, Object>, Map<String, List<Map<String, Object>>>> transformer
    ) {
        Map<String, Object> data = endpointDataService.fetchEndpointData(baseUrl, endpoint);
        if (data == null) {
            return new LinkedHashMap<>();
        }

        List<Map<String, Object>> content = payloadFilterService.filterByProcess(data, processId);
        riscoValidationService.validateStrictRiscoCollection(collectionName, content, canonicalRiscoIds);
        data.put("content", riscoAlignmentService.alignByRiscoIds(content, canonicalRiscoIds));

        return transformer.apply(data);
    }
}
