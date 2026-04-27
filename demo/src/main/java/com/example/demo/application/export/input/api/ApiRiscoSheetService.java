package com.example.demo.application.export.input.api;

import com.example.demo.transformers.IdentificacaoEventosTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
// Monta contexto canonico de riscos e metadados dinamicos da etapa 2.
public class ApiRiscoSheetService {

    private static final Logger log = LoggerFactory.getLogger(ApiRiscoSheetService.class);
    private static final String META_ROW_TYPE_KEY = "__meta_row_type";
    private static final String META_ROW_TYPE_OPTIONS = "etapa2_options";
    private static final String META_CATEGORIA_OPTIONS_KEY = "__meta_categoria_options";
    private static final String META_TIPO_RISCO_INTEGRIDADE_OPTIONS_KEY = "__meta_tipo_risco_integridade_options";

    private final ApiEndpointDataService endpointDataService;
    private final ApiPayloadFilterService payloadFilterService;

    public ApiRiscoSheetService(
            ApiEndpointDataService endpointDataService,
            ApiPayloadFilterService payloadFilterService
    ) {
        this.endpointDataService = endpointDataService;
        this.payloadFilterService = payloadFilterService;
    }

    public ApiRiscoContext buildRiscoContext(String baseUrl, int processId) {
        Set<Integer> riscoIdsDoProcesso = new LinkedHashSet<>();
        Map<Integer, String> riscoNomePorId = new LinkedHashMap<>();

        Map<String, Object> riscosData = endpointDataService.fetchEndpointData(baseUrl, "/riscos");
        if (riscosData == null) {
            return new ApiRiscoContext(new LinkedHashMap<>(), riscoIdsDoProcesso, riscoNomePorId);
        }

        List<String> categoriaOptions = fetchCategoriaOptions(baseUrl);
        List<String> tipoRiscoIntegridadeOptions = fetchTipoRiscoIntegridadeOptions(baseUrl);
        List<Map<String, Object>> riscosFiltrados = payloadFilterService.filterByProcess(riscosData, processId);
        riscosData.put("content", riscosFiltrados);

        for (Map<String, Object> risco : riscosFiltrados) {
            Integer id = payloadFilterService.parseIntegerId(risco.get("id"));
            if (id != null) {
                riscoIdsDoProcesso.add(id);
                riscoNomePorId.put(id, String.valueOf(risco.getOrDefault("nome", "")));
            }
        }

        Map<String, List<Map<String, Object>>> etapa2Sheets = IdentificacaoEventosTransformer.transform(riscosData);
        attachEtapa2OptionsMetadata(etapa2Sheets, categoriaOptions, tipoRiscoIntegridadeOptions);

        return new ApiRiscoContext(etapa2Sheets, riscoIdsDoProcesso, riscoNomePorId);
    }

    private List<String> fetchCategoriaOptions(String baseUrl) {
        return fetchNomeOptions(baseUrl, "/categoriasRisco");
    }

    private List<String> fetchTipoRiscoIntegridadeOptions(String baseUrl) {
        return fetchNomeOptions(baseUrl, "/tiposRiscoIntegridade");
    }

    private List<String> fetchNomeOptions(String baseUrl, String endpoint) {
        try {
            Map<String, Object> optionsData = endpointDataService.fetchEndpointData(baseUrl, endpoint);
            List<Map<String, Object>> content = payloadFilterService.getContentList(optionsData);
            if (content == null || content.isEmpty()) {
                return new ArrayList<>();
            }

            LinkedHashSet<String> names = new LinkedHashSet<>();
            for (Map<String, Object> item : content) {
                if (item == null) {
                    continue;
                }

                Object nome = item.get("nome");
                if (nome == null) {
                    continue;
                }

                String normalized = String.valueOf(nome).trim();
                if (!normalized.isBlank()) {
                    names.add(normalized);
                }
            }

            return new ArrayList<>(names);
        } catch (ApiDataFetchException ex) {
            log.warn(
                    "event=api_export_optional_endpoint_failed endpoint={} url={} message={}",
                    ex.getEndpoint(),
                    ex.getUrl(),
                    ex.getMessage()
            );
            return new ArrayList<>();
        }
    }

    private void attachEtapa2OptionsMetadata(
            Map<String, List<Map<String, Object>>> etapa2Sheets,
            List<String> categoriaOptions,
            List<String> tipoRiscoIntegridadeOptions
    ) {
        if (etapa2Sheets == null || etapa2Sheets.isEmpty()) {
            return;
        }

        List<String> normalizedCategoriaOptions = normalizeOptions(categoriaOptions);
        List<String> normalizedTipoRiscoIntegridadeOptions = normalizeOptions(tipoRiscoIntegridadeOptions);

        if (normalizedCategoriaOptions.isEmpty() && normalizedTipoRiscoIntegridadeOptions.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : etapa2Sheets.entrySet()) {
            List<Map<String, Object>> rows = entry.getValue();
            if (rows == null) {
                rows = new ArrayList<>();
                entry.setValue(rows);
            }

            Map<String, Object> metadataRow = new LinkedHashMap<>();
            metadataRow.put(META_ROW_TYPE_KEY, META_ROW_TYPE_OPTIONS);
            if (!normalizedCategoriaOptions.isEmpty()) {
                metadataRow.put(META_CATEGORIA_OPTIONS_KEY, normalizedCategoriaOptions);
            }
            if (!normalizedTipoRiscoIntegridadeOptions.isEmpty()) {
                metadataRow.put(META_TIPO_RISCO_INTEGRIDADE_OPTIONS_KEY, normalizedTipoRiscoIntegridadeOptions);
            }
            rows.add(0, metadataRow);
        }
    }

    private List<String> normalizeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> normalizedOptions = new LinkedHashSet<>();
        for (String option : options) {
            if (option == null) {
                continue;
            }

            String trimmed = option.trim();
            if (!trimmed.isBlank()) {
                normalizedOptions.add(trimmed);
            }
        }

        return new ArrayList<>(normalizedOptions);
    }
}
