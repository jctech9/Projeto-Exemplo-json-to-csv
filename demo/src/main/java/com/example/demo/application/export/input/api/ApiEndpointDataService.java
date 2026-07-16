package com.example.demo.application.export.input.api;

import com.example.demo.transformers.RefResolver;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
// Encapsula acesso HTTP e resolucao de referencias para payloads da API.
public class ApiEndpointDataService {

    private final ApiHttpClient apiHttpClient;

    public ApiEndpointDataService(ApiHttpClient apiHttpClient) {
        this.apiHttpClient = apiHttpClient;
    }

    public Map<String, Object> fetchEndpointData(String endpoint) {
        Map<String, Object> data = apiHttpClient.fetchAllPages(endpoint);
        if (data != null) {
            RefResolver.resolve(data);
        }
        return data;
    }
}
