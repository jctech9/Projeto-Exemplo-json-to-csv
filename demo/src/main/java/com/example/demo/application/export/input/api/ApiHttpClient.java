package com.example.demo.application.export.input.api;

import com.example.demo.config.ExportApiProperties;
import com.example.demo.config.ApiDestinationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApiHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ApiHttpClient.class);
    private static final int MAX_PAGES = 10_000;

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final ExportApiProperties properties;
    private final ApiDestinationValidator destinationValidator;

    public ApiHttpClient(
            RestTemplate exportApiRestTemplate,
            RetryTemplate exportApiRetryTemplate,
            ExportApiProperties properties,
            ApiDestinationValidator destinationValidator
    ) {
        this.restTemplate = exportApiRestTemplate;
        this.retryTemplate = exportApiRetryTemplate;
        this.properties = properties;
        this.destinationValidator = destinationValidator;
    }

    public Map<String, Object> fetchAllPages(String endpoint) {
        String normalizedEndpoint = normalizeEndpoint(endpoint);
        String normalizedBaseUrl = null;

        Map<String, Object> mergedResponse = null;
        List<Map<String, Object>> mergedContent = new ArrayList<>();

        int page = 0;
        while (true) {
            // Proteção contra laço infinito em APIs com metadados inconsistentes.
            if (page >= MAX_PAGES) {
                throw new ApiDataFetchException(
                        "Limite maximo de paginas excedido para endpoint " + normalizedEndpoint,
                        normalizedEndpoint
                );
            }

            // Revalida DNS imediatamente antes de cada chamada para reduzir a janela
            // de DNS rebinding entre paginas.
            normalizedBaseUrl = destinationValidator.validateConfiguredDestination().toString();
            Map<String, Object> response = fetchPageInternal(normalizedBaseUrl, normalizedEndpoint, page);
            if (mergedResponse == null) {
                // Preserva metadados da primeira página e substitui apenas o content no final.
                mergedResponse = new LinkedHashMap<>(response);
            }

            List<Map<String, Object>> pageContent = extractPageContent(response, normalizedEndpoint, page);
            mergedContent.addAll(pageContent);

            if (!hasNextPage(response, page, pageContent.size())) {
                break;
            }
            page++;
        }

        if (mergedResponse == null) {
            return null;
        }

        mergedResponse.put("content", mergedContent);
        return mergedResponse;
    }

    private Map<String, Object> fetchPageInternal(String normalizedBaseUrl, String normalizedEndpoint, int page) {
        URI uri = buildUri(normalizedBaseUrl, normalizedEndpoint, page);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = retryTemplate.execute(context -> {
                int attempt = context.getRetryCount() + 1;
                log.debug(
                        "event=api_request endpoint={} page={} attempt={}",
                        normalizedEndpoint,
                        page,
                        attempt
                );

                try {
                    return restTemplate.getForObject(uri, Map.class);
                } catch (RuntimeException ex) {
                    log.warn(
                            "event=api_request_attempt_failed endpoint={} page={} attempt={} exception={}",
                            normalizedEndpoint,
                            page,
                            attempt,
                            ex.getClass().getSimpleName()
                    );
                    throw ex;
                }
            });

            if (response == null) {
                log.error("event=api_request_empty_response endpoint={} page={}", normalizedEndpoint, page);
                throw new ApiDataFetchException(
                        "Resposta vazia ao consultar endpoint " + normalizedEndpoint + " (page=" + page + ")",
                        normalizedEndpoint
                );
            }

            return response;
        } catch (ApiDataFetchException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            log.error(
                    "event=api_request_http_error endpoint={} page={} status={}",
                    normalizedEndpoint,
                    page,
                    statusCode
            );
            throw new ApiDataFetchException(
                    "Falha HTTP ao consultar endpoint " + normalizedEndpoint
                            + " (page=" + page + ", status=" + statusCode + ")",
                    normalizedEndpoint,
                    ex
            );
        } catch (RuntimeException ex) {
            log.error(
                    "event=api_request_failed endpoint={} page={} exception={}",
                    normalizedEndpoint,
                    page,
                    ex.getClass().getSimpleName()
            );
            throw new ApiDataFetchException(
                    "Falha ao consultar endpoint " + normalizedEndpoint + " (page=" + page + ") apos tentativas de retry",
                    normalizedEndpoint,
                    ex
            );
        }
    }

    private URI buildUri(String normalizedBaseUrl, String endpoint, int page) {
        return UriComponentsBuilder.fromUriString(normalizedBaseUrl)
                .path(endpoint)
                .queryParam("page", page)
                .queryParam("size", properties.getPageSize())
                .build(true)
                .toUri();
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "/";
        }
        return endpoint.startsWith("/") ? endpoint : "/" + endpoint;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractPageContent(
            Map<String, Object> response,
            String endpoint,
            int page
    ) {
        Object contentObject = response.get("content");
        if (contentObject == null) {
            return List.of();
        }

        if (!(contentObject instanceof List<?> list)) {
            throw new ApiDataFetchException(
                    "Resposta invalida ao consultar endpoint " + endpoint + ": campo 'content' nao e lista",
                    endpoint
            );
        }

        List<Map<String, Object>> pageContent = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Map<?, ?> itemMap)) {
                throw new ApiDataFetchException(
                        "Resposta invalida ao consultar endpoint " + endpoint
                                + ": item de 'content' na posicao " + (i + 1) + " nao e objeto",
                        endpoint
                );
            }
            pageContent.add((Map<String, Object>) itemMap);
        }

        return pageContent;
    }

    private boolean hasNextPage(Map<String, Object> response, int currentPage, int pageContentSize) {
        // Ordem de decisão: totalPages -> flag last -> heurística por tamanho da página.
        Integer totalPages = parseIntOrNull(response.get("totalPages"));
        if (totalPages == null) {
            totalPages = parseIntOrNull(getFromPageMetadata(response, "totalPages"));
        }
        if (totalPages != null) {
            return currentPage + 1 < totalPages;
        }

        Boolean last = parseBooleanOrNull(response.get("last"));
        if (last == null) {
            last = parseBooleanOrNull(getFromPageMetadata(response, "last"));
        }
        if (last != null) {
            return !last;
        }

        // Fallback para APIs sem metadados de paginacao.
        return pageContentSize >= properties.getPageSize();
    }

    private Integer parseIntOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean parseBooleanOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return null;
        }
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object getFromPageMetadata(Map<String, Object> response, String key) {
        Object pageObject = response.get("page");
        if (!(pageObject instanceof Map<?, ?> pageMap)) {
            return null;
        }
        return ((Map<String, Object>) pageMap).get(key);
    }
}
